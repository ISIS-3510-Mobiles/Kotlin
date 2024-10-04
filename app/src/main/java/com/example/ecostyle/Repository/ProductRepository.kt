// ProductRepository.kt
package com.example.ecostyle.Repository

import android.util.Log
import com.example.ecostyle.model.Product
import com.google.firebase.firestore.FirebaseFirestore

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()


    fun getProductById(productId: Int, callback: (Product?) -> Unit) {
        Log.d("ProductRepository", "Fetching product with ID field: $productId")

        db.collection("Products")
            .whereEqualTo("id", productId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val product = documents.documents[0].toObject(Product::class.java)
                    callback(product)
                    Log.d("ProductRepository", "Fetched product: $product")
                } else {
                    Log.d("ProductRepository", "Product not found with id: $productId")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProductRepository", "Error fetching product", exception)
                callback(null)
            }
    }
    fun getProducts(callback: (List<Product>) -> Unit) {
        db.collection("Products")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.mapNotNull { document ->
                    document.toObject(Product::class.java)
                }
                callback(productList)
            }
            .addOnFailureListener { e ->
                Log.e("ProductRepository", "Error getting products", e)
                callback(emptyList())
            }
    }
}
