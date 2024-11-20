package com.example.ecostyle.view

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.ecostyle.R
import com.example.ecostyle.model.Product
import com.example.ecostyle.viewmodel.ProductDetailViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.utils.LocalStorageManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.FirebaseAnalyticsLegacyRegistrar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ProductDetailFragment : Fragment() {

    private val viewModel: ProductDetailViewModel by viewModels()

    private var productId: Int = -1

    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var addToCartButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            productId = it.getInt("PRODUCT_ID", -1)
        }

        if (productId == -1) {
            Log.e("ProductDetailFragment", "Invalid productId received!")
        } else {
            Log.d("ProductDetailFragment", "Received productId: $productId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productImage = view.findViewById(R.id.product_detail_image)
        productName = view.findViewById(R.id.product_detail_title)
        productPrice = view.findViewById(R.id.product_detail_price)
        productDescription = view.findViewById(R.id.product_detail_description)
        favoriteButton = view.findViewById(R.id.favorite_icon)
        addToCartButton = view.findViewById(R.id.btn_add_to_cart)

        viewModel.product.observe(viewLifecycleOwner) { product ->
            productName.text = product.name
            productPrice.text = product.price.toString()
            productDescription.text = product.description

            Glide.with(this)
                .load(product.imageResource)
                .into(productImage)

            // Verificar si el producto está marcado como favorito en el almacenamiento local
            product.isFavorite = LocalStorageManager.isProductLiked(requireContext(), product.firebaseId)

            // Actualizar el icono de "like"
            updateLikeIcon(product.isFavorite)
        }

        viewModel.loadProduct(productId)

        favoriteButton.setOnClickListener {
            val product = viewModel.product.value
            if (product != null) {
                if (!hasInternetConnection()) {
                    Toast.makeText(context, "No Internet connection", Toast.LENGTH_SHORT).show()

                } else {
                    toggleFavorite(product)
                }
            }
        }

        addToCartButton.setOnClickListener {
            viewModel.product.value?.let { product ->
                addToCart(product)
            }
        }
    }

    private fun toggleFavorite(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val context = requireContext()

        if (userId != null) {
            val likesRef = db.collection("likes").document(userId).collection("items")

            likesRef.whereEqualTo("firebaseId", product.firebaseId).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // El producto ya tiene "like", así que lo eliminamos
                        for (document in documents) {
                            likesRef.document(document.id).delete()
                                .addOnSuccessListener {
                                    product.isFavorite = false
                                    updateLikeIcon(product.isFavorite)
                                    Toast.makeText(context, "${product.name} removed from favorites", Toast.LENGTH_SHORT).show()
                                    LocalStorageManager.removeLikedProduct(context, product.firebaseId)

                                    // Aquí puedes registrar el evento de 'like' eliminado
                                    product.name?.let { it1 -> logLikeEvent(it1, false) }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error removing from favorites", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Añadir el producto a likes
                        val likeItem = hashMapOf(
                            "firebaseId" to product.firebaseId,
                            "productName" to product.name,
                            "productPrice" to product.price,
                            "productImage" to product.imageResource,
                            "timestamp" to System.currentTimeMillis()
                        )
                        likesRef.add(likeItem)
                            .addOnSuccessListener {
                                product.isFavorite = true
                                updateLikeIcon(product.isFavorite)
                                Toast.makeText(context, "${product.name} added to favorites", Toast.LENGTH_SHORT).show()
                                LocalStorageManager.addLikedProduct(context, product.firebaseId)

                                // Aquí puedes registrar el evento de 'like' añadido
                                product.name?.let { it1 -> logLikeEvent(it1, true) }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error adding to favorites", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error accessing favorites", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para registrar el evento
    private fun logLikeEvent(productName: String, liked: Boolean) {
        val eventName = if (liked) "liked_$productName" else "unliked_$productName"
        // Aquí puedes usar tu herramienta de analytics para registrar el evento
        val analytics = FirebaseAnalytics.getInstance(requireContext())
        val bundle = Bundle()
        bundle.putString("message", "Number likes")
        analytics.logEvent(eventName, bundle)
    }


    private fun updateLikeIcon(isFavorite: Boolean) {
        val likeIconRes = if (isFavorite) {
            R.drawable.baseline_favorite_24_2 // Ícono de corazón lleno
        } else {
            R.drawable.baseline_favorite_border_24 // Ícono de corazón vacío
        }
        favoriteButton.setImageResource(likeIconRes)
    }

    // Añadir productos al carrito
    private fun addToCart(product: Product) {
        val db = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    // Verificar si el producto ya está en el carrito
                    val documents = cartRef.whereEqualTo("firebaseId", product.firebaseId).get().await()

                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val cartItem = document.toObject(CartItem::class.java)
                            val currentQuantity = cartItem.quantity

                            // Actualizar la cantidad
                            cartRef.document(document.id)
                                .update("quantity", currentQuantity + 1)
                                .await()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Updated quantity in cart", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Añadir el producto con su firebaseId
                        val cartItem = hashMapOf(
                            "firebaseId" to product.firebaseId,
                            "productName" to product.name,
                            "productPrice" to product.price,
                            "productImage" to product.imageResource,
                            "quantity" to 1,
                            "timestamp" to System.currentTimeMillis()
                        )

                        cartRef.add(cartItem).await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
