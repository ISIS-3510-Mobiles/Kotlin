package com.example.ecostyle.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si hay una sesión activa
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email != null && provider != null) {
            // Si ya hay una sesión guardada, ir directamente a HomeActivity
            showHome(email, ProviderType.valueOf(provider))
        } else {
            setContentView(R.layout.activity_auth)
            setup()
        }
    }

    private fun setup() {
        title = "Authentication"
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        signUpButton.setOnClickListener {
            val homeIntent = Intent(this, RegisterActivity::class.java)
            startActivity(homeIntent)
        }

        loginButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        emailEditText.text.toString(),
                        passwordEditText.text.toString()
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val email = it.result?.user?.email ?: ""
                            val provider = ProviderType.BASIC

                            // Guardar las credenciales en SharedPreferences
                            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
                            prefs.putString("email", email)
                            prefs.putString("provider", provider.name)
                            prefs.apply()

                            // Obtener el token FCM después de la autenticación
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                                    return@addOnCompleteListener
                                }

                                // Obtener el token de registro FCM
                                val token = task.result
                                Log.d("FCM", "Token FCM: $token")

                                // Guardar el token FCM en Firestore
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("users").document(userId)
                                        .update("fcmToken", token)
                                        .addOnSuccessListener {
                                            Log.d("FCM", "Token FCM guardado correctamente en Firestore.")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("FCM", "Error al guardar el token FCM en Firestore", e)
                                        }
                                }
                            }

                            // Redirigir a HomeActivity
                            showHome(email, provider)
                        } else {
                            showAlert()
                        }
                    }
            }
        }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("An error occurred while authenticating the user")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
        finish() // Cierra la actividad de autenticación para que no regrese aquí
    }
}
