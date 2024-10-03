package com.example.ecostyle.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.model.Product
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductAdapter(private var productList: List<Product>, private val onItemClicked: (Product) -> Unit) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val addToCartButton: TextView = itemView.findViewById(R.id.add_to_cart_button) // Botón para añadir al carrito
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productPrice.text = product.price
        holder.productImage.setImageResource(product.imageResource)

        holder.addToCartButton.setOnClickListener {
            addToCart(product)
            Toast.makeText(it.context, "${product.name} añadido al carrito", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return productList.size
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

    fun setProductList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }
}
