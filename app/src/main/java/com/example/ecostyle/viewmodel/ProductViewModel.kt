package com.example.ecostyle.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.Product

class ProductViewModel : ViewModel() {

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    fun loadProducts() {
        val sampleProducts = listOf(
            Product("Uniandes Jacket", "$120 000", "Uniandes jacket size XL. I changed to the Nacho, I no longer use the jacket", "https://via.placeholder.com/150")
        )
        _products.value = sampleProducts
    }
}
