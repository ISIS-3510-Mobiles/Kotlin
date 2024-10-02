package com.example.ecostyle.view

import android.content.Context
import android.os.Bundle
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

class CheckoutFragment : Fragment() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkoutButton: Button

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
                        updateCartStatus(false) // Vaciar el carrito
                        showPurchaseConfirmation() // Redirigir a la pantalla de confirmación
                    }
            }
        }

        return view
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
                        updateCartStatus(true) // Marcar que el carrito tiene productos
                    } else {
                        Toast.makeText(context, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
                        checkoutButton.isEnabled = false
                        updateCartStatus(false) // Marcar que el carrito está vacío
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al cargar el carrito", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateCartStatus(hasItems: Boolean) {
        if (isAdded) {
            val sharedPreferences = requireContext().getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("hasItemsInCart", hasItems)
            editor.apply()
        }
    }

    private fun showPurchaseConfirmation() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PurchaseConfirmationFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }
}

