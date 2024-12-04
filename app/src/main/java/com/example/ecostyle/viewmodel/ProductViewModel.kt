// ProductViewModel.kt
package com.example.ecostyle.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.*
import com.example.ecostyle.model.Product
import com.example.ecostyle.Repository.ProductRepository
import com.example.ecostyle.utils.LocalStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val productList: MutableLiveData<List<Product>> = MutableLiveData()
    private val repository = ProductRepository()

    private val _isEcoFriendlyFilterApplied = MutableLiveData<Boolean>()
    val isEcoFriendlyFilterApplied: LiveData<Boolean> get() = _isEcoFriendlyFilterApplied

    private val _isProximityFilterApplied = MutableLiveData<Boolean>(false)
    val isProximityFilterApplied: LiveData<Boolean> get() = _isProximityFilterApplied

    private val sharedPreferences: SharedPreferences = getApplication<Application>().getSharedPreferences("EcoStylePrefs", Context.MODE_PRIVATE)

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    // LiveData to store liked product IDs
    private val _likedProductIds = MutableLiveData<Set<String>>()
    val likedProductIds: LiveData<Set<String>> get() = _likedProductIds

    init {
        val isProximityFilterCached = sharedPreferences.getBoolean("proximity_filter", false)
        _isProximityFilterApplied.value = isProximityFilterCached
        listenToLikes()
        performInternalAnalysis()
    }

    fun getProductList(): LiveData<List<Product>> {
        return productList
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
    }

    fun setProximityFilter(applyFilter: Boolean) {
        _isProximityFilterApplied.value = applyFilter
        sharedPreferences.edit().putBoolean("proximity_filter", applyFilter).apply()
        reloadData()
    }

    fun reloadData() {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts()
                val productsWithLikes = updateProductsWithLikes(products)
                var filteredProducts = productsWithLikes

                // Apply proximity filter if enabled
                if (_isProximityFilterApplied.value == true) {
                    filteredProducts = applyProximityFilter(filteredProducts)
                }

                // Apply eco-friendly filter based on battery
                filteredProducts = filterProductsBasedOnBattery(filteredProducts)

                productList.value = filteredProducts
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products", e)
                productList.value = emptyList()
            }
        }
    }

    private fun applyProximityFilter(products: List<Product>): List<Product> {
        return products.filter { product ->
            if (product.latitude != null && product.longitude != null) {
                val distance = calculateDistance(userLatitude, userLongitude, product.latitude!!, product.longitude!!)
                distance <= 5.0
            } else {
                false
            }
        }
    }

    private suspend fun filterProductsBasedOnBattery(products: List<Product>): List<Product> {
        return try {
            val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

            if (batteryLevel <= 20) {
                _isEcoFriendlyFilterApplied.postValue(true)
                withContext(Dispatchers.Default) { products.filter { it.ecofriend } }
            } else {
                _isEcoFriendlyFilterApplied.postValue(false)
                products
            }
        } catch (e: Exception) {
            _isEcoFriendlyFilterApplied.postValue(false)
            products
        }
    }

    private fun updateProductsWithLikes(products: List<Product>): List<Product> {
        val likedIds = _likedProductIds.value ?: emptySet()
        return products.map { product ->
            product.isFavorite = likedIds.contains(product.firebaseId)
            product
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

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

                        // Update LiveData and local storage
                        _likedProductIds.postValue(likedIds)
                        LocalStorageManager.saveLikedProducts(getApplication(), likedIds)
                        // Reload products to reflect the updated state
                        reloadData()
                    }
                }
        }
    }

    fun performInternalAnalysis() {
        // Your internal analysis code here
    }
}
