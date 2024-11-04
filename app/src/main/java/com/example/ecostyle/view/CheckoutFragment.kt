package com.example.ecostyle.view

import android.content.Context
import android.net.ConnectivityManager
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

        // Inicializar las vistas
        totalPriceTextView = view.findViewById(R.id.total_price_text_view)
        recyclerView = view.findViewById(R.id.recycler_view_cart_items)
        checkoutButton = view.findViewById(R.id.checkout_button)

        recyclerView.layoutManager = LinearLayoutManager(context)

        cartAdapter = CartAdapter(emptyList(), { cartItem ->
            decreaseCartItemQuantity(cartItem)
        }, { cartItem ->
            updateCartItemQuantity(cartItem)
        })
        recyclerView.adapter = cartAdapter

        // Comportamiento del botón de checkout
        checkoutButton.setOnClickListener {
            if (isNetworkAvailable(requireContext())) {
                verifyStockBeforeCheckout()
            } else {
                Toast.makeText(context, "\n" +
                        "Payment cannot be made offline. Please try again later.", Toast.LENGTH_SHORT).show()
            }
        }

        loadCartItems()

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

                        updateCartStatus(cartItems.isNotEmpty())
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "\n" +
                                "Error loading cart.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // No permitir disminuir la cantidad de productos si no hay conexión
    private fun decreaseCartItemQuantity(cartItem: CartItem) {
        if (!isNetworkAvailable(requireContext())) {
            Toast.makeText(context, "You cannot modify the cart offline. Please try again later.", Toast.LENGTH_LONG).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            if (cartItem.quantity > 1) {
                cartItem.quantity -= 1
                cartRef.document(cartItem.id).update("quantity", cartItem.quantity)
                    .addOnSuccessListener {
                        loadCartItems()
                    }
            } else {
                cartRef.document(cartItem.id).delete()
                    .addOnSuccessListener {
                        loadCartItems()
                    }
            }
        }
    }

    // No permitir aumentar la cantidad de productos si no hay conexión
    private fun updateCartItemQuantity(cartItem: CartItem) {
        if (!isNetworkAvailable(requireContext())) {
            Toast.makeText(context, "You cannot modify the cart offline. Please try again later.", Toast.LENGTH_LONG).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("carts").document(userId).collection("items")
                .document(cartItem.id)
                .update("quantity", cartItem.quantity)
                .addOnSuccessListener {
                    loadCartItems()
                }
        }
    }

    private fun verifyStockBeforeCheckout() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            GlobalScope.launch(Dispatchers.IO) {
                val snapshot = cartRef.get().await()
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
                                        if (isAdded) {
                                            Toast.makeText(requireContext(), "\n" +
                                                    "The available quantity of ${cartItem.productName} is just $availableQuantity.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    productRef.update("quantity", availableQuantity - cartItem.quantity).await()
                                }
                            } else {
                                allAvailable = false
                                withContext(Dispatchers.Main) {
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), "Product ${cartItem.productName} not found in inventory.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                }

                if (allAvailable) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            proceedToPayment()
                        }
                    }
                }
            }
        }
    }

    private fun updateTotalPrice() {
        val subtotal = cartAdapter.cartItemList.sumOf { cartItem ->
            val cleanPrice = cartItem.productPrice.replace("$", "").toDoubleOrNull() ?: 0.0
            cleanPrice * cartItem.quantity
        }

        val taxes = subtotal * 0.07
        val totalWithTaxes = subtotal + taxes

        val subtotalTextView = view?.findViewById<TextView>(R.id.subtotal_text_view)
        subtotalTextView?.text = "Subtotal: $${String.format("%.2f", subtotal)}"

        val taxesTextView = view?.findViewById<TextView>(R.id.taxes_text_view)
        taxesTextView?.text = "Impuestos (7%): $${String.format("%.2f", taxes)}"

        totalPriceTextView.text = "Total: $${String.format("%.2f", totalWithTaxes)}"
    }

    private fun updateItemCount(itemCount: Int) {
        val itemCountTextView = view?.findViewById<TextView>(R.id.item_count_text_view)
        itemCountTextView?.text = "Total de artículos: $itemCount"
    }

    private fun updateCartStatus(hasItems: Boolean) {
        if (isAdded) {
            val sharedPreferences = requireContext().getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("hasItemsInCart", hasItems)
            editor.apply()
        }
    }

    private fun proceedToPayment() {
        if (isAdded) {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, PaymentFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    override fun onResume() {
        super.onResume()
        if (isNetworkAvailable(requireContext())) {
            syncCartWithFirebase()
        }
    }

    private fun syncCartWithFirebase() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val sharedPreferences = requireContext().getSharedPreferences("CartData", Context.MODE_PRIVATE)
            val allEntries = sharedPreferences.all

            if (allEntries.isNotEmpty()) {
                val cartRef = db.collection("carts").document(userId).collection("items")

                GlobalScope.launch(Dispatchers.IO) {
                    for ((key, value) in allEntries) {
                        val cartItemId = key
                        val quantity = value as Int

                        cartRef.document(cartItemId).update("quantity", quantity).await()
                    }

                    sharedPreferences.edit().clear().apply()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "\n" +
                                "Cart synced successfully.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}



