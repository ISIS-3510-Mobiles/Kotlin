package checkout

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.ecostyle.R

class CheckoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Configurar el Spinner para métodos de pago
        val paymentMethodsSpinner: Spinner = findViewById(R.id.payment_methods_spinner)
        val paymentMethods = arrayOf("Nequi", "Tarjeta de Crédito", "PSE", "Efectivo")

        // Adaptador para Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodsSpinner.adapter = adapter
    }

}