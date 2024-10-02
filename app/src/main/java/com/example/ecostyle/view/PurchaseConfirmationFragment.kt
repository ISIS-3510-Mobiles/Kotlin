package com.example.ecostyle.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.ecostyle.R

class PurchaseConfirmationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_purchase_confirmation, container, false)

        // Botón para regresar a la pantalla principal
        val backToHomeButton: Button = view.findViewById(R.id.back_to_home_button)
        backToHomeButton.setOnClickListener {
            parentFragmentManager.popBackStack()  // Regresa a la pantalla anterior
        }

        return view
    }
}
