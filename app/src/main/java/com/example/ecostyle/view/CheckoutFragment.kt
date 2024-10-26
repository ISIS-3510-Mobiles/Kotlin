package com.example.ecostyle.view

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
    private var totalPrice: Double = 0.0

    // Variable para almacenar las cantidades de stock localmente
    private val productStockMap = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checkout, container, false)

        totalPriceTextView = view.findViewById(R.id.total_price_text_view)
        recyclerView = view.findViewById(R.id.recycler_view_cart_items)
        recyclerView.layoutManager = LinearLayoutManager(context)

        cartAdapter = CartAdapter(emptyList(), { cartItem ->
            decreaseCartItemQuantity(cartItem)
        }, { cartItem ->
            updateCartItemQuantity(cartItem)
        })
        recyclerView.adapter = cartAdapter

        checkoutButton = view.findViewById(R.id.checkout_button)
        checkoutButton.isEnabled = false

        loadCartItems()

        // Verificar stock y proceder al pago
        checkoutButton.setOnClickListener {
            verifyStockBeforeCheckout()
        }

        return view
    }

    // Cargar productos del carrito y almacenar las cantidades de stock localmente
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

                        cartItem?.let {
                            // Asegúrate de que 'id' sea el firebaseId del producto correcto
                            cartItem.id = document.id // ID del documento de Firestore (firebaseId)
                            cartItems.add(cartItem)

                            // Actualizar precio y cantidad
                            val cleanPrice = cartItem.productPrice.replace("$", "").toDoubleOrNull() ?: 0.0
                            totalPrice += cleanPrice * cartItem.quantity
                            itemCount += cartItem.quantity
                        }
                    }

                    cartAdapter.setCartItems(cartItems)
                    updateTotalPrice()
                    updateItemCount(itemCount)

                    checkoutButton.isEnabled = cartItems.isNotEmpty()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error loading the cart", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Agregar y eliminar productos de forma local
    private fun decreaseCartItemQuantity(cartItem: CartItem) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            // Si la cantidad es mayor que 1, solo disminuimos la cantidad en el carrito
            if (cartItem.quantity > 1) {
                cartItem.quantity -= 1
                cartRef.document(cartItem.id).update("quantity", cartItem.quantity)
                    .addOnSuccessListener {
                        loadCartItems() // Actualizar la vista del carrito
                    }
            } else {
                // Si es 1, eliminamos el producto del carrito
                cartRef.document(cartItem.id).delete()
                    .addOnSuccessListener {
                        loadCartItems() // Recargar los productos del carrito
                    }
            }
        }
    }

    private fun updateCartItemQuantity(cartItem: CartItem) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("carts").document(userId).collection("items")
                .document(cartItem.id)
                .update("quantity", cartItem.quantity)
                .addOnSuccessListener {
                    loadCartItems() // Recargar los productos del carrito
                }
        }
    }

    // Verificar stock actual en Firebase al hacer checkout
    private fun verifyStockBeforeCheckout() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            cartRef.get().addOnSuccessListener { snapshot ->
                var allAvailable = true

                for (document in snapshot.documents) {
                    val cartItem = document.toObject(CartItem::class.java)

                    cartItem?.let {
                        if (cartItem.firebaseId.isNotEmpty()) {
                            val productRef = db.collection("Products").document(cartItem.firebaseId)

                            productRef.get().addOnSuccessListener { productDoc ->
                                if (productDoc.exists()) {
                                    val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0

                                    if (cartItem.quantity > availableQuantity) {
                                        allAvailable = false
                                        Toast.makeText(context, "La cantidad disponible de ${cartItem.productName} es solo $availableQuantity.", Toast.LENGTH_LONG).show()
                                    } else {
                                        // Si todo está bien, restamos la cantidad del producto en Firebase
                                        productRef.update("quantity", availableQuantity - cartItem.quantity)
                                    }
                                } else {
                                    allAvailable = false
                                    Toast.makeText(context, "Producto ${cartItem.productName} no encontrado en el inventario.", Toast.LENGTH_LONG).show()
                                }

                                if (allAvailable) {
                                    proceedToPayment() // Navegamos al fragmento de pago
                                }
                            }
                        } else {
                            Toast.makeText(context, "Error: ID del producto inválido.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }



    // Actualizar el precio total
    private fun updateTotalPrice() {
        totalPrice = cartAdapter.cartItemList.sumOf { cartItem ->
            val cleanPrice = cartItem.productPrice.replace("$", "").toDoubleOrNull() ?: 0.0
            cleanPrice * cartItem.quantity
        }
        val taxes = totalPrice * 0.07
        val totalWithTaxes = totalPrice + taxes
        totalPriceTextView.text = "Total: $${String.format("%.2f", totalWithTaxes)}"
    }

    private fun updateItemCount(itemCount: Int) {
        val itemCountTextView = view?.findViewById<TextView>(R.id.item_count_text_view)
        itemCountTextView?.text = "Total of items: $itemCount"
    }

    private fun proceedToPayment() {
        if (isAdded) {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, PaymentFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}





