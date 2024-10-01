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
            Product(1,"Uniandes Sweater", "$100.00", R.drawable.buzouniandes, ""),
            Product(2,"Uniandes Sweater", "$100.00", R.drawable.buzouniandes, ""),
            Product(3,"Uniandes Sweater", "$100.00", R.drawable.buzouniandes, ""),
            Product(4,"Uniandes Sweater", "$100.00", R.drawable.buzouniandes, ""),
            Product(5,"Uniandes Sweater", "$100.00", R.drawable.buzouniandes, ""),
            Product(6,"Uniandes Sweater", "$100.00", R.drawable.buzouniandes, "")
        )
        productList.value = products
    }
}
