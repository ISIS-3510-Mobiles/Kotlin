package com.example.ecostyle.view

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.ecostyle.activity.AuthActivity
import com.example.ecostyle.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var profileImage: ImageView
    private lateinit var btnCamara: Button
    private var storageReference = FirebaseStorage.getInstance().reference
    private lateinit var email: String
    private var pendingImageFile: File? = null
    private lateinit var connectivityReceiver: ConnectivityReceiver

    // Código para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, abrir la cámara
            dispatchTakePictureIntent()
        } else {
            // Permiso denegado, mostrar un mensaje o manejar el caso
            Toast.makeText(context, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Código para manejar el resultado de la cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageBitmap = result.data!!.extras?.get("data") as Bitmap
            profileImage.setImageBitmap(imageBitmap)
            uploadImageWithConnectivityCheck(imageBitmap, email)
        } else {
            Toast.makeText(context, "No se tomó ninguna foto.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Infla el diseño del fragmento
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        profileImage = view.findViewById(R.id.profileImage)
        btnCamara = view.findViewById(R.id.btnCamara)

        // Inicializar Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        // Cargar datos de sesión de SharedPreferences
        val prefs = requireActivity().getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        email = prefs.getString("email", null) ?: ""

        // Inicializar vistas
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val logOutButton = view.findViewById<Button>(R.id.logOutButton)

        btnCamara.setOnClickListener {
            // Verificar y solicitar el permiso de cámara
            checkCameraPermissionAndOpenCamera()

            // Registrar evento en Firebase Analytics
            val calendar = Calendar.getInstance()
            val photoTime = calendar.get(Calendar.DAY_OF_YEAR)
            val dateCamera = Bundle().apply {
                putInt("photo_time", photoTime)
            }
            firebaseAnalytics.logEvent("photo_time", dateCamera)
            Log.d("FirebaseAnalytics", "Logging session_date event: day of year=$dateCamera")
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

        // Inicializar el receptor de conectividad
        connectivityReceiver = ConnectivityReceiver()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Registrar el receptor para escuchar cambios en la conectividad
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(connectivityReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el receptor
        requireActivity().unregisterReceiver(connectivityReceiver)
    }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido, abrir la cámara
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Mostrar una explicación al usuario
                Toast.makeText(context, "La aplicación necesita acceso a la cámara para tomar fotos.", Toast.LENGTH_LONG).show()
                // Solicitar el permiso
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Solicitar el permiso
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Iniciar la cámara para tomar una foto
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        } else {
            Toast.makeText(context, "No hay ninguna aplicación de cámara disponible.", Toast.LENGTH_SHORT).show()
        }
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

    // Nueva función para manejar la carga de la imagen con verificación de conectividad
    private fun uploadImageWithConnectivityCheck(imageBitmap: Bitmap, email: String) {
        if (hasInternetConnection(requireContext())) {
            // Hay conexión, subir la imagen
            uploadImageToStorage(imageBitmap, email)
        } else {
            // No hay conexión, guardar la imagen localmente
            saveImageLocally(imageBitmap)
            Toast.makeText(context, "No hay conexión a Internet. La imagen se subirá cuando se restablezca la conexión.", Toast.LENGTH_LONG).show()
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
                // Guardar la imagen en la galería
                saveImageToGallery(requireContext(), imageBitmap)
                // Eliminar la imagen local si existe
                deleteLocalImage()
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

    private fun saveImageToGallery(context: Context, imageBitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EcoStyle")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            Toast.makeText(context, "¡Imagen guardada en la galería!", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Error al guardar la imagen en la galería", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val network = connectivityManager?.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    // Guardar la imagen localmente para subirla después
    private fun saveImageLocally(imageBitmap: Bitmap) {
        try {
            val fileName = "pending_profile_image.jpg"
            val file = File(requireContext().filesDir, fileName)
            val fos = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            pendingImageFile = file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al guardar la imagen localmente.", Toast.LENGTH_SHORT).show()
        }
    }

    // Intentar subir la imagen pendiente si existe
    private fun uploadPendingImage() {
        if (pendingImageFile != null && pendingImageFile!!.exists()) {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, android.net.Uri.fromFile(pendingImageFile))
            uploadImageToStorage(bitmap, email)
        }
    }

    // Eliminar la imagen local después de subirla
    private fun deleteLocalImage() {
        pendingImageFile?.let {
            if (it.exists()) {
                it.delete()
                pendingImageFile = null
            }
        }
    }

    // Receptor para detectar cambios en la conectividad
    inner class ConnectivityReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let {
                if (hasInternetConnection(it)) {
                    // Si hay conexión y hay una imagen pendiente, intentar subirla
                    uploadPendingImage()
                }
            }
        }
    }
}
