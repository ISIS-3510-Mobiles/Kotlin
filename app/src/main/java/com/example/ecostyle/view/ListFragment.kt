// ListFragment.kt
package com.example.ecostyle.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Switch
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

class ListFragment : Fragment() {

    private lateinit var productViewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var ecoFriendlyMessage: TextView
    private lateinit var resetFilterButton: Switch
    private lateinit var proximityFilterButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        recyclerView = view.findViewById(R.id.recycler_view_products)
        ecoFriendlyMessage = view.findViewById(R.id.eco_friendly_message)
        resetFilterButton = view.findViewById(R.id.reset_filter_button)
        proximityFilterButton = view.findViewById(R.id.proximity_filter_button)

        // Set the default state to OFF
        resetFilterButton.isChecked = false

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

        productViewModel.getProductList().observe(viewLifecycleOwner) { products ->
            Log.d("ListFragment", "Observer triggered. Received ${products.size} products.")
            productAdapter.setProductList(products)
            Log.d("ListFragment", "RecyclerView updated with ${products.size} products.")
        }

        productViewModel.isEcoFriendlyFilterApplied.observe(viewLifecycleOwner) { isEcoFriendly ->
            if (isEcoFriendly) {
                showEcoFriendlyMessage()
                resetFilterButton.visibility = View.VISIBLE
            } else {
                hideEcoFriendlyMessage()
            }
        }

        productViewModel.isProximityFilterApplied.observe(viewLifecycleOwner) { isFiltered ->
            Log.d("ListFragment", "Proximity filter state: $isFiltered")
            resetFilterButton.isChecked = isFiltered
        }

        resetFilterButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Apply the proximity filter
                productViewModel.setProximityFilter(true)
            } else {
                // Remove the proximity filter
                productViewModel.setProximityFilter(false)
            }
        }

        checkLocationPermission()
    }

    // Function to log the event
    private fun logProductLikeEvent(productName: String) {
        val eventName = "liked_$productName"

        val analytics = FirebaseAnalytics.getInstance(requireContext())
        val bundle = Bundle()
        bundle.putString("message", "Number likes")
        analytics.logEvent(eventName, bundle)
    }

    private fun showEcoFriendlyMessage() {
        ecoFriendlyMessage.visibility = View.VISIBLE
    }

    private fun hideEcoFriendlyMessage() {
        ecoFriendlyMessage.visibility = View.GONE
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

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    saveCachedLocation(userLatitude, userLongitude)
                    productViewModel.setUserLocation(userLatitude, userLongitude)
                } else {
                    val cachedLocation = getCachedLocation()
                    if (cachedLocation != null) {
                        userLatitude = cachedLocation.first
                        userLongitude = cachedLocation.second
                        productViewModel.setUserLocation(userLatitude, userLongitude)
                    } else {
                        Toast.makeText(requireContext(), "Unable to retrieve location.", Toast.LENGTH_SHORT).show()
                    }
                }
                // Load data based on the switch's state
                productViewModel.reloadData()
            }
            .addOnFailureListener {
                // Handle failure
                val cachedLocation = getCachedLocation()
                if (cachedLocation != null) {
                    userLatitude = cachedLocation.first
                    userLongitude = cachedLocation.second
                    productViewModel.setUserLocation(userLatitude, userLongitude)
                } else {
                    Toast.makeText(requireContext(), "Unable to retrieve location.", Toast.LENGTH_SHORT).show()
                }
                productViewModel.reloadData()
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

        return if (!cachedLat.isNaN() && !cachedLon.isNaN()) {
            Pair(cachedLat.toDouble(), cachedLon.toDouble())
        } else {
            null
        }
    }

    private fun saveCachedLocation(latitude: Double, longitude: Double) {
        val sharedPreferences = requireContext().getSharedPreferences("EcoStylePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putFloat("cached_latitude", latitude.toFloat())
            putFloat("cached_longitude", longitude.toFloat())
            apply()
        }
    }
}
