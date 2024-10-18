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

    // Filter products based on battery level
    private fun filterProductsBasedOnBattery(products: List<Product>): List<Product> {
        // Return all products if the product list is null
        if (products == null || products.isEmpty()) {
            return emptyList()
        }

        // Handle potential exceptions when accessing battery information
        return try {
            val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            Log.d("BatteryLevel", "Current battery level: $batteryLevel")


            if (batteryLevel <= 100) {
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
}
