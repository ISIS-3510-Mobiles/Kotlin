package com.example.ecostyle.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.ProductAdapter
import com.example.ecostyle.viewmodel.ProductViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics
import android.widget.ProgressBar


class ListFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var productViewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var ecoFriendlyMessage: TextView
    private lateinit var resetFilterButton: Button
    private lateinit var proximityFilterButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de que el layout corresponde al nuevo archivo sin la barra superior
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        recyclerView = view.findViewById(R.id.recycler_view_products)
        ecoFriendlyMessage = view.findViewById(R.id.eco_friendly_message)
        resetFilterButton = view.findViewById(R.id.reset_filter_button)
        proximityFilterButton = view.findViewById(R.id.proximity_filter_button)
        progressBar = view.findViewById(R.id.progress_bar)

        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

        productAdapter = ProductAdapter(emptyList(), { product ->

            product.name?.let { logProductLikeEvent(it) }

            val productDetailFragment = ProductDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("PRODUCT_ID", product.id)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, productDetailFragment)
                .addToBackStack(null)
                .commit()

            Log.d("ListFragment", "Navigating to product details with ID: ${product.id}")
        })




        recyclerView.adapter = productAdapter

        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)

        productViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("ListFragment", "Progress bar visibility updated: $isLoading")
            toggleProgressBar(isLoading) // Show/hide progress bar
        }

        productViewModel.productState.observe(viewLifecycleOwner) { state ->
            Log.d("ListFragment", "ProductState updated: ${state.products.size} items received")
            if (state.products.isNullOrEmpty()) {
                Log.d("ListFragment", "Empty product list, showing empty state message")
                ecoFriendlyMessage.visibility = View.VISIBLE
            } else {
                ecoFriendlyMessage.visibility = View.GONE
                productAdapter.setProductList(state.products)
                Log.d("ListFragment", "Product list updated in adapter")
            }
        }

        productViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            toggleProgressBar(isLoading) // Observe and toggle progress bar
        }
        checkLocationPermission()


        productViewModel.isProximityFilterApplied.observe(viewLifecycleOwner) { isFiltered ->
            Log.d("ListFragment", "Proximity filter state: $isFiltered")
            if (isFiltered) {
                resetFilterButton.visibility = View.VISIBLE
            } else {
//                resetFilterButton.visibility = View.GONE
            }
        }

        resetFilterButton.setOnClickListener {
            toggleProgressBar(true)
            productViewModel.toggleProximityFilter(userLatitude, userLongitude)
        }
    }

    // Función para registrar el evento
    private fun logProductLikeEvent(productName: String) {
        val eventName = "liked_$productName"

        val analytics = FirebaseAnalytics.getInstance(requireContext())
        val bundle = Bundle()
        bundle.putString("message", "Number likes")
        analytics.logEvent(eventName, bundle)
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getUserLocation()
                } else {
                    Log.d("ListFragment", "Location permission denied")
                }
            }
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        toggleProgressBar(true)

        if (hasInternetConnection()) {
            // Online mode: get live location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    toggleProgressBar(false)
                    if (location != null) {
                        userLatitude = location.latitude
                        userLongitude = location.longitude
                        saveCachedLocation(userLatitude, userLongitude)
                        productViewModel.loadProductsByProximity(userLatitude, userLongitude)
                    } else {
                        toggleProgressBar(false)
                        Log.d("ListFragment", "Live location is null. Using cached location.")
                        val cachedLocation = getCachedLocation()
                        if (cachedLocation != null) {
                            userLatitude = cachedLocation.first
                            userLongitude = cachedLocation.second
                            Log.d("ListFragment", "Using cached location: Latitude = $userLatitude, Longitude = $userLongitude")
                            productViewModel.loadProductsByProximity(userLatitude, userLongitude)
                        } else {
                            Log.d("ListFragment", "No cached location found. Loading all products.")
                            productViewModel.loadAllProducts()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    toggleProgressBar(false)
                    Log.e("ListFragment", "Failed to get live location", e)
                    val cachedLocation = getCachedLocation()
                    if (cachedLocation != null) {
                        userLatitude = cachedLocation.first
                        userLongitude = cachedLocation.second
                        productViewModel.loadProductsByProximity(userLatitude, userLongitude)
                    } else {
                        Toast.makeText(requireContext(), "Unable to retrieve location.", Toast.LENGTH_SHORT).show()
                        productViewModel.loadAllProducts()
                    }
                }
        } else {
            /*
            // Offline mode: use cached location
            Toast.makeText(requireContext(), "You are offline. Using last known location.", Toast.LENGTH_SHORT).show()
            productViewModel.loadProductsByProximity(userLatitude, userLongitude)  // Fallback to cached location

             */
            Log.d("ListFragment", "Offline. Using cached location.")
            val cachedLocation = getCachedLocation()
            if (cachedLocation != null) {
                userLatitude = cachedLocation.first
                userLongitude = cachedLocation.second
                productViewModel.loadProductsByProximity(userLatitude, userLongitude)
            } else {
                Log.d("ListFragment", "No cached location found. Loading all products.")
                productViewModel.loadAllProducts()
            }
        }
    }
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun getCachedLocation(): Pair<Double, Double>? {
        val sharedPreferences = requireContext().getSharedPreferences("EcoStylePrefs", Context.MODE_PRIVATE)
        val cachedLat = sharedPreferences.getFloat("cached_latitude", Float.NaN)
        val cachedLon = sharedPreferences.getFloat("cached_longitude", Float.NaN)
        Log.d("ListFragment", "Retrieved cached location: Latitude = $cachedLat, Longitude = $cachedLon")

        if (!cachedLat.isNaN() && !cachedLon.isNaN()) {
            return Pair(cachedLat.toDouble(), cachedLon.toDouble())
        } else {
            productViewModel.setProximityFilter(false)
            return null
        }
    }

    private fun saveCachedLocation(latitude: Double, longitude: Double) {
        val sharedPreferences = requireContext().getSharedPreferences("EcoStylePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putFloat("cached_latitude", latitude.toFloat())
            putFloat("cached_longitude", longitude.toFloat())
            apply()
        }
        Log.d("ListFragment", "Saved location: Latitude = $latitude, Longitude = $longitude")

    }

    private fun toggleProgressBar(isVisible: Boolean) {
        progressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }


}
