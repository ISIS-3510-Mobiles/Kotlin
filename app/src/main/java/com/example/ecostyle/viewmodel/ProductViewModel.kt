package com.example.ecostyle.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File



class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productList: MutableLiveData<List<Product>> = MutableLiveData()
    private val repository = ProductRepository()

    private val _isEcoFriendlyFilterApplied = MutableLiveData<Boolean>()
    val isEcoFriendlyFilterApplied: LiveData<Boolean> get() = _isEcoFriendlyFilterApplied

    private val sharedPreferences: SharedPreferences = getApplication<Application>().getSharedPreferences("EcoStylePrefs", Context.MODE_PRIVATE)

    private val _isProximityFilterApplied = MutableLiveData<Boolean>()
    val isProximityFilterApplied: LiveData<Boolean> get() = _isProximityFilterApplied

    // LiveData para almacenar los IDs de productos con "like"
    private val _likedProductIds = MutableLiveData<Set<String>>()
    val likedProductIds: LiveData<Set<String>> get() = _likedProductIds

    init {
/*
        loadProducts()
        listenToLikes()

        val isProximityFilterCached = sharedPreferences.getBoolean("proximity_filter", false)
        sharedPreferences.edit().putBoolean("proximity_filter", false).apply()

        Log.d("Initialization", "Proximity filter initialized as OFF.")
        _isProximityFilterApplied.value = isProximityFilterCached
        performInternalAnalysis()

 */
        val isProximityFilterCached = sharedPreferences.getBoolean("proximity_filter", false)
        _isProximityFilterApplied.value = isProximityFilterCached
        if (!isProximityFilterCached) {
            loadAllProducts()
        }
        listenToLikes()
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
        Log.d("ProductViewModel", "loadAllProducts called")

        viewModelScope.launch {
            try {
                val products = repository.getProducts() // Llamada suspendida
                val productsWithLikes = updateProductsWithLikes(products)
                _isEcoFriendlyFilterApplied.value = false
                _isProximityFilterApplied.value = false
                sharedPreferences.edit().putBoolean("proximity_filter", false).apply()

                productList.value = productsWithLikes

                Log.d("ProximityFilter", "Loaded all products. Proximity filter is OFF.")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading all products", e)
                productList.value = emptyList() // En caso de error, devolver lista vacía
            }
        }
    }

    fun loadProductsByProximity(userLatitude: Double, userLongitude: Double) {
        Log.d("ProductViewModel", "loadProductsByProximity called with location: $userLatitude, $userLongitude")
        Log.d("ProximityFilter", "User location: $userLatitude, $userLongitude")
        viewModelScope.launch {
            try {

                //if (_isProximityFilterApplied.value == false) return@launch


                saveLastKnownLocation(userLatitude, userLongitude)
                Log.d("ProximityFilter", "Saving user location: Latitude = $userLatitude, Longitude = $userLongitude")

                val products = repository.getProducts() // Llamada suspendida

                val productsWithLikes = updateProductsWithLikes(products)
                val filteredProducts = productsWithLikes.filter { product ->
                    Log.d("ProductCoordinates", "Product: ${product.name}, Latitude: ${product.latitude}, Longitude: ${product.longitude}")

                    if (product.latitude != null && product.longitude != null) {
                        val distance = calculateDistance(userLatitude, userLongitude, product.latitude!!, product.longitude!!)
                        Log.d("ProximityFilter", "Product: ${product.name}, Distance: $distance")
                        distance<= 5.0
                    } else {
                        Log.d("Product", "Product ${product.name} has null latitude/longitude")
                        false
                    }

                }
                Log.d("ProximityFilter", "Filtered products count: ${filteredProducts.size}")

                _isProximityFilterApplied.value = true
                sharedPreferences.edit().putBoolean("proximity_filter", true).apply()
                productList.value = filteredProducts
                Log.d("ProximityFilter", "productList updated with ${filteredProducts.size} products.")

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products by proximity", e)
                productList.value = emptyList() // En caso de error, devolver lista vacía
            }
        }
    }

    private suspend fun filterProductsBasedOnBattery(products: List<Product>): List<Product> {
        return try {
            val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

            if (batteryLevel <= 20) {
                _isEcoFriendlyFilterApplied.value = true
                withContext(Dispatchers.Default) { products.filter { it.ecofriend == true } }
            } else {
                _isEcoFriendlyFilterApplied.value = false
                products
            }
        } catch (e: Exception) {
            _isEcoFriendlyFilterApplied.value = false
            products
        }
    }


    private val distanceCache = mutableMapOf<Pair<String, String>, Double>()

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val key = Pair("$lat1-$lon1", "$lat2-$lon2")
        return distanceCache[key] ?: run {
            val earthRadius = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val distance = earthRadius * c
            distanceCache[key] = distance
            distance
        }
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

    private fun saveLastKnownLocation(latitude: Double, longitude: Double) {
        sharedPreferences.edit().apply {
            putFloat("cached_latitude", latitude.toFloat())
            putFloat("cached_longitude", longitude.toFloat())
            apply()
        }
    }
}
