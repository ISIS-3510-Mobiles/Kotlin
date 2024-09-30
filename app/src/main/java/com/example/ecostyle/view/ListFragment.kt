package com.example.ecostyle.view

import android.os.Bundle
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
        return inflater.inflate(R.layout.activity_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_products)

        // Set GridLayoutManager with 2 columns
        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

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
    }
}