package com.example.ecostyle.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ecostyle.R

class PaymentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_payment, container, false) // Cambié activity_payment por fragment_payment_method

        // Configurar el Spinner de métodos de pago
        val paymentMethodsSpinner: Spinner = view.findViewById(R.id.payment_methods_spinner)
        val paymentMethods = arrayOf("Nequi", "Tarjeta de Crédito", "Tarjeta de Débito", "PSE", "Efectivo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter

        // Obtener los campos de facturación
        val billingAddress: EditText = view.findViewById(R.id.billing_address)
        val billingCity: EditText = view.findViewById(R.id.billing_city)
        val billingZipcode: EditText = view.findViewById(R.id.billing_zipcode)

        // Configurar el botón para proceder a la confirmación
        val proceedButton: Button = view.findViewById(R.id.proceed_to_confirmation_button)
        proceedButton.setOnClickListener {
            // Aquí puedes validar la información y proceder a la confirmación
            val selectedPaymentMethod = paymentMethodsSpinner.selectedItem.toString()
            val address = billingAddress.text.toString()
            val city = billingCity.text.toString()
            val zipcode = billingZipcode.text.toString()

            if (address.isNotEmpty() && city.isNotEmpty() && zipcode.isNotEmpty()) {
                // Redirigir a la pantalla de confirmación
                showPurchaseConfirmation()
            } else {
                // Mostrar mensaje de error si la información no es válida
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showPurchaseConfirmation() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PurchaseConfirmationFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }
}

