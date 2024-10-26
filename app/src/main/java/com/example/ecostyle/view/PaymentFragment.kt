package com.example.ecostyle.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ecostyle.model.CartItem

class PaymentFragment : Fragment() {

    private lateinit var cancelPurchaseButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_payment, container, false)

        // Configurar el Spinner de métodos de pago
        val paymentMethodsSpinner: Spinner = view.findViewById(R.id.payment_methods_spinner)
        val paymentMethods = arrayOf("Nequi", "Tarjeta de Crédito", "Tarjeta de Débito", "PSE", "Efectivo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter

        // Obtener los campos de facturación
        val billingAddress: EditText = view.findViewById(R.id.billing_address)
        val billingCity: EditText = view.findViewById(R.id.billing_city)
        val billingZipcode: EditText = view.findViewById(R.id.billing_zipcode)

        // Configurar el botón para proceder a la confirmación
        val proceedButton: Button = view.findViewById(R.id.proceed_to_confirmation_button)
        proceedButton.setOnClickListener {
            // Validar la información
            val selectedPaymentMethod = paymentMethodsSpinner.selectedItem.toString()
            val address = billingAddress.text.toString()
            val city = billingCity.text.toString()
            val zipcode = billingZipcode.text.toString()

            if (address.isNotEmpty() && city.isNotEmpty() && zipcode.isNotEmpty()) {
                showPurchaseConfirmation()
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el botón para cancelar la compra
        cancelPurchaseButton = view.findViewById(R.id.cancel_purchase_button)
        cancelPurchaseButton.setOnClickListener {
            cancelPurchase()
        }

        return view
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
                        val productRef = db.collection("Products").document(cartItem.id)

                        productRef.get().addOnSuccessListener { productDoc ->
                            val currentStock = productDoc.getLong("quantity")?.toInt() ?: 0
                            productRef.update("quantity", currentStock + cartItem.quantity)
                        }

                        // Eliminar productos del carrito
                        cartRef.document(document.id).delete()
                    }
                }
                Toast.makeText(context, "Purchase cancelled, cart emptied.", Toast.LENGTH_SHORT).show()
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
}
