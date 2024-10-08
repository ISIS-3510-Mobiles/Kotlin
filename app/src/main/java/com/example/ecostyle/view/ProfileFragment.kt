package com.example.ecostyle.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.ecostyle.Activity.AuthActivity
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var btnCamara: Button
    private var storageReference = FirebaseStorage.getInstance().reference
    private lateinit var email: String

    // Código para manejar el resultado de la cámara
    private val takePicturePreviewLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            profileImage.setImageBitmap(bitmap)
            uploadImageToStorage(bitmap, email)
        } else {
            Toast.makeText(context, "No se tomó ninguna foto.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el diseño del fragmento
        val view = inflater.inflate(R.layout.activity_profile, container, false)
        profileImage = view.findViewById(R.id.profileImage)
        btnCamara = view.findViewById(R.id.btnCamara)

        // Cargar datos de sesión de SharedPreferences
        val prefs = requireActivity().getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        email = prefs.getString("email", null) ?: ""

        // Inicializar vistas
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val logOutButton = view.findViewById<Button>(R.id.logOutButton)

        btnCamara.setOnClickListener {
            // Lanza la cámara para tomar una foto
            dispatchTakePictureIntent()
        }

        if (email.isNotEmpty()) {
            // Recuperar el nombre del usuario desde Firestore
            loadUserProfile(email, nameTextView, emailTextView)
        }

        // Configurar el botón de cerrar sesión
        logOutButton.setOnClickListener {
            // Cerrar sesión de Firebase
            FirebaseAuth.getInstance().signOut()

            // Limpiar SharedPreferences
            val prefsEditor = prefs.edit()
            prefsEditor.clear()
            prefsEditor.apply()

            // Iniciar la actividad de autenticación
            val authIntent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(authIntent)

            // Finalizar la actividad actual
            requireActivity().finish()
        }

        return view
    }

    // Iniciar la cámara para tomar una foto
    private fun dispatchTakePictureIntent() {
        takePicturePreviewLauncher.launch(null)
    }

    private fun loadUserProfile(email: String, nameTextView: TextView, emailTextView: TextView) {
        val db = FirebaseFirestore.getInstance()

        // Recuperar el documento del usuario desde la colección "User"
        val docRef = db.collection("User").document(email)
        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val name = document.getString("name") ?: ""
                val imgUrl = document.getString("imgUrl") ?: ""

                if (imgUrl.isNotEmpty()) {
                    // Si el imgUrl no está vacío, cargar la imagen con Glide
                    Glide.with(this)
                        .load(imgUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .circleCrop()
                        .into(profileImage)
                }

                // Actualizar los TextViews con los datos obtenidos
                nameTextView.text = name
                emailTextView.text = email
            } else {
                // El documento no existe
                println("El documento no existe.")
            }
        }.addOnFailureListener { exception ->
            println("Error al obtener el documento: $exception")
        }
    }

    private fun uploadImageToStorage(imageBitmap: Bitmap, email: String) {
        // Convertir el Bitmap a un ByteArray
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Crear una referencia única para la imagen usando el correo del usuario
        val fileReference = storageReference.child("profile_images/${UUID.randomUUID()}_${email}.jpg")

        // Subir la imagen a Firebase Storage
        val uploadTask = fileReference.putBytes(data)
        uploadTask.addOnSuccessListener {
            // Obtener la URL de descarga de la imagen
            fileReference.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                // Actualizar el campo imgUrl en Firestore
                updateUserProfileImageUrl(email, imageUrl)
            }
        }.addOnFailureListener {
            // Manejar errores en la subida
            println("Error al subir la imagen: ${it.message}")
            Toast.makeText(context, "Error al subir la imagen.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfileImageUrl(email: String, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()

        // Actualizar el campo imgUrl del usuario en Firestore
        db.collection("User").document(email)
            .update("imgUrl", imageUrl)
            .addOnSuccessListener {
                println("Imagen de perfil actualizada correctamente en Firestore")
            }
            .addOnFailureListener {
                println("Error al actualizar la imagen en Firestore: ${it.message}")
            }
    }
}
