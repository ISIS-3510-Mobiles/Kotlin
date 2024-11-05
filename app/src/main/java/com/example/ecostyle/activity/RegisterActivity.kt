package com.example.ecostyle.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        val registerButton = findViewById<Button>(R.id.loginButtonRegister)
        val emailEditText = findViewById<EditText>(R.id.emailEditTextRegister)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditTextRegister)
        val nameEditText = findViewById<EditText>(R.id.nameEditTextRegister)
        val addressEditText = findViewById<EditText>(R.id.addressEditTextRegister)
        val numberEditText = findViewById<EditText>(R.id.numberEditTextRegister)

        // Error TextViews
        val emailErrorTextView = findViewById<TextView>(R.id.emailErrorTextViewRegister)
        val nameErrorTextView = findViewById<TextView>(R.id.nameErrorTextViewRegister)
        val passwordErrorTextView = findViewById<TextView>(R.id.passwordErrorTextViewRegister)
        val addressErrorTextView = findViewById<TextView>(R.id.addressErrorTextViewRegister)
        val numberErrorTextView = findViewById<TextView>(R.id.numberErrorTextViewRegister)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()
            val number = numberEditText.text.toString().trim()

            val emailValid = validateEmail(email, emailEditText, emailErrorTextView)
            val nameValid = validateName(name, nameEditText, nameErrorTextView)
            val passwordValid = validatePassword(password, passwordEditText, passwordErrorTextView)
            val addressValid = validateAddress(address, addressEditText, addressErrorTextView)
            val numberValid = validatePhoneNumber(number, numberEditText, numberErrorTextView)

            if (emailValid && nameValid && passwordValid && addressValid && numberValid) {
                if (isNetworkAvailable()) {
                    viewModel.registerUser(email, password, name, address, number)
                } else {
                    showAlert("No internet connection. Please check your network settings.")
                }
            } else {
                showAlert("Please correct the highlighted errors")
            }
        }
    }

    private fun validateEmail(email: String, editText: EditText, errorTextView: TextView): Boolean {
        return if (!email.contains("@") || !email.contains(".")) {
            editText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = "Invalid email. Please include '@' and a domain (e.g., '.com')"
            false
        } else {
            editText.setTextColor(ContextCompat.getColor(this, R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validateName(name: String, editText: EditText, errorTextView: TextView): Boolean {
        return if (name.length < 3) {
            editText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = "Name must be at least 3 characters long"
            false
        } else {
            editText.setTextColor(ContextCompat.getColor(this, R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validatePassword(password: String, editText: EditText, errorTextView: TextView): Boolean {
        return if (password.length < 7) {
            editText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = "Password must be at least 7 characters long"
            false
        } else {
            editText.setTextColor(ContextCompat.getColor(this, R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validateAddress(address: String, editText: EditText, errorTextView: TextView): Boolean {
        return if (address.isEmpty() || address.length < 5) {
            editText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = "Address must be at least 5 characters long"
            false
        } else {
            editText.setTextColor(ContextCompat.getColor(this, R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validatePhoneNumber(number: String, editText: EditText, errorTextView: TextView): Boolean {
        return if (number.isEmpty() || number.length < 10 || !number.all { it.isDigit() }) {
            editText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = "Please enter a valid 10-digit phone number"
            false
        } else {
            editText.setTextColor(ContextCompat.getColor(this, R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun observeViewModel() {
        viewModel.registrationSuccess.observe(this, Observer { success ->
            if (success) {
                val provider = ProviderType.BASIC
                val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
                prefs.putString("email", viewModel.email)
                prefs.putString("provider", provider.name)
                prefs.apply()
                showHome(viewModel.email, provider)
            } else {
                showAlert("An error occurred during registration")
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        })
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Accept", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
        finish()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
}

