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
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.PublishItemViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private var imageSource = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var nameEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var ecoFriendlyCheckbox: CheckBox
    private lateinit var quantityEditText: EditText
    private lateinit var brandEditText: EditText
    private lateinit var initialPriceEditText: EditText
    private lateinit var publishButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_publish_item, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Inicializar las vistas
        nameEditText = view.findViewById(R.id.product_name_edittext)
        priceEditText = view.findViewById(R.id.product_price_edittext)
        brandEditText = view.findViewById(R.id.product_brand_edittext)
        initialPriceEditText = view.findViewById(R.id.product_initialPrice_edittext)
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
        val brandErrorTextView = view.findViewById<TextView>(R.id.brand_error_text_view)
        val initialPriceErrorTextView = view.findViewById<TextView>(R.id.initialPrice_error_text_view)
        val quantityErrorTextView = view.findViewById<TextView>(R.id.quantity_error_text_view)
        val imageErrorTextView = view.findViewById<TextView>(R.id.image_error_text_view)

        loadFormData()
        setupFieldValidation(
            nameEditText, nameErrorTextView,
            priceEditText, priceErrorTextView,
            descriptionEditText, descriptionErrorTextView,
            quantityEditText, quantityErrorTextView,
            brandEditText, brandErrorTextView,
            initialPriceEditText, initialPriceErrorTextView
        )
        setupAutoSaveFields()

        uploadImageButton.setOnClickListener { openGalleryForImage() }
        takePhotoButton.setOnClickListener { openCameraForImage() }

        publishButton.setOnClickListener {
            val productName = nameEditText.text.toString()
            val productPrice = priceEditText.text.toString()
            val productInitialPrice = initialPriceEditText.text.toString()
            val productBrand = brandEditText.text.toString()
            val productDescription = descriptionEditText.text.toString()
            val ecoFriendly = ecoFriendlyCheckbox.isChecked
            val quantityText = quantityEditText.text.toString()

            val nameValid = validateName(nameEditText, nameErrorTextView)
            val priceValid = validatePrice(priceEditText, priceErrorTextView)
            val initialPriceValid = validatePrice(initialPriceEditText, initialPriceErrorTextView)
            val descriptionValid = validateDescription(descriptionEditText, descriptionErrorTextView)
            val quantityValid = validateQuantity(quantityEditText, quantityErrorTextView)
            val brandValid = validateBrand(brandEditText, brandErrorTextView)

            // Validar si se ha seleccionado una imagen
            val imageValid = if (this::productImageUri.isInitialized) {
                imageErrorTextView.visibility = View.GONE
                true
            } else {
                imageErrorTextView.visibility = View.VISIBLE
                false
            }

            if (nameValid && priceValid && initialPriceValid && descriptionValid && quantityValid && brandValid && imageValid) {
                if (isNetworkAvailable(requireContext())) {
                    val quantity = quantityText.toInt()
                    getLocationAndPublishProduct(
                        productName, productPrice, productDescription, ecoFriendly, productImageUri, quantity, productBrand, productInitialPrice
                    )
                    navigateToConfirmation()
                    clearFormData()
                } else {
                    saveDataLocally(productName, productPrice, productDescription, ecoFriendly, quantityText, productBrand, productInitialPrice)
                    Toast.makeText(requireContext(), "\n" +
                            "Off-line. Data saved locally", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields and correct errors", Toast.LENGTH_SHORT).show()
            }
        }

        priceEditText.addTextChangedListener(createThousandSeparatorTextWatcher(priceEditText, priceErrorTextView))
        initialPriceEditText.addTextChangedListener(createThousandSeparatorTextWatcher(initialPriceEditText, initialPriceErrorTextView))

        return view
    }

    private fun createThousandSeparatorTextWatcher(editText: EditText, errorTextView: TextView): TextWatcher {
        return object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != currentText) {
                    editText.removeTextChangedListener(this)

                    val cleanText = s.toString().replace(".", "")
                    if (cleanText.isNotEmpty()) {
                        val formatted = formatToThousandSeparator(cleanText.toDouble())
                        currentText = formatted

                        editText.setText(formatted)
                        editText.setSelection(formatted.length)

                        validatePrice(editText, errorTextView)
                    }

                    editText.addTextChangedListener(this)
                }
            }
        }
    }

    // Función para formatear números con separador de miles "."
    private fun formatToThousandSeparator(value: Double): String {
        val formatter: NumberFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.GERMANY))
        return formatter.format(value)
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

        // Verificar el permiso de la cámara
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permiso otorgado, abrir la cámara
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_CAPTURE_CODE)
        } else {
            // Solicitar el permiso de la cámara
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado, abrir la cámara
                openCameraForImage()
            } else {
                // Permiso denegado, mostrar un mensaje al usuario
                Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
            }
        }
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
    // Obtener la ubicación del usuario y publicar el producto
    private fun getLocationAndPublishProduct(
        name: String, price: String, description: String, ecoFriendly: Boolean,
        imageUri: Uri, quantity: Int, brand: String, initialPrice: String
    ) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // Intentar obtener la última ubicación conocida
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // La ubicación es válida, procede con la publicación del producto
                    viewModel.publishProduct(
                        name, price, description, ecoFriendly, imageUri, quantity, location.latitude, location.longitude, brand, initialPrice
                    )
                } else {
                    // La ubicación es null, mostrar un mensaje al usuario
                    Toast.makeText(requireContext(), "Location is not available. Verify that GPS is activated and try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Maneja errores en la obtención de la ubicación
                Toast.makeText(requireContext(), "Error getting location: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Validar los campos de entrada
    private fun setupFieldValidation(
        nameEditText: EditText, nameErrorTextView: TextView,
        priceEditText: EditText, priceErrorTextView: TextView,
        descriptionEditText: EditText, descriptionErrorTextView: TextView,
        quantityEditText: EditText, quantityErrorTextView: TextView,
        brandEditText: EditText, brandErrorTextView: TextView,
        initialPriceEditText: EditText, initialPriceErrorTextView: TextView
    ) {
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateName(nameEditText, nameErrorTextView)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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

        brandEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateBrand(brandEditText, brandErrorTextView)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateName(nameEditText: EditText, errorTextView: TextView): Boolean {
        val productName = nameEditText.text.toString().trim()
        return if (productName.length < 3 || productName.isBlank()) {
            nameEditText.setTextColor(Color.RED)
            errorTextView.text = "Product name must have at least 3 characters and cannot be blank"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            nameEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validatePrice(priceEditText: EditText, errorTextView: TextView): Boolean {
        val priceText = priceEditText.text.toString().replace(".", "").trim()
        val productPrice = priceText.toDoubleOrNull() ?: 0.0
        return if (productPrice < 50 || productPrice > 1000000) {
            priceEditText.setTextColor(Color.RED)
            errorTextView.text = "Price must be between 50 and 1,000,000"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            priceEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validateBrand(brandEditText: EditText, errorTextView: TextView): Boolean {
        val brandName = brandEditText.text.toString().trim()
        val containsLetter = brandName.any { it.isLetter() }

        return if (brandName.length < 3 || !containsLetter || brandName.isBlank()) {
            brandEditText.setTextColor(Color.RED)
            errorTextView.text = "Brand must have at least 3 characters and contain at least one letter"
            errorTextView.visibility = View.VISIBLE
            false
        } else {
            brandEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            errorTextView.visibility = View.GONE
            true
        }
    }

    private fun validateDescription(descriptionEditText: EditText, errorTextView: TextView): Boolean {
        val productDescription = descriptionEditText.text.toString().trim()
        val containsLettersOrSpecialCharacters = productDescription.any { it.isLetterOrDigit() || !it.isWhitespace() }
        return if (productDescription.length < 10 || !containsLettersOrSpecialCharacters) {
            descriptionEditText.setTextColor(Color.RED)
            errorTextView.text = "Description must have at least 10 characters and contain letters, numbers, or special characters"
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

    private fun saveDataLocally(
        name: String, price: String, description: String, ecoFriendly: Boolean,
        quantity: String, brand: String, initialPrice: String
    ) {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("name", name)
        editor.putString("price", price)
        editor.putString("description", description)
        editor.putBoolean("ecoFriendly", ecoFriendly)
        editor.putString("quantity", quantity)
        editor.putString("brand", brand)
        editor.putString("initialPrice", initialPrice)

        // Guardar la imagen en el almacenamiento externo y almacenar la URI
        if (this::productImageUri.isInitialized) {
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "saved_image.jpg")
            val fos = FileOutputStream(file)
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, productImageUri)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            editor.putString("imageUri", Uri.fromFile(file).toString())
        }

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

        brandEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("brand", s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        initialPriceEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("initialPrice", s.toString()).apply()
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
        val brand = sharedPreferences.getString("brand", "")
        val initialPrice = sharedPreferences.getString("initialPrice", "")
        val imageUriString = sharedPreferences.getString("imageUri", null)

        nameEditText.setText(name)
        priceEditText.setText(price)
        descriptionEditText.setText(description)
        quantityEditText.setText(quantity)
        ecoFriendlyCheckbox.isChecked = ecoFriendly
        brandEditText.setText(brand)
        initialPriceEditText.setText(initialPrice)

        // Cargar la imagen si existe
        if (!imageUriString.isNullOrEmpty()) {
            productImageUri = Uri.parse(imageUriString)
            imageView.setImageURI(productImageUri)
        }
    }

    // Limpiar el formulario y los datos almacenados en SharedPreferences
    private fun clearFormData() {
        nameEditText.text.clear()
        priceEditText.text.clear()
        descriptionEditText.text.clear()
        quantityEditText.text.clear()
        ecoFriendlyCheckbox.isChecked = false
        brandEditText.text.clear()
        initialPriceEditText.text.clear()
        imageView.setImageResource(0) // Limpiar la imagen

        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    // Verificar si hay conexión a Internet
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
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
        const val CAMERA_PERMISSION_REQUEST_CODE = 1003 // Código de solicitud de permiso para la cámara
    }
}

