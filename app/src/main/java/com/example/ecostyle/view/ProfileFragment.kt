package com.example.ecostyle.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ecostyle.Activity.AuthActivity
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el diseño del fragmento
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        // Cargar datos de sesión de SharedPreferences
        val prefs = requireActivity().getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        // Inicializar vistas dentro de onCreateView
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val providerTextView = view.findViewById<TextView>(R.id.providerTextView)
        val logOutButton = view.findViewById<Button>(R.id.logOutButton)

        // Configurar el fragmento con los datos recibidos
        setup(email ?: "", provider ?: "", emailTextView, providerTextView, logOutButton)

        return view
    }

    private fun setup(
        email: String,
        provider: String,
        emailTextView: TextView,
        providerTextView: TextView,
        logOutButton: Button
    ) {
        // Actualizar los TextView con la información del usuario
        emailTextView.text = email
        providerTextView.text = provider

        // Configurar el botón de cerrar sesión
        logOutButton.setOnClickListener {
            // Cerrar sesión de Firebase
            FirebaseAuth.getInstance().signOut()

            // Limpiar SharedPreferences
            val prefsEditor = requireActivity().getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefsEditor.clear()
            prefsEditor.apply()

            // Iniciar la actividad de autenticación
            val authIntent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(authIntent)

            // Finalizar la actividad actual
            requireActivity().finish()
        }
    }
}
