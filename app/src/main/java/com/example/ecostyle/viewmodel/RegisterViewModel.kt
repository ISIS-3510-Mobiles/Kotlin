package com.example.ecostyle.viewmodel

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecostyle.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterViewModel : ViewModel() {

    private val _registrationSuccess = MutableLiveData<Boolean>()
    val registrationSuccess: LiveData<Boolean> get() = _registrationSuccess

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    var email: String = ""

    fun registerUser(email: String, password: String, name: String, address: String, number: String) {
        if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && number.isNotEmpty()) {
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        this.email = email
                        val user = User(
                            id = email,
                            imgUrl = "",
                            name = name,
                            adress = address,
                            number = number
                        )
                        saveUserToFirestore(user)

                    } else {
                        _errorMessage.value = "Registration failed"
                        _registrationSuccess.value = false
                    }
                }
        } else {
            _errorMessage.value = "Please fill in all fields"
            _registrationSuccess.value = false
        }
    }

    private fun saveUserToFirestore(user: User) {
        val db = FirebaseFirestore.getInstance()
        db.collection("User")
            .document(user.id ?: "")
            .set(user)
            .addOnSuccessListener {
                _registrationSuccess.value = true
            }
            .addOnFailureListener {
                _errorMessage.value = "Error saving user to Firestore"
                _registrationSuccess.value = false
            }
    }

}
