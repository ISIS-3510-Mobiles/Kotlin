package com.example.ecostyle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.Repository.ProductRepository
import com.example.ecostyle.model.Product

class ProductDetailViewModel : ViewModel() {

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> get() = _product
    private val repository = ProductRepository()

    fun loadProduct(productId: Int) {
        Log.d("ProductDetailViewModel", "loadProduct called with productId: $productId")
        repository.getProductById(productId) { product ->
            _product.value = product ?: Product(
                id = -1,
                name = "Product Not Found",
                price = "$0.00",
                imageResource = "",
                description = "This product could not be found.",
                isFavorite = false,
                ecofriend= false
            )
            Log.d("ProductDetailViewModel", "Loaded product: ${_product.value}")
        }
    }

    fun toggleFavorite() {
        _product.value?.let {
            it.isFavorite = !(it.isFavorite ?: false)
            _product.value = it
        }
    }
}
