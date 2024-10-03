package com.example.ecostyle.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.Product

class ProductDetailViewModel : ViewModel() {

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> get() = _product

    fun loadProduct(productId: Int) {
        _product.value = Product(
            id = productId,
            name = "Saco uniandes",
            price = "$ 120 000",
            imageResource = com.example.ecostyle.R.drawable.buzouniandes,
            description = "Saco uniandes talla XL. Me cambié a la nacho, ya no uso el saco",
            isFavorite = false
        )
    }

    fun toggleFavorite() {
        _product.value?.let {
            it.isFavorite = !it.isFavorite
            _product.value = it
        }
    }
}
