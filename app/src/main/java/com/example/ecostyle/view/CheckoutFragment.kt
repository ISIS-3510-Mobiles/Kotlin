package com.example.ecostyle.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.Adapter.CartAdapter
import com.example.ecostyle.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

class CheckoutFragment : Fragment() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkoutButton: Button
    private var fcmToken: String? = null // Mantener el token en memoria durante la sesión

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_checkout, container, false)

        val paymentMethodsSpinner: Spinner = view.findViewById(R.id.payment_methods_spinner)
        val paymentMethods = arrayOf("Nequi", "Tarjeta de Crédito", "Tarjeta de Debito", "PSE", "Efectivo")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter

        recyclerView = view.findViewById(R.id.recycler_view_cart_items)
        recyclerView.layoutManager = LinearLayoutManager(context)

        cartAdapter = CartAdapter(emptyList())
        recyclerView.adapter = cartAdapter

        checkoutButton = view.findViewById(R.id.checkout_button)
        checkoutButton.isEnabled = false // Deshabilitar el botón por defecto

        // Obtener el token FCM cuando el fragmento se crea (solo una vez durante la sesión)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result // Guardar el token en memoria
                Log.d("FCM", "Token obtenido: $fcmToken")
            } else {
                Log.w("FCM", "Error al obtener el token FCM", task.exception)
            }
        }

        loadCartItems()

        // Implementación de la lógica para realizar la compra y vaciar el carrito
        checkoutButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("carts").document(userId).collection("items")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (document in snapshot.documents) {
                            document.reference.delete() // Borrar cada producto en el carrito
                        }
                        Toast.makeText(context, "Compra realizada", Toast.LENGTH_SHORT).show()
                        showPurchaseConfirmation() // Redirigir a la pantalla de confirmación
                    }
            }
        }

        return view
    }

    override fun onPause() {
        super.onPause()

        // Verifica si hay productos en el carrito antes de que la app se minimice
        if (cartAdapter.itemCount > 0 && fcmToken != null) {
            // Envía la notificación si hay productos en el carrito y el token FCM existe
            sendAbandonedCartNotification(fcmToken!!)
        }
    }

    private fun loadCartItems() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("carts").document(userId).collection("items")
                .get()
                .addOnSuccessListener { documents ->
                    val cartItems = mutableListOf<CartItem>()
                    for (document in documents) {
                        val cartItem = document.toObject(CartItem::class.java)
                        cartItems.add(cartItem)
                    }

                    cartAdapter.setCartItems(cartItems)

                    // Si hay productos en el carrito, habilitar el botón de Checkout
                    if (cartItems.isNotEmpty()) {
                        checkoutButton.isEnabled = true
                    } else {
                        Toast.makeText(context, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
                        checkoutButton.isEnabled = false // Asegurarse de que el botón esté deshabilitado si no hay productos
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al cargar el carrito", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showPurchaseConfirmation() {
        // Lógica para redirigir a la pantalla de confirmación de compra
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PurchaseConfirmationFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun sendAbandonedCartNotification(token: String) {
        // Crea la notificación con el título y mensaje que desees
        val notification = RemoteMessage.Builder(token)
            .setMessageId("abandoned_cart_${System.currentTimeMillis()}")
            .addData("title", "Carrito Abandonado")
            .addData("body", "No olvides completar tu compra.")
            .build()

        // Envía la notificación utilizando Firebase Messaging
        FirebaseMessaging.getInstance().send(notification)
    }
}

