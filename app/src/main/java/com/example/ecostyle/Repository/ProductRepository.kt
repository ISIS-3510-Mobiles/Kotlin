package com.example.ecostyle.Repository

import android.net.Uri
import android.util.Log
import com.example.ecostyle.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.*

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getProductById(productId: Int): Product? {
        return try {
            val documents = db.collection("Products")
                .whereEqualTo("id", productId)
                .get()
                .await() // Convertir a una llamada suspendida

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

    // Nueva función suspendida que devuelve la lista de productos
    suspend fun getProducts(): List<Product> {
        return try {
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
            emptyList() // En caso de error, devolver lista vacía
        }
    }

    // Publicar producto en Firestore
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
        return try {
            // Obtener el ID máximo actual de la base de datos
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

            // Generar un ID único para la imagen
            val imageId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("product_images/$imageId.jpg")

            // Subir la imagen a Firebase Storage
            storageRef.putFile(imageUri).await()

            // Obtener la URL de descarga de la imagen
            val downloadUrl = storageRef.downloadUrl.await()

            // Crear un mapa de datos para el producto
            val productData = hashMapOf(
                "id" to newId,
                "name" to name,
                "price" to price,
                "description" to description,
                "ecofriendly" to ecoFriendly,
                "imageResource" to downloadUrl.toString(),
                "isFavorite" to false,
                "latitude" to latitude,  // Guardar la latitud
                "longitude" to longitude,  // Guardar la longitud
                "quantity" to quantity,
                "brand" to brand,
                "initialPrice" to initialPrice
            )

            // Guardar los detalles del producto en Firestore
            db.collection("Products").add(productData).await()

            true  // Publicación exitosa
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error publishing product", e)
            false  // Error al guardar en Firestore
        }
    }

}


