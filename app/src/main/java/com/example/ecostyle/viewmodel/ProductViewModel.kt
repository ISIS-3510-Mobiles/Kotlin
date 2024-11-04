// ProductViewModel.kt

package com.example.ecostyle.viewmodel

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ecostyle.model.Product
import com.example.ecostyle.Repository.ProductRepository
import com.example.ecostyle.utils.LocalStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productList: MutableLiveData<List<Product>> = MutableLiveData()
    private val repository = ProductRepository()

    private val _isEcoFriendlyFilterApplied = MutableLiveData<Boolean>()
    val isEcoFriendlyFilterApplied: LiveData<Boolean> get() = _isEcoFriendlyFilterApplied

    private val _isProximityFilterApplied = MutableLiveData<Boolean>()
    val isProximityFilterApplied: LiveData<Boolean> get() = _isProximityFilterApplied

    // LiveData para almacenar los IDs de productos con "like"
    private val _likedProductIds = MutableLiveData<Set<String>>()
    val likedProductIds: LiveData<Set<String>> get() = _likedProductIds

    init {
        loadProducts()
        listenToLikes()
    }

    fun getProductList(): LiveData<List<Product>> {
        return productList
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts() // Llamada suspendida
                val productsWithLikes = updateProductsWithLikes(products)
                productList.value = filterProductsBasedOnBattery(productsWithLikes)
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products", e)
                productList.value = emptyList() // En caso de error, devolver lista vacía
            }
        }
    }

    // Actualizar productos con el estado de "like" basado en el almacenamiento local
    private fun updateProductsWithLikes(products: List<Product>): List<Product> {
        val likedIds = _likedProductIds.value ?: emptySet()
        return products.map { product ->
            product.isFavorite = likedIds.contains(product.firebaseId)
            product
        }
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts() // Llamada suspendida
                val productsWithLikes = updateProductsWithLikes(products)
                _isEcoFriendlyFilterApplied.value = false
                _isProximityFilterApplied.value = false
                productList.value = productsWithLikes
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading all products", e)
                productList.value = emptyList() // En caso de error, devolver lista vacía
            }
        }
    }

    fun loadProductsByProximity(userLatitude: Double, userLongitude: Double) {
        viewModelScope.launch {
            try {
                val products = repository.getProducts() // Llamada suspendida
                val productsWithLikes = updateProductsWithLikes(products)
                val filteredProducts = productsWithLikes.filter { product ->
                    if (product.latitude != null && product.longitude != null) {
                        calculateDistance(userLatitude, userLongitude, product.latitude!!, product.longitude!!) <= 10.0
                    } else {
                        Log.d("Product", "Product ${product.name} has null latitude/longitude")
                        false
                    }
                }
                _isProximityFilterApplied.value = true
                productList.value = filteredProducts
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products by proximity", e)
                productList.value = emptyList() // En caso de error, devolver lista vacía
            }
        }
    }

    private fun filterProductsBasedOnBattery(products: List<Product>): List<Product> {
        if (products.isEmpty()) {
            return emptyList()
        }

        return try {
            val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            Log.d("BatteryLevel", "Current battery level: $batteryLevel")

            if (batteryLevel <= 20) {
                _isEcoFriendlyFilterApplied.value = true
                products.filter { it.ecofriend == true }
            } else {
                _isEcoFriendlyFilterApplied.value = false
                products
            }
        } catch (e: Exception) {
            _isEcoFriendlyFilterApplied.value = false
            products
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    fun toggleProximityFilter(userLatitude: Double, userLongitude: Double) {
        if (_isProximityFilterApplied.value == true) {
            // Si el filtro de proximidad ya está aplicado, cargar todos los productos
            loadAllProducts()
        } else {
            // Si no está aplicado, cargar productos dentro de los 10 km
            loadProductsByProximity(userLatitude, userLongitude)
        }
    }

    // Escuchar cambios en los likes y actualizar el almacenamiento local
    private fun listenToLikes() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("likes").document(userId).collection("items")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("ProductViewModel", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val likedIds = snapshot.documents.mapNotNull { doc ->
                            doc.getString("firebaseId")
                        }.toSet()

                        // Actualizar LiveData y almacenamiento local
                        _likedProductIds.postValue(likedIds)
                        LocalStorageManager.saveLikedProducts(getApplication(), likedIds)
                        // Recargar productos para reflejar el estado actualizado
                        loadProducts()
                    }
                }
        }
    }
}
