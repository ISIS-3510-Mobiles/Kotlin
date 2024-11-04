// ProductDetailFragment.kt

package com.example.ecostyle.view

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

            // Actualizar el icono del botón de favoritos basado en isFavorite
            val likeIconRes = if (product.isFavorite) {
                R.drawable.baseline_favorite_24_2 // Ícono de corazón lleno
            } else {
                R.drawable.baseline_favorite_border_24 // Ícono de corazón vacío
            }
            favoriteButton.setImageResource(likeIconRes)
        }

        viewModel.loadProduct(productId)

        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        addToCartButton.setOnClickListener {
            viewModel.product.value?.let { product ->
                addToCart(product)
            }
        }
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
}
