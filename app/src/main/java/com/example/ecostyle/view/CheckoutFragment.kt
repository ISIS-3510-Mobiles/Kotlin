package com.example.ecostyle.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.CartAdapter
import com.example.ecostyle.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CheckoutFragment : Fragment() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkoutButton: Button
    private lateinit var totalPriceTextView: TextView
    private var totalPrice: Double = 0.0 // Para el total de la compra

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_checkout, container, false)

        totalPriceTextView = view.findViewById(R.id.total_price_text_view)

        recyclerView = view.findViewById(R.id.recycler_view_cart_items)
        recyclerView.layoutManager = LinearLayoutManager(context)

        cartAdapter = CartAdapter(emptyList()) { cartItem ->
            // Lógica para eliminar el producto
            removeCartItem(cartItem)
        }
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
                        Toast.makeText(context, "Redirigiendo a la pantalla de pago", Toast.LENGTH_SHORT).show()
                        updateCartStatus(false) // Vaciar el carrito
                        totalPrice = 0.0 // Reiniciar el total
                        updateTotalPrice()
                        showPaymentMethodScreen() // Redirigir a la pantalla de método de pago
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
                    totalPrice = 0.0
                    var itemCount = 0

                    for (document in documents) {
                        val cartItem = document.toObject(CartItem::class.java)
                        cartItem.id = document.id // Asignar el ID del documento al cartItem
                        cartItems.add(cartItem)

                        // Eliminar el símbolo de dólar antes de convertir a Double
                        val cleanPrice = cartItem.productPrice.toString().replace("$", "").toDoubleOrNull() ?: 0.0
                        totalPrice += cleanPrice // Sumar el precio de cada producto
                        itemCount++
                    }

                    cartAdapter.setCartItems(cartItems)
                    updateTotalPrice() // Actualizar total, impuestos y subtotal
                    updateItemCount(itemCount) // Actualizar el número total de ítems

                    // Si hay productos en el carrito, habilitar el botón de Checkout
                    if (cartItems.isNotEmpty()) {
                        checkoutButton.isEnabled = true
                        updateCartStatus(true) // Marcar que el carrito tiene productos
                    } else {
                        // Si no hay productos en el carrito
                        totalPrice = 0.0 // Reiniciar el total
                        updateTotalPrice() // Actualizar el total a $0.00
                        updateItemCount(0) // No hay ítems
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

    // Eliminar un producto del carrito
    private fun removeCartItem(cartItem: CartItem) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("carts").document(userId).collection("items")
                .document(cartItem.id) // Usar el ID del producto para eliminarlo
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()

                    // Eliminar el símbolo de dólar antes de convertir a Double
                    val cleanPrice = cartItem.productPrice.toString().replace("$", "").toDouble()

                    // Restar el precio del producto eliminado
                    totalPrice -= cleanPrice

                    // Recargar los productos del carrito
                    loadCartItems()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al eliminar el producto", Toast.LENGTH_SHORT).show()
                }
        }
}

    // Actualizar el precio total mostrado en el TextView
    private fun updateTotalPrice() {
        // Calcular el impuesto (7% del subtotal)
        val taxes = totalPrice * 0.07

        // Calcular el total con impuestos
        val totalWithTaxes = totalPrice + taxes

        // Mostrar el subtotal sin impuestos
        val subtotalTextView = view?.findViewById<TextView>(R.id.subtotal_text_view)
        subtotalTextView?.text = "Subtotal: $${String.format("%.2f", totalPrice)}"

        // Mostrar los impuestos calculados
        val taxesTextView = view?.findViewById<TextView>(R.id.taxes_text_view)
        taxesTextView?.text = "Impuestos (7%): $${String.format("%.2f", taxes)}"

        // Mostrar el total con impuestos
        totalPriceTextView.text = "Total: $${String.format("%.2f", totalWithTaxes)}"
    }
    private fun updateItemCount(itemCount: Int) {
        val itemCountTextView = view?.findViewById<TextView>(R.id.item_count_text_view)
        itemCountTextView?.text = "Total de ítems: $itemCount"
    }


    private fun updateCartStatus(hasItems: Boolean) {
        if (isAdded) {
            val sharedPreferences = requireContext().getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("hasItemsInCart", hasItems)
            editor.apply()
        }
    }

    private fun showPaymentMethodScreen() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PaymentFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

}


