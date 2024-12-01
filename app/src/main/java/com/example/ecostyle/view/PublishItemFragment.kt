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
import androidx.core.content.FileProvider
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


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

// Agregar TextWatchers con límite de 7 dígitos
        priceEditText.addTextChangedListener(createThousandSeparatorTextWatcher(priceEditText, view.findViewById(R.id.price_error_text_view)))
        initialPriceEditText.addTextChangedListener(createThousandSeparatorTextWatcher(initialPriceEditText, view.findViewById(R.id.initialPrice_error_text_view)))

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
        takePhotoButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                openCameraForImage()
            }
        }


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
                    // Llamar a la función `getLocationAndPublishProduct` original
                    getLocationAndPublishProduct(
                        productName, productPrice, productDescription, ecoFriendly,
                        productImageUri, quantity, productBrand, productInitialPrice
                    )
                } else {
                    // Guardar datos localmente en caso de estar offline
                    saveDataLocally(
                        productName, productPrice, productDescription, ecoFriendly,
                        quantityText, productBrand, productInitialPrice
                    )
                    Toast.makeText(
                        requireContext(),
                        "Off-line. Data saved locally",
                        Toast.LENGTH_LONG
                    ).show()
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

                    // Limitar a 7 dígitos antes de procesar
                    val cleanText = s.toString().replace(".", "")
                    if (cleanText.length > 7) {
                        editText.setText(currentText) // Restaurar el texto anterior si excede 7 dígitos
                        editText.setSelection(currentText.length)
                    } else {
                        if (cleanText.isNotEmpty()) {
                            val formatted = formatToThousandSeparator(cleanText.toDouble())
                            currentText = formatted

                            editText.setText(formatted)
                            editText.setSelection(formatted.length)

                            validatePrice(editText, errorTextView)
                        }
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
        try {
            val imageFile = createImageFile()
            productImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, productImageUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivityForResult(intent, CAMERA_CAPTURE_CODE)
            } else {
                Toast.makeText(requireContext(), "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }




    // Guardar el Bitmap en un archivo y devolver la Uri del archivo creado
    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        try {
            deletePreviousImage() // Elimina la imagen anterior
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "saved_image.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCameraForImage()
                } else {
                    Toast.makeText(requireContext(), "Permission denied. Cannot access the camera.", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pendingPublishParams?.let {
                        getLocationAndPublishProduct(
                            it.name, it.price, it.description, it.ecoFriendly,
                            it.imageUri, it.quantity, it.brand, it.initialPrice
                        )
                        pendingPublishParams = null
                    }
                } else {
                    Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show()
                    pendingPublishParams = null
                }
            }
        }
    }

    // Manejar el resultado de la selección de la imagen o la foto tomada
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageErrorTextView = view?.findViewById<TextView>(R.id.image_error_text_view)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    productImageUri = data?.data!!
                    saveAndUpdateImage(productImageUri, imageErrorTextView)
                }
                CAMERA_CAPTURE_CODE -> {
                    if (::productImageUri.isInitialized) {
                        saveAndUpdateImage(productImageUri, imageErrorTextView)
                    } else {
                        Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Image selection/capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveImageUriToPreferences(uri: Uri) {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("imageUri", uri.toString()).apply()
    }

    private fun saveImageToLocalStorage(uri: Uri): Uri? {
        try {
            // Abre el stream de entrada para leer la imagen
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            deletePreviousImage() // Elimina la imagen anterior
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "saved_image.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream) // Copia la imagen al archivo local
            inputStream?.close()
            outputStream.close()

            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }
    private var pendingPublishParams: PendingPublishParams? = null

    data class PendingPublishParams(
        val name: String,
        val price: String,
        val description: String,
        val ecoFriendly: Boolean,
        val imageUri: Uri,
        val quantity: Int,
        val brand: String,
        val initialPrice: String
    )

    private fun getLocationAndPublishProduct(
        name: String, price: String, description: String, ecoFriendly: Boolean,
        imageUri: Uri, quantity: Int, brand: String, initialPrice: String
    ) {
        if (!File(imageUri.path ?: "").exists()) {
            Toast.makeText(requireContext(), "Image file not found. Please re-upload the image.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isGPSEnabled()) {
            Toast.makeText(requireContext(), "GPS is disabled. Please enable it and try again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPublishParams = PendingPublishParams(
                name, price, description, ecoFriendly, imageUri, quantity, brand, initialPrice
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        requestCurrentLocation { location ->
            if (location != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // Llamar al ViewModel para publicar el producto
                        val success = viewModel.publishProduct(
                            name, price, description, ecoFriendly, imageUri, quantity,
                            location.latitude, location.longitude, brand, initialPrice
                        )

                        if (success) {
                            Toast.makeText(requireContext(), "Product published successfully!", Toast.LENGTH_SHORT).show()
                            navigateToConfirmation()
                            clearFormData()
                        } else {
                            Toast.makeText(requireContext(), "Failed to publish product.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Unable to retrieve current location. Ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCurrentLocation(callback: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 5000 // 5 segundos
                fastestInterval = 2000 // 2 segundos
                numUpdates = 1 // Solo una actualización
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        callback(locationResult.lastLocation)
                    }
                },
                null
            )
        } else {
            callback(null)
        }
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
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

    private fun validateImage(imageErrorTextView: TextView): Boolean {
        return if (this::productImageUri.isInitialized) {
            imageErrorTextView.visibility = View.GONE
            true
        } else {
            imageErrorTextView.visibility = View.VISIBLE
            false
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

        // Save the image URI if it's initialized
        if (this::productImageUri.isInitialized) {
            editor.putString("imageUri", productImageUri.toString())
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

        // Guardar la URI de la imagen automáticamente si está inicializada
        if (this::productImageUri.isInitialized) {
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "auto_saved_image.jpg")
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, productImageUri)
                if (bitmap != null) {
                    val fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.flush()
                    fos.close()
                    sharedPreferences.edit().putString("imageUri", Uri.fromFile(file).toString()).apply()
                } else {
                    Toast.makeText(requireContext(), "Error: Bitmap is null, cannot save image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Cargar los datos guardados en el formulario desde SharedPreferences
    private fun loadFormData() {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        nameEditText.setText(sharedPreferences.getString("name", ""))
        priceEditText.setText(sharedPreferences.getString("price", ""))
        descriptionEditText.setText(sharedPreferences.getString("description", ""))
        quantityEditText.setText(sharedPreferences.getString("quantity", ""))
        brandEditText.setText(sharedPreferences.getString("brand", ""))
        initialPriceEditText.setText(sharedPreferences.getString("initialPrice", ""))
        ecoFriendlyCheckbox.isChecked = sharedPreferences.getBoolean("ecoFriendly", false)

        val imageUriString = sharedPreferences.getString("imageUri", null)
        if (!imageUriString.isNullOrEmpty()) {
            val file = File(Uri.parse(imageUriString).path ?: "")
            if (file.exists()) {
                productImageUri = Uri.parse(imageUriString)
                imageView.setImageURI(productImageUri)
            } else {
                imageView.setImageResource(0) // Clear the ImageView if there is no image
                sharedPreferences.edit().remove("imageUri").apply()
                Toast.makeText(requireContext(), "Previous image file not found. Please re-upload the image.", Toast.LENGTH_SHORT).show()
            }
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
        imageView.setImageResource(0) // Clear the image

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
        const val CAMERA_PERMISSION_REQUEST_CODE = 1003
        const val LOCATION_PERMISSION_REQUEST_CODE = 1004
    }

    private fun deletePreviousImage() {
        val sharedPreferences = requireContext().getSharedPreferences("PublishData", Context.MODE_PRIVATE)
        val previousImageUriString = sharedPreferences.getString("imageUri", null)
        if (!previousImageUriString.isNullOrEmpty()) {
            val previousFile = File(Uri.parse(previousImageUriString).path ?: "")
            if (previousFile.exists()) {
                previousFile.delete()
            }
        }
    }

    private fun saveAndUpdateImage(uri: Uri, imageErrorTextView: TextView?) {
        val savedUri = saveImageToLocalStorage(uri)
        if (savedUri != null) {
            productImageUri = savedUri

            // Añadir un identificador único a la URI para evitar el caché
            val uniqueUri = Uri.parse("${productImageUri}?timestamp=${System.currentTimeMillis()}")

            imageView.setImageURI(null) // Limpia el caché del ImageView
            imageView.setImageURI(uniqueUri) // Configura la URI única

            imageErrorTextView?.visibility = View.GONE
            saveImageUriToPreferences(productImageUri)
        } else {
            Toast.makeText(requireContext(), "Failed to save image locally", Toast.LENGTH_SHORT).show()
        }
    }
    private fun createImageFile(): File {
        val timestamp = System.currentTimeMillis().toString()
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs() // Asegúrate de crear el directorio si no existe
        }
        return File.createTempFile("IMG_$timestamp", ".jpg", storageDir)
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return false
        } else {
            return true
        }
    }

}

