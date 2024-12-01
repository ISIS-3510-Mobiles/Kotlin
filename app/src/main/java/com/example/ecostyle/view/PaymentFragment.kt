package com.example.ecostyle.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ecostyle.model.CartItem
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class PaymentFragment : Fragment() {

    private lateinit var cancelPurchaseButton: Button
    private lateinit var billingAddress: EditText
    private lateinit var billingCity: EditText
    private lateinit var billingZipcode: EditText
    private lateinit var proceedButton: Button
    private lateinit var paymentMethodsSpinner: Spinner

    private lateinit var billingAddressError: TextView
    private lateinit var billingCityError: TextView
    private lateinit var billingZipcodeError: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_payment, container, false)

        // Inicialización de vistas
        paymentMethodsSpinner = view.findViewById(R.id.payment_methods_spinner)
        billingAddress = view.findViewById(R.id.billing_address)
        billingCity = view.findViewById(R.id.billing_city)
        billingZipcode = view.findViewById(R.id.billing_zipcode)
        proceedButton = view.findViewById(R.id.proceed_to_confirmation_button)
        cancelPurchaseButton = view.findViewById(R.id.cancel_purchase_button)

        billingAddressError = view.findViewById(R.id.billing_address_error)
        billingCityError = view.findViewById(R.id.billing_city_error)
        billingZipcodeError = view.findViewById(R.id.billing_zipcode_error)

        // Filtros para limitar a 6 dígitos y solo números en billingZipcode
        billingZipcode.filters = arrayOf(InputFilter.LengthFilter(6), NumericInputFilter())

        val paymentMethods = arrayOf("Nequi", "Daviplata", "Credit card", "Debit card", "PSE", "Efectivo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter

        val cartItems = arguments?.getParcelableArrayList<CartItem>("cartItems")

        proceedButton.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                Toast.makeText(context, "Offline. Cannot proceed. Please try again later.", Toast.LENGTH_LONG).show()
            } else if (isValidForm()) {
                GlobalScope.launch(Dispatchers.IO) {
                    updateStockAndConfirmPurchase(cartItems)
                }
            } else {
                Toast.makeText(context, "Please complete all fields correctly.", Toast.LENGTH_SHORT).show()
            }
        }

        cancelPurchaseButton.setOnClickListener {
            cancelPurchase()
        }

        setupFieldValidation()

        return view
    }

    private suspend fun updateStockAndConfirmPurchase(cartItems: List<CartItem>?) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null && !cartItems.isNullOrEmpty()) { // Validar que cartItems no sea nulo ni vacío
            var allAvailable = true

            cartItems.forEach { cartItem ->
                try {
                    val productRef = db.collection("Products").document(cartItem.firebaseId)
                    val productDoc = productRef.get().await()
                    val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0

                    if (cartItem.quantity <= availableQuantity) {
                        productRef.update("quantity", availableQuantity - cartItem.quantity).await()
                    } else {
                        allAvailable = false
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Insufficient stock for ${cartItem.productName}. Available: $availableQuantity.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Error updating stock for ${cartItem.productName}: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            if (allAvailable) {
                savePurchaseHistory(cartItems, userId)

                val cartRef = db.collection("carts").document(userId).collection("items")
                cartRef.get().await().forEach { document ->
                    cartRef.document(document.id).delete().await()
                }

                withContext(Dispatchers.Main) {
                    showPurchaseConfirmation()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Some items in your cart do not have enough stock. Please review your cart.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Cart is empty or user not logged in. Cannot proceed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun savePurchaseHistory(cartItems: List<CartItem>, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val historyRef = db.collection("historial").document(userId)

        try {
            val documentSnapshot = historyRef.get().await()

            if (documentSnapshot.exists()) {
                val currentPurchases = documentSnapshot.get("compras") as? List<Map<String, Any>> ?: emptyList()
                val updatedPurchases = currentPurchases.toMutableList()

                cartItems.forEach { cartItem ->
                    val existingProduct = updatedPurchases.find { it["productId"] == cartItem.firebaseId }
                    if (existingProduct != null) {
                        val currentQuantity = (existingProduct["quantity"] as? Number)?.toInt() ?: 0
                        val newQuantity = currentQuantity + cartItem.quantity

                        val updatedProduct = existingProduct.toMutableMap()
                        updatedProduct["quantity"] = newQuantity

                        updatedPurchases[updatedPurchases.indexOf(existingProduct)] = updatedProduct
                    } else {
                        updatedPurchases.add(
                            hashMapOf(
                                "productId" to cartItem.firebaseId,
                                "name" to cartItem.productName,
                                "price" to cartItem.productPrice,
                                "quantity" to cartItem.quantity,
                                "imageResource" to cartItem.productImage
                            )
                        )
                    }
                }

                historyRef.update("compras", updatedPurchases).await()
            } else {
                val initialData = hashMapOf(
                    "ventas" to emptyList<Map<String, Any>>(),
                    "compras" to cartItems.map { cartItem ->
                        hashMapOf(
                            "productId" to cartItem.firebaseId,
                            "name" to cartItem.productName,
                            "price" to cartItem.productPrice,
                            "quantity" to cartItem.quantity,
                            "imageResource" to cartItem.productImage
                        )
                    }
                )
                historyRef.set(initialData).await()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Failed to update purchase history: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isValidForm(): Boolean {
        return validateBillingAddress() && validateBillingCity() && validateBillingZipcode()
    }

    private fun validateBillingAddress(): Boolean {
        val addressText = billingAddress.text.toString()

        return if (addressText.length > 6 && addressText.isNotBlank() && addressText.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d).+$"))) {
            billingAddress.setTextColor(Color.BLACK)
            billingAddressError.isVisible = false
            true
        } else {
            billingAddress.setTextColor(Color.RED)
            billingAddressError.isVisible = true
            billingAddressError.text = "Address must be more than 6 characters and contain both letters and numbers"
            false
        }
    }

    private fun validateBillingCity(): Boolean {
        val city = billingCity.text.toString().trim()

        return if (city.length > 4 && city.matches(Regex("^[a-zA-Z]+(\\s[a-zA-Z]+)*\$"))) {
            billingCity.setTextColor(Color.BLACK)
            billingCityError.isVisible = false
            true
        } else {
            billingCity.setTextColor(Color.RED)
            billingCityError.isVisible = true
            billingCityError.text = "City must be more than 4 characters and can only contain letters and spaces"
            false
        }
    }

    private fun validateBillingZipcode(): Boolean {
        return if (billingZipcode.text.length == 6 && billingZipcode.text.matches(Regex("^[0-9]+\$"))) {
            billingZipcode.setTextColor(Color.BLACK)
            billingZipcodeError.isVisible = false
            true
        } else {
            billingZipcode.setTextColor(Color.RED)
            billingZipcodeError.isVisible = true
            billingZipcodeError.text = "Zip code must be exactly 6 digits"
            false
        }
    }

    private fun cancelPurchase() {
        Toast.makeText(context, "Purchase canceled.", Toast.LENGTH_SHORT).show()
        activity?.supportFragmentManager?.popBackStack()
    }

    private fun showPurchaseConfirmation() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PurchaseConfirmationFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun setupFieldValidation() {
        billingAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateBillingAddress()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        billingCity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateBillingCity()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        billingZipcode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateBillingZipcode()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private class NumericInputFilter : InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            for (i in start until end) {
                if (!Character.isDigit(source[i])) {
                    return ""
                }
            }
            return null
        }
    }
}


