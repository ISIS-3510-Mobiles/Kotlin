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
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


class CheckoutFragment : Fragment() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkoutButton: Button
    private lateinit var totalPriceTextView: TextView
    private var totalPrice: Double = 0.0

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

        checkoutButton.setOnClickListener {
            verifyStockBeforeCheckout()
        }

        return view
    }

    private fun loadCartItems() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val documents = db.collection("carts").document(userId).collection("items").get().await()

                    val cartItems = mutableListOf<CartItem>()
                    totalPrice = 0.0
                    var itemCount = 0

                    for (document in documents) {
                        val cartItem = document.toObject(CartItem::class.java)

                        cartItem?.let {
                            cartItem.id = document.id
                            cartItems.add(cartItem)

                            val cleanPrice = cartItem.productPrice.replace("$", "").toDoubleOrNull() ?: 0.0
                            totalPrice += cleanPrice * cartItem.quantity
                            itemCount += cartItem.quantity
                        }
                    }

                    withContext(Dispatchers.Main) {
                        cartAdapter.setCartItems(cartItems)
                        updateTotalPrice()
                        updateItemCount(itemCount)

                        checkoutButton.isEnabled = cartItems.isNotEmpty()
                        updateCartStatus(cartItems.isNotEmpty())
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading the cart", Toast.LENGTH_SHORT).show()
                    }
                }
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
    private fun updateCartStatus(hasItems: Boolean) {
        if (isAdded) {
            val sharedPreferences = requireContext().getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("hasItemsInCart", hasItems)
            editor.apply()
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

            GlobalScope.launch(Dispatchers.IO) {
                val snapshot = cartRef.get().await() // Esperar a que la operación termine sin bloquear el hilo principal
                var allAvailable = true

                snapshot.documents.forEach { document ->
                    val cartItem = document.toObject(CartItem::class.java)
                    cartItem?.let {
                        if (cartItem.firebaseId.isNotEmpty()) {
                            val productRef = db.collection("Products").document(cartItem.firebaseId)

                            val productDoc = productRef.get().await()
                            if (productDoc.exists()) {
                                val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0

                                if (cartItem.quantity > availableQuantity) {
                                    allAvailable = false
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "The available quantity of  ${cartItem.productName} is just  $availableQuantity.", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    // Si todo está bien, restamos la cantidad del producto en Firebase
                                    productRef.update("quantity", availableQuantity - cartItem.quantity).await()
                                }
                            } else {
                                allAvailable = false
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Product ${cartItem.productName} not found in inventory.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                if (allAvailable) {
                    withContext(Dispatchers.Main) {
                        proceedToPayment() // Navegamos al fragmento de pago
                    }
                }
            }
        }
    }



    // Actualizar el precio total
    private fun updateTotalPrice() {
        // Calcula el subtotal sin impuestos
        val subtotal = cartAdapter.cartItemList.sumOf { cartItem ->
            val cleanPrice = cartItem.productPrice.replace("$", "").toDoubleOrNull() ?: 0.0
            cleanPrice * cartItem.quantity
        }

        // Calcula los impuestos (7%)
        val taxes = subtotal * 0.07

        // Calcula el total con impuestos
        val totalWithTaxes = subtotal + taxes

        // Actualiza los TextViews correspondientes en la interfaz
        val subtotalTextView = view?.findViewById<TextView>(R.id.subtotal_text_view)
        subtotalTextView?.text = "Subtotal: $${String.format("%.2f", subtotal)}"

        val taxesTextView = view?.findViewById<TextView>(R.id.taxes_text_view)
        taxesTextView?.text = "Taxes (7%): $${String.format("%.2f", taxes)}"

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





