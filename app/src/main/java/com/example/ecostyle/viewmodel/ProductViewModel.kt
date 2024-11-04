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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File



class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productList: MutableLiveData<List<Product>> = MutableLiveData()
    private val repository = ProductRepository()

    private val _isEcoFriendlyFilterApplied = MutableLiveData<Boolean>()
    val isEcoFriendlyFilterApplied: LiveData<Boolean> get() = _isEcoFriendlyFilterApplied

    private val _isProximityFilterApplied = MutableLiveData<Boolean>()
    val isProximityFilterApplied: LiveData<Boolean> get() = _isProximityFilterApplied

    init {
        loadProducts()

        performInternalAnalysis()

    }

    data class ResaleMetrics(
        val groupKey: String,
        val averageRVR: Double,
        val medianRVR: Double,
        val productCount: Int
    )

    fun getProductList(): LiveData<List<Product>> {
        return productList
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts() // Llamada suspendida
                productList.value = filterProductsBasedOnBattery(products)
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products", e)
                productList.value = emptyList() // En caso de error, devolver lista vacía
            }
        }
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts() // Llamada suspendida
                _isEcoFriendlyFilterApplied.value = false
                _isProximityFilterApplied.value = false
                productList.value = products
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

    fun performInternalAnalysis() {
        viewModelScope.launch {
            try {
                val products = repository.getProducts()

                if (products.isNotEmpty()) {
                    val metricsByBrand = calculateResaleMetrics(products) { it.brand }

                    // Log the results
                    printResaleMetrics(metricsByBrand, "Brand")

                    // Save results to CSV files
                    saveMetricsToCsv(metricsByBrand, "brand_metrics.csv")
                } else {
                    Log.d("ProductViewModel", "No products available for analysis.")
                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error performing internal analysis", e)
            }
        }
    }

    // Helper function to parse price strings to Double
    private fun parsePrice(priceStr: String?): Double? {
        return priceStr?.replace(Regex("[^\\d.]"), "")?.toDoubleOrNull()
    }

    // Function to calculate resale metrics grouped by a key (brand or product type)
    private fun calculateResaleMetrics(
        products: List<Product>,
        groupBy: (Product) -> String?
    ): List<ResaleMetrics> {
        return products.groupBy { groupBy(it) ?: "Unknown" }
            .mapNotNull { (groupKey, productsInGroup) ->
                val rvrValues = productsInGroup.mapNotNull { product ->
                    val initialPrice = parsePrice(product.initialPrice)
                    val resalePrice = parsePrice(product.price)
                    if (initialPrice != null && resalePrice != null && initialPrice > 0) {
                        (resalePrice / initialPrice) * 100
                    } else {
                        null
                    }
                }

                if (rvrValues.isNotEmpty()) {
                    ResaleMetrics(
                        groupKey = groupKey,
                        averageRVR = rvrValues.average(),
                        medianRVR = rvrValues.median(),
                        productCount = rvrValues.size
                    )
                } else {
                    null
                }
            }
    }

    // Extension function to calculate the median of a list of Doubles
    private fun List<Double>.median(): Double {
        if (isEmpty()) return 0.0
        val sortedList = sorted()
        val middle = size / 2
        return if (size % 2 == 0) {
            (sortedList[middle - 1] + sortedList[middle]) / 2
        } else {
            sortedList[middle]
        }
    }

    // Function to log the resale metrics
    private fun printResaleMetrics(metricsList: List<ResaleMetrics>, groupBy: String) {
        Log.d("ResaleMetrics", "Resale Metrics Grouped by $groupBy:")
        metricsList.forEach { metrics ->
            Log.d("ResaleMetrics", "Group: ${metrics.groupKey}")
            Log.d("ResaleMetrics", "Average RVR: ${"%.2f".format(metrics.averageRVR)}%")
            Log.d("ResaleMetrics", "Median RVR: ${"%.2f".format(metrics.medianRVR)}%")
            Log.d("ResaleMetrics", "Number of Products: ${metrics.productCount}")
            Log.d("ResaleMetrics", "----------------------------")
        }
    }

    // Function to save the metrics to a CSV file
    private suspend fun saveMetricsToCsv(metricsList: List<ResaleMetrics>, fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val file = File(context.filesDir, fileName)

                file.printWriter().use { out ->
                    out.println("Group,Average RVR,Median RVR,Product Count")
                    metricsList.forEach { metrics ->
                        out.println("${metrics.groupKey},${metrics.averageRVR},${metrics.medianRVR},${metrics.productCount}")
                    }
                }

                Log.d("ResaleMetrics", "Metrics saved to ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("ResaleMetrics", "Error saving metrics to CSV", e)
            }
        }
    }
}

