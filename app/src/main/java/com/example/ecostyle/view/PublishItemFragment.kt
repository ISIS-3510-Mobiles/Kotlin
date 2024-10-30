package com.example.ecostyle.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.PublishItemViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class PublishItemFragment : Fragment() {

    private val viewModel: PublishItemViewModel by viewModels()
    private lateinit var imageView: ImageView
    private lateinit var productImageUri: Uri
    private var imageSource = false // False si es desde galería, true si es desde la cámara
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var nameEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var ecoFriendlyCheckbox: CheckBox
    private lateinit var quantityEditText: EditText
    private lateinit var publishButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_publish_item, container, false)

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Inicializar las vistas
        nameEditText = view.findViewById(R.id.product_name_edittext)
        priceEditText = view.findViewById(R.id.product_price_edittext)
        descriptionEditText = view.findViewById(R.id.product_description_edittext)
        ecoFriendlyCheckbox = view.findViewById(R.id.ecofriendly_checkbox)
        imageView = view.findViewById(R.id.product_image_view)
        val uploadImageButton = view.findViewById<Button>(R.id.upload_image_button)
        val takePhotoButton = view.findViewById<Button>(R.id.take_photo_button)
        publishButton = view.findViewById(R.id.publish_button)
        quantityEditText = view.findViewById(R.id.product_quantity_edittext)

        // Mensajes de error debajo de cada campo
        val nameErrorTextView = view.findViewById<TextView>(R.id.name_error_text_view)
        val priceErrorTextView = view.findViewById<TextView>(R.id.price_error_text_view)
        val descriptionErrorTextView = view.findViewById<TextView>(R.id.description_error_text_view)
        val quantityErrorTextView = view.findViewById<TextView>(R.id.quantity_error_text_view)

        // Cargar datos guardados en SharedPreferences al iniciar el fragmento
        loadFormData()

        // Validación de campos y otros componentes
        setupFieldValidation(nameEditText, nameErrorTextView, priceEditText, priceErrorTextView, descriptionEditText, descriptionErrorTextView, quantityEditText, quantityErrorTextView)

        // Detectar cambios en los campos para guardarlos
        setupAutoSaveFields()

        uploadImageButton.setOnClickListener {
            openGalleryForImage()
        }

        takePhotoButton.setOnClickListener {
            openCameraForImage()
        }

        publishButton.setOnClickListener {
            val productName = nameEditText.text.toString()
            val productPrice = priceEditText.text.toString()
            val productDescription = descriptionEditText.text.toString()
            val ecoFriendly = ecoFriendlyCheckbox.isChecked
            val quantityText = quantityEditText.text.toString()

            val nameValid = validateName(nameEditText, nameErrorTextView)
            val priceValid = validatePrice(priceEditText, priceErrorTextView)
            val descriptionValid = validateDescription(descriptionEditText, descriptionErrorTextView)
            val quantityValid = validateQuantity(quantityEditText, quantityErrorTextView)

            if (nameValid && priceValid && descriptionValid && quantityValid && this::productImageUri.isInitialized) {
                if (isNetworkAvailable(requireContext())) {
                    // Obtener la ubicación del usuario y publicar el producto
                    val quantity = quantityText.toInt()
                    getLocationAndPublishProduct(
                        productName, productPrice, productDescription, ecoFriendly, productImageUri, quantity
                    )

                    // Redirigir a la página de confirmación después de la publicación
                    navigateToConfirmation()
                    clearFormData()
                } else {
                    // Guardar localmente si no hay conexión
                    saveDataLocally(productName, productPrice, productDescription, ecoFriendly, quantityText)
                    Toast.makeText(requireContext(), "Sin conexión. Datos guardados localmente", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields and correct errors", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Función para abrir la galería para seleccionar la imagen del producto
    private fun openGalleryForImage() {
        imageSource = false // Imagen desde la galería
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // Función para abrir la cámara para tomar la imagen del producto
    private fun openCameraForImage() {
        imageSource = true // Imagen desde la cámara
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_CAPTURE_CODE)
    }

    // Guardar el Bitmap en un archivo y devolver la Uri del archivo creado
    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_image.jpg")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.flush()
        fos.close()
        return Uri.fromFile(file)
    }

    // Manejar el resultado de la selección de la imagen o la foto tomada
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE) {
                // Imagen seleccionada de la galería
                productImageUri = data?.data!!
                imageView.setImageURI(productImageUri)  // Mostrar la imagen seleccionada
            } else if (requestCode == CAMERA_CAPTURE_CODE) {
                // Imagen capturada con la cámara
                val bitmap = data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)

                    // Guardar el Bitmap como un archivo y obtener su Uri
                    val tempImageUri = saveBitmapToFile(bitmap)

                    // Actualizar productImageUri para que apunte al archivo temporal
                    if (tempImageUri != null) {
                        productImageUri = tempImageUri
                        imageView.setImageURI(productImageUri)  // Mostrar la imagen capturada
                    }
                }
            }
        }
    }

    // Obtener la ubicación del usuario y publicar el producto
    private fun getLocationAndPublishProduct(
        name: String, price: String, description: String, ecoFriendly: Boolean,
        imageUri: Uri, quantity: Int
    ) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.publishProduct(
                        name, price, description, ecoFriendly, imageUri, quantity, location.latitude, location.longitude
                    )
                } else {
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Validar los campos de entrada
    private fun setupFieldValidation(
        nameEditText: EditText, nameErrorTextView: TextView,
        priceEditText: EditText, priceErrorTextView: TextView,
        descriptionEditText: EditText, descriptionErrorTextView: TextView,
        quantityEditText: EditText, quantityErrorTextView: TextView
    ) {
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateName(nameEditText, nameErrorTextView)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        priceEditText.addTextChangedListener(object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != currentText) {
                    priceEditText.removeTextChangedListener(this)

                    val cleanText = s.toString().replace(".", "")
                    if (cleanText.isNotEmpty()) {
                        val formatted = formatToThousandSeparator(cleanText.toDouble())
                        currentText = formatted

                        priceEditText.setText(formatted)
                        priceEditText.setSelection(formatted.length)

                        validatePrice(priceEditText, priceErrorTextView)
                    }

                    priceEditText.addTextChangedListener(this)
                }
            }
        })

        descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateDescription(descriptionEditText, descriptionErrorTextView)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        quantityEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateQuantity(quantityEditText, quantityErrorTextView)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Funciones de validación para cada campo
    private fun validateName(nameEditText: EditText, errorTextView: TextView): Boolean {
        val productName = nameEditText.text.toString()
        return if (productName.length < 3) {
            nameEditText.setTextColor(Color.RED)
            errorTextView.text = "Product name must have at least 3 characters"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            nameEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validatePrice(priceEditText: EditText, errorTextView: TextView): Boolean {
        val priceText = priceEditText.text.toString().replace(".", "")
        val productPrice = priceText.toDoubleOrNull() ?: 0.0
        return if (productPrice < 50) {
            priceEditText.setTextColor(Color.RED)
            errorTextView.text = "Price must be greater than 50"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            priceEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validateDescription(descriptionEditText: EditText, errorTextView: TextView): Boolean {
        val productDescription = descriptionEditText.text.toString()
        val containsLetters = productDescription.any { it.isLetter() }
        return if (productDescription.length < 10 || !containsLetters) {
            descriptionEditText.setTextColor(Color.RED)
            errorTextView.text = "Description must have at least 10 characters and contain letters"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            descriptionEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validateQuantity(quantityEditText: EditText, errorTextView: TextView): Boolean {
        val quantity = quantityEditText.text.toString().toIntOrNull() ?: 0
        return if (quantity <= 0) {
            quantityEditText.setTextColor(Color.RED)
            errorTextView.text = "Please enter a valid quantity"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            quantityEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    // Guardar los datos localmente
    private fun saveDataLocally(name: String, price: String, description: String, ecoFriendly: Boolean, quantity: String) {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("name", name)
        editor.putString("price", price)
        editor.putString("description", description)
        editor.putBoolean("ecoFriendly", ecoFriendly)
        editor.putString("quantity", quantity)
        editor.apply()
    }

    // Guardar los datos del formulario automáticamente
    private fun setupAutoSaveFields() {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)

        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("name", s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        priceEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("price", s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("description", s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        quantityEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("quantity", s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ecoFriendlyCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("ecoFriendly", isChecked).apply()
        }
    }

    // Cargar los datos guardados en el formulario desde SharedPreferences
    private fun loadFormData() {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name", "")
        val price = sharedPreferences.getString("price", "")
        val description = sharedPreferences.getString("description", "")
        val quantity = sharedPreferences.getString("quantity", "")
        val ecoFriendly = sharedPreferences.getBoolean("ecoFriendly", false)

        nameEditText.setText(name)
        priceEditText.setText(price)
        descriptionEditText.setText(description)
        quantityEditText.setText(quantity)
        ecoFriendlyCheckbox.isChecked = ecoFriendly
    }

    // Limpiar el formulario y los datos almacenados en SharedPreferences
    private fun clearFormData() {
        nameEditText.text.clear()
        priceEditText.text.clear()
        descriptionEditText.text.clear()
        quantityEditText.text.clear()
        ecoFriendlyCheckbox.isChecked = false
        imageView.setImageResource(0) // Limpiar la imagen

        // Limpiar los datos de SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    // Verificar si hay conexión a Internet
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    // Función para formatear números con separador de miles "."
    private fun formatToThousandSeparator(value: Double): String {
        val formatter: NumberFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.GERMANY))
        return formatter.format(value)
    }

    // Redirigir a la página de confirmación
    private fun navigateToConfirmation() {
        val confirmationFragment = PublishConfirmationFragment() // Fragmento de confirmación
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, confirmationFragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val IMAGE_PICK_CODE = 1001
        const val CAMERA_CAPTURE_CODE = 1002
    }
}





