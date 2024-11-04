// ProductAdapter.kt

package com.example.ecostyle.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Changed to ImageButton for consistency
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.R
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.model.Product
import com.example.ecostyle.utils.LocalStorageManager
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
        val likeButton: ImageButton = itemView.findViewById(R.id.favorite_icon) // Use ImageButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        val context = holder.itemView.context

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

        // Actualizar el icono del botón de "like" basado en el almacenamiento local
        val isLiked = LocalStorageManager.isProductLiked(context, product.firebaseId)
        val likeIconRes = if (isLiked) {
            R.drawable.baseline_favorite_24_2 // Ícono de corazón lleno
        } else {
            R.drawable.baseline_favorite_border_24 // Ícono de corazón vacío
        }
        holder.likeButton.setImageResource(likeIconRes)

        // Manejar clics en el botón de "like"
        holder.likeButton.setOnClickListener {
            toggleFavorite(holder, product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    private fun toggleFavorite(holder: ProductViewHolder, product: Product) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val context = holder.itemView.context

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
                                    notifyItemChanged(holder.adapterPosition)
                                    Toast.makeText(context, "${product.name} removed from favorites", Toast.LENGTH_SHORT).show()
                                    holder.likeButton.setImageResource(R.drawable.baseline_favorite_border_24)
                                    LocalStorageManager.removeLikedProduct(context, product.firebaseId) // Actualizar almacenamiento local
                                    onLikeClicked?.invoke(product)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error removing from favorites", Toast.LENGTH_SHORT).show()
                                    Log.e("ProductAdapter", "Error removing from favorites", e)
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
                                notifyItemChanged(holder.adapterPosition)
                                Toast.makeText(context, "${product.name} added to favorites", Toast.LENGTH_SHORT).show()
                                holder.likeButton.setImageResource(R.drawable.baseline_favorite_24_2)
                                LocalStorageManager.addLikedProduct(context, product.firebaseId) // Actualizar almacenamiento local
                                onLikeClicked?.invoke(product)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error adding to favorites", Toast.LENGTH_SHORT).show()
                                Log.e("ProductAdapter", "Error adding to favorites", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error accessing favorites", Toast.LENGTH_SHORT).show()
                    Log.e("ProductAdapter", "Error accessing favorites", e)
                }
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToCart(holder: ProductViewHolder, product: Product) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val productRef = db.collection("Products").document(product.firebaseId)

            productRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val availableQuantity = document.getLong("quantity")?.toInt() ?: 0

                    if (availableQuantity > 0) {
                        val cartRef = db.collection("carts").document(userId).collection("items")
                        cartRef.whereEqualTo("firebaseId", product.firebaseId).get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    // Incrementar la cantidad si ya existe en el carrito
                                    for (document in documents) {
                                        val cartItem = document.toObject(CartItem::class.java)
                                        val newQuantity = cartItem.quantity + 1

                                        if (newQuantity <= availableQuantity) {
                                            cartRef.document(document.id).update("quantity", newQuantity)
                                            Toast.makeText(holder.itemView.context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(holder.itemView.context, "No more stock available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    // Añadir producto por primera vez al carrito
                                    val cartItem = hashMapOf(
                                        "firebaseId" to product.firebaseId,
                                        "productName" to product.name,
                                        "productPrice" to product.price,
                                        "productImage" to product.imageResource,
                                        "quantity" to 1
                                    )
                                    cartRef.add(cartItem)
                                    Toast.makeText(holder.itemView.context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(holder.itemView.context, "No more stock available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun setProductList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }
}
