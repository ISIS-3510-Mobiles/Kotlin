package com.example.ecostyle.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_list, container, false) // Asegúrate de tener este layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_products)

        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

        // Inicializa el adaptador con una lista vacía y configura el listener
        productAdapter = ProductAdapter(emptyList()) { product ->
            // Navegar al ProductDetailFragment en lugar de iniciar una actividad
            val productDetailFragment = ProductDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("PRODUCT_ID", product.id)
                }
            }

            // Reemplaza el fragmento actual por el ProductDetailFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, productDetailFragment)
                .addToBackStack(null) // Para poder volver atrás
                .commit()

            Log.d("ListFragment", "Navigating to product details with ID: ${product.id}")
        }

        recyclerView.adapter = productAdapter

        // Inicializa el ViewModel
        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)

        // Observa el LiveData del ViewModel
        productViewModel.getProductList().observe(viewLifecycleOwner) { products ->
            // Actualiza la lista de productos en el adaptador
            productAdapter.setProductList(products)
        }
    }
}
