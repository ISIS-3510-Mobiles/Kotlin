import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class UploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val storageReference = FirebaseStorage.getInstance().reference

    override suspend fun doWork(): Result {
        val email = inputData.getString("email") ?: return Result.failure()
        val fileName = "pending_profile_image.jpg"
        val file = File(applicationContext.getExternalFilesDir(null), fileName)

        if (!file.exists()) {
            return Result.success() // No hay imagen pendiente
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        // Convertir el Bitmap a un ByteArray
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Crear una referencia única para la imagen usando el correo del usuario
        val fileReference = storageReference.child("profile_images/${UUID.randomUUID()}_${email}.jpg")

        return try {
            // Subir la imagen a Firebase Storage y obtener la URL de descarga
            fileReference.putBytes(data).await()
            val downloadUrl = fileReference.downloadUrl.await()
            val imageUrl = downloadUrl.toString()

            // Actualizar el campo imgUrl en Firestore
            updateUserProfileImageUrl(email, imageUrl)

            // Eliminar la imagen local después de subirla
            file.delete()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun updateUserProfileImageUrl(email: String, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()

        // Actualizar el campo imgUrl del usuario en Firestore
        db.collection("User").document(email)
            .update("imgUrl", imageUrl)
            .await()
    }
}
