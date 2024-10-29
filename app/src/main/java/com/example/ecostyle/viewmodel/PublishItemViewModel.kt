package com.example.ecostyle.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.Repository.ProductRepository

class PublishItemViewModel : ViewModel() {

    private val repository = ProductRepository()

    private val _publishStatus = MutableLiveData<Boolean>()
    val publishStatus: LiveData<Boolean> get() = _publishStatus

    fun publishProduct(name: String, price: Int, description: String, ecoFriendly: Boolean, imageUri: Uri, quantity: Int) {
        repository.publishProductToFirestore(name, price, description, ecoFriendly, imageUri, quantity) { success ->
            _publishStatus.postValue(success)
        }
    }
}
