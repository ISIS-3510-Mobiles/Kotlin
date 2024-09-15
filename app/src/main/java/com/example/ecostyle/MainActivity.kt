package com.example.ecostyle

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            val response = IdpResponse.fromResultIntent(it.data)

            if (it.resultCode == RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null){
                    Toast.makeText( this, "Bienvenido", Toast.LENGTH_SHORT).show()
                }
            }

        }.launch(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build())
    }
}