package com.example.ecostyle.view

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.CartAdapter
import com.example.ecostyle.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class CheckoutFragment : Fragment() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkoutButton: Button
    private lateinit var totalPriceTextView: TextView
    private var totalPrice: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checkout, container, false)

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

        checkoutButton.setOnClickListener {
            if (isNetworkAvailable(requireContext())) {
                if (cartAdapter.itemCount > 0) {
                    verifyStockBeforeCheckout()
                } else {
                    Toast.makeText(
                        context,
                        "Your cart is empty. Please add items to your cart before checking out.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Payment cannot be made offline. Please try again later.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


        loadCartItems()

        return view
    }

    private fun loadCartItems() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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
                        Toast.makeText(context, "Error loading cart.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun decreaseCartItemQuantity(cartItem: CartItem) {
        if (!isNetworkAvailable(requireContext())) {
            Toast.makeText(
                context,
                "You cannot modify the cart offline. Please try again later.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (cartItem.quantity > 1) {
                    cartItem.quantity -= 1
                    db.collection("carts").document(userId).collection("items")
                        .document(cartItem.id).update("quantity", cartItem.quantity).await()
                } else {
                    db.collection("carts").document(userId).collection("items")
                        .document(cartItem.id).delete().await()
                }
                loadCartItems()
            }
        }
    }

    private fun updateCartItemQuantity(cartItem: CartItem) {
        if (!isNetworkAvailable(requireContext())) {
            Toast.makeText(
                context,
                "You cannot modify the cart offline. Please try again later.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                db.collection("carts").document(userId).collection("items")
                    .document(cartItem.id).update("quantity", cartItem.quantity).await()
                loadCartItems()
            }
        }
    }

    private fun verifyStockBeforeCheckout() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val cartRef = db.collection("carts").document(userId).collection("items")
                val snapshot = cartRef.get().await()
                var allAvailable = true

                for (document in snapshot.documents) {
                    val cartItem = document.toObject(CartItem::class.java)
                    cartItem?.let {
                        if (cartItem.firebaseId.isNotEmpty()) {
                            val productRef = db.collection("Products").document(cartItem.firebaseId)
                            val productDoc = productRef.get().await()
                            val availableQuantity = productDoc.getLong("quantity")?.toInt() ?: 0

                            if (cartItem.quantity > availableQuantity) {
                                allAvailable = false
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
                                        "The available quantity of ${cartItem.productName} is $availableQuantity.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }

                if (allAvailable) {
                    withContext(Dispatchers.Main) {
                        proceedToPayment(snapshot.documents.mapNotNull { it.toObject(CartItem::class.java) })
                    }
                }
            }
        }
    }

    private fun proceedToPayment(cartItems: List<CartItem>) {
        if (isAdded) {
            val paymentFragment = PaymentFragment()
            val bundle = Bundle()
            bundle.putParcelableArrayList("cartItems", ArrayList(cartItems))
            paymentFragment.arguments = bundle

            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, paymentFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun updateTotalPrice() {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        numberFormat.maximumFractionDigits = 0

        val subtotal = cartAdapter.cartItemList.sumOf { cartItem ->
            val cleanPrice = cartItem.productPrice.replace(".", "").toDoubleOrNull() ?: 0.0
            cleanPrice * cartItem.quantity
        }

        val taxes = subtotal * 0.07
        val totalWithTaxes = subtotal + taxes

        val subtotalTextView = view?.findViewById<TextView>(R.id.subtotal_text_view)
        subtotalTextView?.text = "Subtotal: ${numberFormat.format(subtotal)}"

        val taxesTextView = view?.findViewById<TextView>(R.id.taxes_text_view)
        taxesTextView?.text = "Taxes (7%): ${numberFormat.format(taxes)}"

        totalPriceTextView.text = "Total: ${numberFormat.format(totalWithTaxes)}"
    }

    private fun updateItemCount(itemCount: Int) {
        val itemCountTextView = view?.findViewById<TextView>(R.id.item_count_text_view)
        itemCountTextView?.text = "Total items: $itemCount"
    }

    private fun updateCartStatus(hasItems: Boolean) {
        if (isAdded) {
            val sharedPreferences =
                requireContext().getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("hasItemsInCart", hasItems)
            editor.apply()
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}

