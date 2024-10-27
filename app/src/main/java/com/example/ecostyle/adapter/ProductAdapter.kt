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

class ProductAdapter(private var productList: List<Product>, private val onItemClicked: (Product) -> Unit) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val addToCartButton: TextView = itemView.findViewById(R.id.add_to_cart_button)
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

        Glide.with(holder.itemView.context)
            .load(product.imageResource)
            .into(holder.productImage)

        holder.itemView.setOnClickListener {
            onItemClicked(product)
        }

        holder.addToCartButton.setOnClickListener {
            addToCart(holder, product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
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
                                            Toast.makeText(holder.itemView.context, "\n" +
                                                    "There is no more stock available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    // Añadir producto por primera vez al carrito con su firebaseId
                                    val cartItem = hashMapOf(
                                        "firebaseId" to product.firebaseId,  // Guardar el ID de producto
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
                        Toast.makeText(holder.itemView.context, "There is no more stock available", Toast.LENGTH_SHORT).show()
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
