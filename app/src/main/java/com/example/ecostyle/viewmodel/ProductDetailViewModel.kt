// ProductDetailViewModel.kt

package com.example.ecostyle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailViewModel : ViewModel() {

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> get() = _product

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun loadProduct(productId: Int) {
        // Cargar el producto desde Firestore usando el productId
        db.collection("Products")
            .whereEqualTo("id", productId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val product = document.toObject(Product::class.java)
                    product?.let {
                        it.firebaseId = document.id
                        checkIfFavorite(it)
                    }
                } else {
                    Log.e("ProductDetailViewModel", "Product not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetailViewModel", "Error loading product", e)
            }
    }

    private fun checkIfFavorite(product: Product) {
        if (userId != null) {
            db.collection("likes").document(userId).collection("items")
                .document(product.firebaseId)
                .get()
                .addOnSuccessListener { document ->
                    product.isFavorite = document.exists()
                    _product.value = product
                }
                .addOnFailureListener { e ->
                    Log.e("ProductDetailViewModel", "Error checking favorite status", e)
                    _product.value = product
                }
        } else {
            _product.value = product
        }
    }

    fun toggleFavorite() {
        val product = _product.value ?: return
        if (userId == null) {
            Log.e("ProductDetailViewModel", "User not authenticated")
            return
        }

        val likesRef = db.collection("likes").document(userId).collection("items")

        if (product.isFavorite) {
            // Eliminar de favoritos
            likesRef.document(product.firebaseId).delete()
                .addOnSuccessListener {
                    product.isFavorite = false
                    _product.value = product
                    Log.d("ProductDetailViewModel", "Product removed from favorites")
                }
                .addOnFailureListener { e ->
                    Log.e("ProductDetailViewModel", "Error removing from favorites", e)
                }
        } else {
            // Añadir a favoritos
            val likeItem = hashMapOf(
                "firebaseId" to product.firebaseId,
                "productName" to product.name,
                "productPrice" to product.price.toString(),
                "productImage" to product.imageResource,
                "timestamp" to System.currentTimeMillis()
            )
            likesRef.document(product.firebaseId).set(likeItem)
                .addOnSuccessListener {
                    product.isFavorite = true
                    _product.value = product
                    Log.d("ProductDetailViewModel", "Product added to favorites")
                }
                .addOnFailureListener { e ->
                    Log.e("ProductDetailViewModel", "Error adding to favorites", e)
                }
        }
    }
}
