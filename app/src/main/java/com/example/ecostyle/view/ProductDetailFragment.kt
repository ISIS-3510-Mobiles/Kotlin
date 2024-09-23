package com.example.ecostyle.view
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.ecostyle.viewmodel.ProductViewModel
import com.example.ecostyle.ui.theme.EcoStyleTheme

class ProductDetailFragment : Fragment() {

    private val productViewModel: ProductViewModel by activityViewModels()
    //private val productViewModel: ProductViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Load the products when the fragment is created
        productViewModel.loadProducts()

        return ComposeView(requireContext()).apply {
            setContent {
                EcoStyleTheme {
                    // Observe the product and pass it
                    val product = productViewModel.products.value?.firstOrNull()
                    product?.let {
                        ProductDetailScreen(product = it)
                    } ?: run {
                        Text("No product available or loading")
                    }
                }
            }
        }
    }
}