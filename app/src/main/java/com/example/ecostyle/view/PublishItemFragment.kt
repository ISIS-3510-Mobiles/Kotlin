package com.example.ecostyle.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.PublishItemViewModel

class PublishItemFragment : Fragment() {

    private val viewModel: PublishItemViewModel by viewModels()
    private lateinit var imageView: ImageView
    private lateinit var productImageUri: Uri
    private var imageSource = false // False if from gallery, true if from camera

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_publish_item, container, false)

        val nameEditText = view.findViewById<EditText>(R.id.product_name_edittext)
        val priceEditText = view.findViewById<EditText>(R.id.product_price_edittext)
        val descriptionEditText = view.findViewById<EditText>(R.id.product_description_edittext)
        val ecoFriendlyCheckbox = view.findViewById<CheckBox>(R.id.ecofriendly_checkbox)
        imageView = view.findViewById(R.id.product_image_view)
        val uploadImageButton = view.findViewById<Button>(R.id.upload_image_button)
        val takePhotoButton = view.findViewById<Button>(R.id.take_photo_button)
        val publishButton = view.findViewById<Button>(R.id.publish_button)
        val quantityEditText = view.findViewById<EditText>(R.id.product_quantity_edittext)

        // Mensajes de error debajo de cada campo
        val nameErrorTextView = view.findViewById<TextView>(R.id.name_error_text_view)
        val priceErrorTextView = view.findViewById<TextView>(R.id.price_error_text_view)
        val descriptionErrorTextView = view.findViewById<TextView>(R.id.description_error_text_view)
        val quantityErrorTextView = view.findViewById<TextView>(R.id.quantity_error_text_view)

        // Añadir listeners para cambios de texto
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateName(nameEditText, nameErrorTextView)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        priceEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validatePrice(priceEditText, priceErrorTextView)
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

        // Permitir subir imagen desde la galería
        uploadImageButton.setOnClickListener {
            openGalleryForImage()
        }

        // Permitir tomar foto con la cámara
        takePhotoButton.setOnClickListener {
            openCameraForImage()
        }

        // Validar y publicar producto
        publishButton.setOnClickListener {
            val productName = nameEditText.text.toString()
            val productPriceText = priceEditText.text.toString()
            val productDescription = descriptionEditText.text.toString()
            val ecoFriendly = ecoFriendlyCheckbox.isChecked
            val quantityText = quantityEditText.text.toString()

            val nameValid = validateName(nameEditText, nameErrorTextView)
            val priceValid = validatePrice(priceEditText, priceErrorTextView)
            val descriptionValid = validateDescription(descriptionEditText, descriptionErrorTextView)
            val quantityValid = validateQuantity(quantityEditText, quantityErrorTextView)

            // Verificar que todos los campos estén completos y sean válidos
            if (nameValid && priceValid && descriptionValid && quantityValid && this::productImageUri.isInitialized) {
                val quantity = quantityText.toInt()
                val productPrice = productPriceText.toInt()

                // Llamar al ViewModel para publicar el artículo
                viewModel.publishProduct(
                    productName,
                    productPrice,
                    productDescription,
                    ecoFriendly,
                    productImageUri,
                    quantity
                )

            } else {
                Toast.makeText(requireContext(), "Please fill all fields and correct errors", Toast.LENGTH_SHORT).show()
            }
        }

        // Observa el estado de la publicación
        viewModel.publishStatus.observe(viewLifecycleOwner) { status ->
            if (status) {
                Toast.makeText(requireContext(), "Product published successfully", Toast.LENGTH_SHORT).show()
                navigateToConfirmation() // Navegar a la pantalla de confirmación
            } else {
                Toast.makeText(requireContext(), "Failed to publish product", Toast.LENGTH_SHORT).show()
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

    // Manejar el resultado de la selección de la imagen o la foto tomada
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE) {
                // Imagen seleccionada de la galería
                productImageUri = data?.data!!
                imageView.setImageURI(productImageUri)
            } else if (requestCode == CAMERA_CAPTURE_CODE) {
                // Imagen capturada con la cámara
                val bitmap = data?.extras?.get("data") as? Uri
                productImageUri = bitmap ?: Uri.EMPTY
                imageView.setImageURI(productImageUri)
            }
        }
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
        val productPrice = priceEditText.text.toString().toDoubleOrNull() ?: 0.0
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

    // Función para navegar a la pantalla de confirmación
    private fun navigateToConfirmation() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, PublishConfirmationFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    companion object {
        const val IMAGE_PICK_CODE = 1001
        const val CAMERA_CAPTURE_CODE = 1002
    }
}


