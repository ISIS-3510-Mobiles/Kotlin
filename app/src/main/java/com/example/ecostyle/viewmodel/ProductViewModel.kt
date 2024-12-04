package com.example.ecostyle.viewmodel

import com.example.ecostyle.activity.Notification
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

    private val _productState = MutableLiveData<ProductState>()
    val productState: LiveData<ProductState> get() = _productState

    private val _isEcoFriendlyFilterApplied = MutableLiveData<Boolean>()
    val isEcoFriendlyFilterApplied: LiveData<Boolean> get() = _isEcoFriendlyFilterApplied

    private val sharedPreferences: SharedPreferences = getApplication<Application>().getSharedPreferences("EcoStylePrefs", Context.MODE_PRIVATE)

    private val _isProximityFilterApplied = MutableLiveData<Boolean>()
    val isProximityFilterApplied: LiveData<Boolean> get() = _isProximityFilterApplied

    // LiveData para almacenar los IDs de productos con "like"
    private val _likedProductIds = MutableLiveData<Set<String>>()
    val likedProductIds: LiveData<Set<String>> get() = _likedProductIds

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading


    init {

        // Retrieve cached proximity filter state
        val isProximityFilterCached = sharedPreferences.getBoolean("proximity_filter", false)

        // Retrieve cached location
        val cachedLatitude = sharedPreferences.getFloat("cached_latitude", Float.NaN)
        val cachedLongitude = sharedPreferences.getFloat("cached_longitude", Float.NaN)

        // Validate cached location
        val isCachedLocationValid = !cachedLatitude.isNaN() && !cachedLongitude.isNaN()

        if (isProximityFilterCached && isCachedLocationValid) {
            Log.d("ProductViewModel", "Applying cached proximity filter with valid location.")
            _isProximityFilterApplied.value = false
            //loadProductsByProximity(cachedLatitude.toDouble(), cachedLongitude.toDouble())
            sharedPreferences.edit().putBoolean("proximity_filter", false).apply()
            loadAllProducts()

        } else {
            Log.d("ProductViewModel", "Cached proximity filter or location is invalid. Loading all products.")
            _isProximityFilterApplied.value = false
            sharedPreferences.edit().putBoolean("proximity_filter", false).apply()
            loadAllProducts()
        }
    }

    data class ResaleMetrics(
        val groupKey: String,
        val averageRVR: Double,
        val medianRVR: Double,
        val productCount: Int
    )

    data class ProductState(
        val isFiltered: Boolean,
        val products: List<Product>
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
            _isLoading.postValue(true)
            try {
                // Check for internet connection
                val hasInternet = hasInternetConnection() // No argument passed

                val products = if (hasInternet) {
                    // Fetch from Firestore and sync with Room
                    val fetchedProducts = repository.getProducts()
                    repository.syncWithLocalDatabase(fetchedProducts)
                    fetchedProducts
                } else {
                    // Fetch from Room
                    repository.getCachedProducts()
                }

                val productsWithLikes = updateProductsWithLikes(products)

                // Emit the state after fetching products
                _productState.postValue(ProductState(isFiltered = false, products = productsWithLikes))
                _isProximityFilterApplied.postValue(false)
                sharedPreferences.edit().putBoolean("proximity_filter", false).apply()

                Log.d("ProximityFilter", "Loaded all products. Proximity filter is OFF.")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading all products", e)
                _productState.postValue(ProductState(isFiltered = false, products = emptyList()))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadProductsByProximity(userLatitude: Double, userLongitude: Double) {
        if (_isProximityFilterApplied.value == false) return

        Log.d("ProductViewModel", "loadProductsByProximity called with location: $userLatitude, $userLongitude")

        viewModelScope.launch {
            _isLoading.postValue(true)
            val startTime = System.currentTimeMillis()

            try {
                val hasInternet = hasInternetConnection() // No argument passed

                val products = if (hasInternet) {
                    // Fetch from Firestore and sync with Room
                    val fetchedProducts = repository.getProducts()
                    repository.syncWithLocalDatabase(fetchedProducts)
                    fetchedProducts
                } else {
                    // Fetch from Room
                    repository.getCachedProducts()
                }

                val productsWithLikes = updateProductsWithLikes(products)

                val filteredProducts = productsWithLikes.filter { product ->
                    product.latitude != null && product.longitude != null &&
                            calculateDistance(userLatitude, userLongitude, product.latitude!!, product.longitude!!) <= 5.0
                }

                // Emit the state after filtering products
                _productState.postValue(ProductState(isFiltered = true, products = filteredProducts))
                _isProximityFilterApplied.postValue(true)
                sharedPreferences.edit().putBoolean("proximity_filter", true).apply()

                Log.d("ProximityFilter", "Filtered products count: ${filteredProducts.size}")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products by proximity", e)
                _productState.postValue(ProductState(isFiltered = true, products = emptyList()))
            } finally {
                _isLoading.postValue(false)
                Log.d("ProductViewModel", "loadProductsByProximity completed in ${System.currentTimeMillis() - startTime}ms")
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
        Log.d("DistanceCalc", "Calculating distance: From ($lat1, $lon1) to ($lat2, $lon2)")

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
            _isProximityFilterApplied.value = false
            loadAllProducts()
        } else {
            _isProximityFilterApplied.value = true
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

    fun setProximityFilter(isApplied: Boolean) {
        _isProximityFilterApplied.value = isApplied
        sharedPreferences.edit().putBoolean("proximity_filter", isApplied).apply()
    }

    private fun hasInternetConnection(): Boolean {
        val context = Notification.getAppContext()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

}
