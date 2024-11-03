// ProductAdapter.kt
package com.example.ecostyle.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.R
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductAdapter(
    private var productList: List<Product>,
    private val onItemClicked: (Product) -> Unit,
    private val onLikeClicked: ((Product) -> Unit)? = null
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val addToCartButton: TextView = itemView.findViewById(R.id.add_to_cart_button)
        val likeButton: ImageView = itemView.findViewById(R.id.favorite_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productPrice.text = product.price.toString()

        Glide.with(holder.itemView.context)
            .load(product.imageResource)
            .into(holder.productImage)

        holder.itemView.setOnClickListener {
            onItemClicked(product)
        }

        holder.addToCartButton.setOnClickListener {
            addToCart(holder, product)
        }

        // Actualizar el ícono del botón de "like" basado en isFavorite
        val likeIconRes = if (product.isFavorite) R.drawable.baseline_favorite_24_2 else R.drawable.baseline_favorite_border_24
        holder.likeButton.setImageResource(likeIconRes)

        // Manejar clics en el botón de "like"
        holder.likeButton.setOnClickListener {
            toggleLikeProduct(holder, product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    private fun toggleLikeProduct(holder: ProductViewHolder, product: Product) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val likesRef = db.collection("users").document(userId).collection("likes")

            if (product.isFavorite) {
                // Quitar "like" al producto
                likesRef.document(product.firebaseId).delete()
                    .addOnSuccessListener {
                        product.isFavorite = false
                        notifyItemChanged(holder.adapterPosition)
                        onLikeClicked?.invoke(product)
                    }
                    .addOnFailureListener {
                        Toast.makeText(holder.itemView.context, "Error al quitar de favoritos", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Dar "like" al producto
                val likeData = hashMapOf(
                    "productId" to product.firebaseId
                )
                likesRef.document(product.firebaseId).set(likeData)
                    .addOnSuccessListener {
                        product.isFavorite = true
                        notifyItemChanged(holder.adapterPosition)
                        onLikeClicked?.invoke(product)
                    }
                    .addOnFailureListener {
                        Toast.makeText(holder.itemView.context, "Error al agregar a favoritos", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(holder.itemView.context, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToCart(holder: ProductViewHolder, product: Product) {
        // Tu implementación existente para agregar al carrito
    }

    fun setProductList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }
}
