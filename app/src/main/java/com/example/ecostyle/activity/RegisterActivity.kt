package com.example.ecostyle.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
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

        val registerButton = findViewById<Button>(R.id.loginButtonRegister)
        val emailEditTextRegister = findViewById<EditText>(R.id.emailEditTextRegister)
        val passwordEditTextRegister = findViewById<EditText>(R.id.passwordEditTextRegister)
        val nameEditText = findViewById<EditText>(R.id.nameEditTextRegister)
        val addressEditText = findViewById<EditText>(R.id.addressEditTextRegister)
        val numberEditText = findViewById<EditText>(R.id.numberEditTextRegister)

        registerButton.setOnClickListener {
            // Check internet connectivity
            if (isNetworkAvailable()) {
                viewModel.registerUser(
                    emailEditTextRegister.text.toString(),
                    passwordEditTextRegister.text.toString(),
                    nameEditText.text.toString(),
                    addressEditText.text.toString(),
                    numberEditText.text.toString()
                )
            } else {
                // Show message if no internet connection
                showAlert("No internet connection. Please check your network settings.")
            }
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
        Log.d("RegisterActivity", "Registration successful, redirecting to HomeActivity")
        finish()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For API level 23 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                // For devices able to connect with Ethernet
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                // For checking internet over Bluetooth
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            // For API level below 23
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
}
