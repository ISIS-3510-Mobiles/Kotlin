package com.example.ecostyle.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecostyle.Repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PublishItemViewModel : ViewModel() {

    private val repository = ProductRepository()

    private val _publishStatus = MutableLiveData<Boolean?>()
    val publishStatus: LiveData<Boolean?> get() = _publishStatus

    suspend fun publishProduct(
        name: String,
        price: String,
        description: String,
        ecoFriendly: Boolean,
        imageUri: Uri,
        quantity: Int,
        latitude: Double,
        longitude: Double,
        brand: String,
        initialPrice: String
    ): Boolean {
        return try {
            repository.publishProductToFirestore(
                name, price, description, ecoFriendly, imageUri, quantity,
                latitude, longitude, brand, initialPrice
            )
        } catch (e: Exception) {
            false
        }
    }

    fun resetPublishStatus() {
        _publishStatus.postValue(null)
    }
}


