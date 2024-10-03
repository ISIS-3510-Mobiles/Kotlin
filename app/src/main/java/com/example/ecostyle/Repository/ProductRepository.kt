// ProductRepository.kt
package com.example.ecostyle.Repository

import android.util.Log
import com.example.ecostyle.model.Product
import com.google.firebase.firestore.FirebaseFirestore

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()

    // Fetch a single product by ID
    fun getProductById(productId: Int, callback: (Product?) -> Unit) {
        val docRef = db.collection("Products").document(productId.toString())

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val product = document.toObject(Product::class.java)
                    callback(product) // Product found
                } else {
                    callback(null) // Product not found
                }
            }
            .addOnFailureListener {
                callback(null) // Handle failure case
            }
    }

    // Fetch all products from Firestore
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
                callback(emptyList()) // Handle failure
            }
    }
}
