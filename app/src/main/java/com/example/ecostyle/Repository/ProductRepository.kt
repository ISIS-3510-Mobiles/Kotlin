package com.example.ecostyle.Repository

import android.net.Uri
import android.util.Log
import com.example.ecostyle.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Obtener un producto por su ID y asignar firebaseId
    fun getProductById(productId: Int, callback: (Product?) -> Unit) {
        Log.d("ProductRepository", "Fetching product with ID field: $productId")

        db.collection("Products")
            .whereEqualTo("id", productId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val product = documents.documents[0].toObject(Product::class.java)
                    product?.firebaseId = documents.documents[0].id  // Asigna el firebaseId
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

    // Obtener todos los productos y asignar firebaseId a cada uno
    fun getProducts(callback: (List<Product>) -> Unit) {
        db.collection("Products")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.mapNotNull { document ->
                    val product = document.toObject(Product::class.java)
                    product.firebaseId = document.id  // Asigna el firebaseId a cada producto
                    product
                }
                callback(productList)
            }
            .addOnFailureListener { e ->
                Log.e("ProductRepository", "Error getting products", e)
                callback(emptyList())
            }
    }

    fun publishProductToFirestore(
        name: String,
        price: String,
        description: String,
        ecoFriendly: Boolean,
        imageUri: Uri,
        quantity: Int,
        callback: (Boolean) -> Unit,

    ) {
        // Generar un ID único para la imagen
        val imageId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("product_images/$imageId.jpg")

        // Subir la imagen a Firebase Storage
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                // Obtener la URL de descarga de la imagen
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Crear un mapa de datos para el producto
                    val productData = hashMapOf(
                        "name" to name,
                        "price" to price,
                        "description" to description,
                        "ecofriend" to ecoFriendly,
                        "imageResource" to uri.toString(),
                        "isFavorite" to false,
                        "latitude" to 0.0,
                        "longitude" to 0.0,
                        "quantity" to quantity
                    )

                    // Guardar los detalles del producto en Firestore
                    db.collection("Products")
                        .add(productData)
                        .addOnSuccessListener {
                            callback(true)  // Publicación exitosa
                        }
                        .addOnFailureListener {
                            callback(false)  // Error al guardar en Firestore
                        }
                }
            }
            .addOnFailureListener {
                callback(false)  // Error al subir la imagen
            }
    }
}

