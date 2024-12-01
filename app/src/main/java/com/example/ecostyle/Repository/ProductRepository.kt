package com.example.ecostyle.Repository

import android.net.Uri
import android.util.Log
import com.example.ecostyle.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.*

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getProductById(productId: Int): Product? {
        return withContext(Dispatchers.IO) {
            try {
                val documents = db.collection("Products")
                    .whereEqualTo("id", productId)
                    .get()
                    .await()

                if (!documents.isEmpty) {
                    val product = documents.documents[0].toObject(Product::class.java)
                    product?.firebaseId = documents.documents[0].id  // Asigna el firebaseId
                    product
                } else {
                    Log.d("ProductRepository", "Product not found with id: $productId")
                    null
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error fetching product", e)
                null
            }
        }
    }

    suspend fun getProducts(): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("Products")
                    .get()
                    .await()

                result.mapNotNull { document ->
                    val product = document.toObject(Product::class.java)
                    product.firebaseId = document.id
                    product
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error getting products", e)
                emptyList()
            }
        }
    }

    suspend fun publishProductToFirestore(
        name: String,
        price: String,
        description: String,
        ecoFriendly: Boolean,
        imageUri: Uri,
        quantity: Int,
        latitude: Double,
        longitude: Double,
        brand: String,
        initialPrice: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val querySnapshot = db.collection("Products")
                    .orderBy("id", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()

                val newId = if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                    val lastId = querySnapshot.documents[0].getLong("id") ?: 0L
                    lastId + 1
                } else {
                    1L
                }

                val imageId = UUID.randomUUID().toString()
                val storageRef = storage.reference.child("product_images/$imageId.jpg")

                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await()

                val productData = hashMapOf(
                    "id" to newId,
                    "name" to name,
                    "price" to price,
                    "description" to description,
                    "ecofriendly" to ecoFriendly,
                    "imageResource" to downloadUrl.toString(),
                    "isFavorite" to false,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "quantity" to quantity,
                    "brand" to brand,
                    "initialPrice" to initialPrice
                )

                val productRef = db.collection("Products").add(productData).await()
                val productId = productRef.id

                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    addToSalesHistory(userId, productId, productData)
                }

                true
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error publishing product", e)
                false
            }
        }
    }


    private suspend fun addToSalesHistory(userId: String, productId: String, productData: Map<String, Any>) {
        val historyRef = db.collection("historial").document(userId)

        val productWithId = productData.toMutableMap()
        productWithId["productId"] = productId

        val documentSnapshot = historyRef.get().await()
        if (documentSnapshot.exists()) {
            historyRef.update("ventas", com.google.firebase.firestore.FieldValue.arrayUnion(productWithId)).await()
        } else {
            val initialData = hashMapOf(
                "ventas" to listOf(productWithId),
                "compras" to emptyList<Map<String, Any>>()
            )
            historyRef.set(initialData).await()
        }
    }

}


