package com.example.ecostyle.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.Adapter.ProductAdapter
import com.example.ecostyle.viewmodel.ProductViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer


class ListFragment : Fragment() {

    private lateinit var productViewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_list, container, false) //listfragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_products)

        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

        /*
        // Initialize the adapter with an empty list
        productAdapter = ProductAdapter(emptyList())
        recyclerView.adapter = productAdapter

        // Initialize the ViewModel
        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)

        // Observe the LiveData from the ViewModel
        productViewModel.getProductList().observe(viewLifecycleOwner, Observer { products ->
            // Update the adapter's product list
            productAdapter.setProductList(products)
        })
        */

        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)

        productViewModel.getProductList().observe(viewLifecycleOwner, Observer { products ->
            productAdapter = ProductAdapter(products) { product ->
                val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                intent.putExtra("PRODUCT_ID", product.id) // Make sure `id` exists in `Product`
                Log.d("ListFragment", "Navigating to product details with ID: ${product.id}")
                startActivity(intent)
            }

            recyclerView.adapter = productAdapter
        })
    }
}