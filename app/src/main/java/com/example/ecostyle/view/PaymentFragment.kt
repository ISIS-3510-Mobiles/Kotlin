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

class PaymentFragment : Fragment() {

    private lateinit var cancelPurchaseButton: Button
    private lateinit var billingAddress: EditText
    private lateinit var billingCity: EditText
    private lateinit var billingZipcode: EditText
    private lateinit var proceedButton: Button
    private lateinit var paymentMethodsSpinner: Spinner

    // Mensajes de error
    private lateinit var billingAddressError: TextView
    private lateinit var billingCityError: TextView
    private lateinit var billingZipcodeError: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_payment, container, false)

        // Inicializar las vistas y los botones
        paymentMethodsSpinner = view.findViewById(R.id.payment_methods_spinner)
        billingAddress = view.findViewById(R.id.billing_address)
        billingCity = view.findViewById(R.id.billing_city)
        billingZipcode = view.findViewById(R.id.billing_zipcode)
        proceedButton = view.findViewById(R.id.proceed_to_confirmation_button)
        cancelPurchaseButton = view.findViewById(R.id.cancel_purchase_button)

        // Inicializar los mensajes de error
        billingAddressError = view.findViewById(R.id.billing_address_error)
        billingCityError = view.findViewById(R.id.billing_city_error)
        billingZipcodeError = view.findViewById(R.id.billing_zipcode_error)

        // Configurar el Spinner de métodos de pago
        val paymentMethods = arrayOf("Nequi", "Daviplata", "Credit card", "Debit card", "PSE", "Efectivo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter

        // Configurar el botón para proceder a la confirmación
        proceedButton.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                Toast.makeText(context, "\n" +
                        "Offline. Cannot proceed. Please try again later.", Toast.LENGTH_LONG).show()
            } else if (isValidForm()) {
                showPurchaseConfirmation()
            } else {
                Toast.makeText(context, "\n" +
                        "Please complete all fields correctly.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el botón para cancelar la compra
        cancelPurchaseButton.setOnClickListener {
            cancelPurchase()
        }

        // Validación en tiempo real
        setupFieldValidation()

        return view
    }

    // Validar los campos de entrada
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
            billingCityError.text = "\n" +
                    "The city must be more than 4 characters and cannot contain numbers"
            false
        }
    }

    private fun validateBillingZipcode(): Boolean {
        return if (billingZipcode.text.length >= 6 && billingZipcode.text.matches(Regex("^[0-9]+\$"))) {
            billingZipcode.setTextColor(Color.BLACK)
            billingZipcodeError.isVisible = false
            true
        } else {
            billingZipcode.setTextColor(Color.RED)
            billingZipcodeError.isVisible = true
            billingZipcodeError.text = "\n" +
                    "Zip code must be at least 6 digits"
            false
        }
    }

    private fun isValidForm(): Boolean {
        return validateBillingAddress() && validateBillingCity() && validateBillingZipcode()
    }

    // Cancelar la compra y restaurar el inventario en Firebase
    private fun cancelPurchase() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            cartRef.get().addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    val cartItem = document.toObject(CartItem::class.java)

                    cartItem?.let {
                        val productRef = db.collection("Products").document(cartItem.firebaseId)

                        productRef.get().addOnSuccessListener { productDoc ->
                            val currentStock = productDoc.getLong("quantity")?.toInt() ?: 0
                            productRef.update("quantity", currentStock + cartItem.quantity)
                        }

                        cartRef.document(document.id).delete()
                    }
                }
                Toast.makeText(context, "Purchase canceled, cart empty.", Toast.LENGTH_SHORT).show()
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    private fun showPurchaseConfirmation() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PurchaseConfirmationFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    // Función para verificar si hay conexión a Internet
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}

