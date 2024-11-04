package com.example.ecostyle.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
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

        paymentMethodsSpinner = view.findViewById(R.id.payment_methods_spinner)
        billingAddress = view.findViewById(R.id.billing_address)
        billingCity = view.findViewById(R.id.billing_city)
        billingZipcode = view.findViewById(R.id.billing_zipcode)
        proceedButton = view.findViewById(R.id.proceed_to_confirmation_button)
        cancelPurchaseButton = view.findViewById(R.id.cancel_purchase_button)

        billingAddressError = view.findViewById(R.id.billing_address_error)
        billingCityError = view.findViewById(R.id.billing_city_error)
        billingZipcodeError = view.findViewById(R.id.billing_zipcode_error)

        val paymentMethods = arrayOf("Nequi", "Daviplata", "Credit card", "Debit card", "PSE", "Efectivo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter

        val cartItems = arguments?.getParcelableArrayList<CartItem>("cartItems")

        proceedButton.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                Toast.makeText(context, "Offline. Cannot proceed. Please try again later.", Toast.LENGTH_LONG).show()
            } else if (isValidForm()) {
                updateStockAndConfirmPurchase(cartItems)
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

    private fun updateStockAndConfirmPurchase(cartItems: List<CartItem>?) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            GlobalScope.launch(Dispatchers.IO) {
                var allAvailable = true

                cartItems?.forEach { cartItem ->
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
                }

                if (allAvailable) {
                    // Borrar el carrito del usuario al confirmar la compra
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
            }
        } else {
            Toast.makeText(context, "User not logged in. Please log in to proceed.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun isValidForm(): Boolean {
        return validateBillingAddress() && validateBillingCity() && validateBillingZipcode()
    }

    private fun validateBillingAddress(): Boolean {
        return if (billingAddress.text.length > 6) {
            billingAddress.setTextColor(Color.BLACK)
            billingAddressError.isVisible = false
            true
        } else {
            billingAddress.setTextColor(Color.RED)
            billingAddressError.isVisible = true
            billingAddressError.text = "Address must be more than 6 characters"
            false
        }
    }

    private fun validateBillingCity(): Boolean {
        val city = billingCity.text.toString()
        return if (city.length > 4 && city.matches(Regex("^[a-zA-Z]+\$"))) {
            billingCity.setTextColor(Color.BLACK)
            billingCityError.isVisible = false
            true
        } else {
            billingCity.setTextColor(Color.RED)
            billingCityError.isVisible = true
            billingCityError.text = "The city must be more than 4 characters and cannot contain numbers"
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
}
