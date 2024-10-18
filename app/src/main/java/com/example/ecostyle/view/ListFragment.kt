package com.example.ecostyle.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.Adapter.ProductAdapter
import com.example.ecostyle.viewmodel.ProductViewModel

class ListFragment : Fragment() {

    private lateinit var productViewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var ecoFriendlyMessage: TextView
    private lateinit var resetFilterButton: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_list, container, false) // Asegúrate de tener este layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_products)
        ecoFriendlyMessage = view.findViewById(R.id.eco_friendly_message)
        resetFilterButton = view.findViewById(R.id.reset_filter_button)

        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

        productAdapter = ProductAdapter(emptyList()) { product ->
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
        }

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
                resetFilterButton.visibility = View.GONE
            }
        }
        resetFilterButton.setOnClickListener {
            productViewModel.loadAllProducts()
        }
    }
    private fun showEcoFriendlyMessage() {
        ecoFriendlyMessage.visibility = View.VISIBLE
    }

    private fun hideEcoFriendlyMessage() {
        ecoFriendlyMessage.visibility = View.GONE
    }



}
