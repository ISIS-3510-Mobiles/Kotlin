package com.example.ecostyle.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ecostyle.R
import com.example.ecostyle.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        setup()
    }

    private fun setup() {
        title = "Autenticación"
        val registerButton = findViewById<Button>(R.id.loginButtonRegister)
        val emailEditTextRegister = findViewById<EditText>(R.id.emailEditTextRegister)
        val passwordEditTextRegister = findViewById<EditText>(R.id.passwordEditTextRegister)
        val nameEditText = findViewById<EditText>(R.id.nameEditTextRegister) // New field for name
        val addressEditText = findViewById<EditText>(R.id.addressEditTextRegister) // New field for address
        val numberEditText = findViewById<EditText>(R.id.numberEditTextRegister) // New field for phone number

        registerButton.setOnClickListener {
            if (emailEditTextRegister.text.isNotEmpty()
                && passwordEditTextRegister.text.isNotEmpty()
                && nameEditText.text.isNotEmpty()
                && addressEditText.text.isNotEmpty()
                && numberEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(
                        emailEditTextRegister.text.toString(),
                        passwordEditTextRegister.text.toString()
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            // Add the user to Firestore
                            val email = it.result?.user?.email ?: ""
                            val user = User(
                                id = emailEditTextRegister.text.toString(),
                                imgUrl = "", // Empty by default
                                name = nameEditText.text.toString(),
                                adress = addressEditText.text.toString(),
                                number = numberEditText.text.toString()
                            )
                            saveUserToFirestore(user)

                            showHome(email, ProviderType.BASIC)
                        } else {
                            showAlert()
                        }
                    }
            }
            else {
                // Mostrar mensaje de error si la información no es válida
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserToFirestore(user: User) {
        val db = FirebaseFirestore.getInstance()
        db.collection("User")
            .document(user.id ?: "")
            .set(user)
            .addOnSuccessListener {
                // User saved successfully
                println("User saved successfully on Firestore")
            }
            .addOnFailureListener {e ->
                // Handle the error
                println("Error saving user to Firestore: ${e.message}")
            }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("An error occurred authenticating the user")
        builder.setPositiveButton("Accept", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider)
        }
        startActivity(homeIntent)
    }
}
