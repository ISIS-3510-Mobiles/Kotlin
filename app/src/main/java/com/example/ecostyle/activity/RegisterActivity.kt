package com.example.ecostyle.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setup()
        observeViewModel()
    }

    private fun setup() {
        title = "Autenticación"
        val registerButton = findViewById<Button>(R.id.loginButtonRegister)
        val emailEditTextRegister = findViewById<EditText>(R.id.emailEditTextRegister)
        val passwordEditTextRegister = findViewById<EditText>(R.id.passwordEditTextRegister)
        val nameEditText = findViewById<EditText>(R.id.nameEditTextRegister)
        val addressEditText = findViewById<EditText>(R.id.addressEditTextRegister)
        val numberEditText = findViewById<EditText>(R.id.numberEditTextRegister)

        registerButton.setOnClickListener {
            viewModel.registerUser(
                emailEditTextRegister.text.toString(),
                passwordEditTextRegister.text.toString(),
                nameEditText.text.toString(),
                addressEditText.text.toString(),
                numberEditText.text.toString()
            )
        }
    }

    private fun observeViewModel() {
        viewModel.registrationSuccess.observe(this, Observer { success ->
            if (success) {
                showHome(viewModel.email)
            } else {
                showAlert()
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        })
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("An error occurred authenticating the user")
        builder.setPositiveButton("Accept", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String) {
        // Implementación de la navegación a HomeActivity
    }
}
