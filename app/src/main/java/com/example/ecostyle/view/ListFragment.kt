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

class ListFragment : Fragment() {

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

        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

        productAdapter = ProductAdapter(emptyList(), { product ->
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
            productAdapter.setProductList(products)
        }
        productViewModel.isEcoFriendlyFilterApplied.observe(viewLifecycleOwner) { isEcoFriendly ->
            if (isEcoFriendly) {
                showEcoFriendlyMessage()
                resetFilterButton.visibility = View.VISIBLE
            } else {
                hideEcoFriendlyMessage()
//                resetFilterButton.visibility = View.GONE
            }
        }

        checkLocationPermission()


        productViewModel.isProximityFilterApplied.observe(viewLifecycleOwner) { isFiltered ->
            if (isFiltered) {
                resetFilterButton.visibility = View.VISIBLE
            } else {
//                resetFilterButton.visibility = View.GONE
            }
        }

        resetFilterButton.setOnClickListener {
            productViewModel.toggleProximityFilter(userLatitude, userLongitude)
        }
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

        if (hasInternetConnection()) {
            // Online mode: get live location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLatitude = location.latitude
                        userLongitude = location.longitude
                        productViewModel.loadProductsByProximity(userLatitude, userLongitude)
                    } else {
                        Log.d("ListFragment", "Location is null")
                        Toast.makeText(requireContext(), "Failed to retrieve location", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ListFragment", "Failed to get location", e)
                    Toast.makeText(requireContext(), "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Offline mode: use cached location
            Toast.makeText(requireContext(), "You are offline. Using last known location.", Toast.LENGTH_SHORT).show()
            productViewModel.loadProductsByProximity(userLatitude, userLongitude)  // Fallback to cached location
        }
    }
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

}
