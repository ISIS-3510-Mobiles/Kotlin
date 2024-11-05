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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email != null && provider != null) {
            showHome(email, ProviderType.valueOf(provider))
        } else {
            setContentView(R.layout.activity_auth)
            setup()
        }

        val firebaseConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60)
            .build()
        firebaseConfig.setConfigSettingsAsync(configSettings)
    }

    private fun setup() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val emailErrorTextView = findViewById<TextView>(R.id.emailErrorTextView)
        val passwordErrorTextView = findViewById<TextView>(R.id.passwordErrorTextView)

        signUpButton.setOnClickListener {
            val homeIntent = Intent(this, RegisterActivity::class.java)
            startActivity(homeIntent)
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            val emailValid = validateEmail(email, emailEditText, emailErrorTextView)
            val passwordValid = validatePassword(password, passwordEditText, passwordErrorTextView)

            if (emailValid && passwordValid) {
                if (isNetworkAvailable()) {
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val email = it.result?.user?.email ?: ""
                                val provider = ProviderType.BASIC

                                val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
                                prefs.putString("email", email)
                                prefs.putString("provider", provider.name)
                                prefs.apply()

                                showHome(email, provider)
                            } else {
                                showAlert("An error occurred while authenticating the user")
                            }
                        }
                } else {
                    showAlert("No internet connection. Please check your network settings.")
                }
            } else {
                showAlert("Please correct the highlighted errors")
            }
        }
    }

    private fun validateEmail(email: String, editText: EditText, errorTextView: TextView): Boolean {
        return if (!email.contains("@")) {
            editText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            errorTextView.visibility = View.VISIBLE
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
            false
        } else {
            editText.setTextColor(ContextCompat.getColor(this, R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
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

