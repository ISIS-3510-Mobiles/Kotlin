package com.example.ecostyle.Repository


import android.content.Context

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.ecostyle.activity.Notification
import com.example.ecostyle.model.Product
import com.example.ecostyle.model.ProductEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.*

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val productDao = ProductDatabase.getDatabase(Notification.getAppContext()).productDao()


    /*
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

     */
    suspend fun getProducts(): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                if (hasInternetConnection()) {
                    val result = db.collection("Products").get().await()
                    val products = result.mapNotNull { document ->
                        document.toObject(Product::class.java)?.toEntity()
                    }

                    // Sync with local database
                    productDao.clearProducts()
                    productDao.insertProducts(products)

                    // Return fetched data
                    products.map { it.toProduct() }
                } else {
                    // Fetch from local database
                    productDao.getAllProducts().map { it.toProduct() }
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error fetching products", e)
                productDao.getAllProducts().map { it.toProduct() }
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = Notification.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
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
                    "initialPrice" to initialPrice,
                    "number_comments" to 0
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

    fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = id,
            name = name,
            price = price,
            imageResource = imageResource,
            description = description,
            ecoFriendly = ecofriend,
            latitude = latitude,
            longitude = longitude,
            quantity = quantity,
            brand = brand,
            initialPrice = initialPrice
        )
    }

    fun ProductEntity.toProduct(): Product {
        return Product(
            id = id,
            name = name,
            price = price,
            imageResource = imageResource,
            description = description,
            ecofriend = ecoFriendly,
            latitude = latitude,
            longitude = longitude,
            quantity = quantity,
            brand = brand,
            initialPrice = initialPrice
        )
    }

    suspend fun getCachedProducts(): List<Product> {
        return withContext(Dispatchers.IO) {
            productDao.getAllProducts().map { it.toProduct() }
        }
    }

    suspend fun syncWithLocalDatabase(products: List<Product>) {
        withContext(Dispatchers.IO) {
            productDao.clearProducts()
            productDao.insertProducts(products.map { it.toEntity() })
        }
    }

}


