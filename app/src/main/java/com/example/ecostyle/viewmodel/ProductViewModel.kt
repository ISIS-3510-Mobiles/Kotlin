package com.example.ecostyle.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.Product
import com.example.ecostyle.R

class ProductViewModel : ViewModel() {
    private val productList: MutableLiveData<List<Product>> = MutableLiveData()

    init {
        loadProducts()
    }

    fun getProductList(): LiveData<List<Product>> {
        return productList
    }

    private fun loadProducts() {
        val products = listOf(
            Product("Uniandes Sweater", "$100.00", R.drawable.buzouniandes),
            Product("Uniandes Sweater", "$100.00", R.drawable.buzouniandes),
            Product("Uniandes Sweater", "$100.00", R.drawable.buzouniandes),
            Product("Uniandes Sweater", "$100.00", R.drawable.buzouniandes),
            Product("Uniandes Sweater", "$100.00", R.drawable.buzouniandes),
            Product("Uniandes Sweater", "$100.00", R.drawable.buzouniandes)
            // Add more products as needed
        )
        productList.value = products
    }
}
