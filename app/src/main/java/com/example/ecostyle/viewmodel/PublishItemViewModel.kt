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

    private val _publishStatus = MutableLiveData<Boolean>()
    val publishStatus: LiveData<Boolean> get() = _publishStatus

    fun publishProduct(
        name: String,
        price: String,
        description: String,
        ecoFriendly: Boolean,
        imageUri: Uri,
        quantity: Int,
        latitude: Double,
        longitude: Double
    ) {
        // Lanzar una corrutina para realizar la operación en un hilo secundario
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Llamar al repositorio para publicar el producto
                val success = repository.publishProductToFirestore(name, price, description, ecoFriendly, imageUri, quantity, latitude, longitude)
                _publishStatus.postValue(success) // Actualizar el estado de la publicación en la UI
            } catch (e: Exception) {
                // Si hay algún error, publicar false
                _publishStatus.postValue(false)
            }
        }
    }
}


