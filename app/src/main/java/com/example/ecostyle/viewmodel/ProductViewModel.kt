// ProductViewModel.kt
package com.example.ecostyle.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.Product
import com.example.ecostyle.Repository.ProductRepository

class ProductViewModel : ViewModel() {
    private val productList: MutableLiveData<List<Product>> = MutableLiveData()
    private val repository = ProductRepository()

    init {
        loadProducts()
    }

    fun getProductList(): LiveData<List<Product>> {
        return productList
    }

    private fun loadProducts() {
        repository.getProducts { products ->
            productList.value = products
        }
    }
}
