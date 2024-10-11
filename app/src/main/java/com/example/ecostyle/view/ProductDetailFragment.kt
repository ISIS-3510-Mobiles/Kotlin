package com.example.ecostyle.view

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailFragment : Fragment() {

    private val viewModel: ProductDetailViewModel by viewModels()

    private var productId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener argumentos si se pasaron
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


        val productImage = view.findViewById<ImageView>(R.id.product_detail_image)
        val productName = view.findViewById<TextView>(R.id.product_detail_title)
        val productPrice = view.findViewById<TextView>(R.id.product_detail_price)
        val productDescription = view.findViewById<TextView>(R.id.product_detail_description)
        val favoriteButton = view.findViewById<ImageButton>(R.id.favorite_icon)
        val addToCartButton = view.findViewById<Button>(R.id.btn_add_to_cart)

        viewModel.product.observe(viewLifecycleOwner) { product ->
            Log.d("ProductDetailFragment", "Observed product: $product")
            productName.text = product.name
            productPrice.text = product.price
            productDescription.text = product.description

            Glide.with(this)
                .load(product.imageResource)
                .into(productImage)

            favoriteButton.setImageResource(
                if (product.isFavorite == true) R.drawable.baseline_favorite_24_2
                else R.drawable.baseline_favorite_border_24
            )
        }

        viewModel.loadProduct(productId)

        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        addToCartButton.setOnClickListener {
            viewModel.product.value?.let { product ->
                addToCart(product)
                Toast.makeText(requireContext(), "${product.name} añadido al carrito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToCart(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val cartItem = hashMapOf(
                "productName" to product.name,
                "productPrice" to product.price,
                "productImage" to product.imageResource,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("carts").document(userId).collection("items").add(cartItem)
                .addOnSuccessListener {
                    // Producto añadido exitosamente
                }
                .addOnFailureListener {
                    // Error al añadir el producto
                }
        }
    }
}
