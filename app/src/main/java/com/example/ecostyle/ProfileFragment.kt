package com.example.ecostyle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el diseño del fragmento
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        // Obtener los argumentos pasados desde el Bundle
        val email = arguments?.getString("email")
        val provider = arguments?.getString("provider")

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

            // Volver a la pantalla anterior (equivalente a onBackPressed() en fragmento)
            activity?.onBackPressed()
        }
    }
}
