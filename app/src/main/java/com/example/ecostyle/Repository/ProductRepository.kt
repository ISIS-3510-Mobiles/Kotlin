// ProductRepository.kt
package com.example.ecostyle.Repository

import android.util.Log
import com.example.ecostyle.model.Product
import com.google.firebase.firestore.FirebaseFirestore

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()

    /*
    fun getProductById(productId: Int, callback: (Product?) -> Unit) {

        Log.d("ProductRepository", "Fetching product with ID: $productId")

        val docRef = db.collection("Products").document(productId.toString())

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val product = document.toObject(Product::class.java)
                    callback(product)
                    Log.d("ProductRepository", "Fetched product: $product")
                } else {
                    callback(null) // Product not found
                }
            }
            .addOnFailureListener {
                callback(null) // Handle failure case
            }
    }


     */

    fun getProductById(productId: Int, callback: (Product?) -> Unit) {
        Log.d("ProductRepository", "Fetching product with ID field: $productId")

        // Query the collection for a document with the matching id field
        db.collection("Products")
            .whereEqualTo("id", productId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming there's only one product with this ID, get the first result
                    val product = documents.documents[0].toObject(Product::class.java)
                    callback(product)
                    Log.d("ProductRepository", "Fetched product: $product")
                } else {
                    Log.d("ProductRepository", "Product not found with id: $productId")
                    callback(null) // No product found
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProductRepository", "Error fetching product", exception)
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
