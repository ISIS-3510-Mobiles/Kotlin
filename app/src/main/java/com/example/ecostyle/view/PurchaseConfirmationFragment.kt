package com.example.ecostyle.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.ecostyle.R
import com.example.ecostyle.Activity.HomeActivity

class PurchaseConfirmationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_purchase_confirmation, container, false)

        // Botón para regresar a la pantalla principal
        val backToHomeButton: Button = view.findViewById(R.id.back_to_home_button)
        backToHomeButton.setOnClickListener {
            // Lanzar HomeActivity y limpiar la pila de actividades
            val intent = Intent(requireContext(), HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            requireActivity().finish() // Cerrar la actividad actual
        }

        return view
    }
}
