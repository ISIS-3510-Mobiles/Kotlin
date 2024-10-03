// ProductDetailViewModel.kt
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
        repository.getProductById(productId) { product ->
            /*
            _product.value = product ?: Product(
                id = -1, // Default or error ID
                name = "Product Not Found",
                price = "$0.00",
                imageResource = "", // Default image URL or placeholder
                description = "This product could not be found.",
                isFavorite = false
            )
             */
            _product.value = Product(
                id = 1,
                name = "Sweater Uniandes",
                price = "$100.00",
                imageResource = "test_image_url",
                description = "SweaterXL",
                isFavorite = false
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
