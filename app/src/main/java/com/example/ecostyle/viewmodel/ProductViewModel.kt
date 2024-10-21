// ProductViewModel.kt
package com.example.ecostyle.viewmodel

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.Product
import com.example.ecostyle.Repository.ProductRepository

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productList: MutableLiveData<List<Product>> = MutableLiveData()
    private val repository = ProductRepository()


    private val _isEcoFriendlyFilterApplied = MutableLiveData<Boolean>()
    val isEcoFriendlyFilterApplied: LiveData<Boolean> get() = _isEcoFriendlyFilterApplied

    private val _isProximityFilterApplied = MutableLiveData<Boolean>()
    val isProximityFilterApplied: LiveData<Boolean> get() = _isProximityFilterApplied

    init {
        loadProducts()
    }

    fun getProductList(): LiveData<List<Product>> {
        return productList
    }

    private fun loadProducts() {
        repository.getProducts { products ->
            productList.value = filterProductsBasedOnBattery(products)
        }
    }
    fun loadAllProducts() {
        repository.getProducts { products ->
            _isEcoFriendlyFilterApplied.value = false
            _isProximityFilterApplied.value = false
            productList.value = products
        }
    }

    fun loadProductsByProximity(userLatitude: Double, userLongitude: Double) {
        repository.getProducts { products ->
            val filteredProducts = products.filter { product ->
                if (product.latitude != null && product.longitude != null) {
                    calculateDistance(userLatitude, userLongitude, product.latitude!!, product.longitude!!) <= 10.0
                } else {
                    Log.d("Product", "Product ${product.name} has null latitude/longitude")
                    false
                }
            }
            _isProximityFilterApplied.value = true
            productList.value = filteredProducts
        }
    }

    private fun filterProductsBasedOnBattery(products: List<Product>): List<Product> {
        if (products == null || products.isEmpty()) {
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
}