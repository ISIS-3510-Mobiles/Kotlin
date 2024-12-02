import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.ecostyle.model.Comment
import com.example.ecostyle.utils.LocalStorageManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CommentUploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingComments = LocalStorageManager.getPendingComments(applicationContext)
        if (pendingComments.isEmpty()) {
            return Result.success() // No hay comentarios pendientes
        }

        val db = FirebaseFirestore.getInstance()

        try {

            for ((productId, comments) in pendingComments) {
                val commentsRef = db.collection("Products").document(productId).collection("Comments")

                for (comment in comments) {
                    try {
                        val existingComments = commentsRef
                            .whereEqualTo("userId", comment.userId)
                            .whereEqualTo("timestamp", comment.timestamp)
                            .get()
                            .await()
                        if (existingComments.isEmpty) {
                            // Subir comentario si no existe en Firebase
                            commentsRef.add(comment).await()
                            LocalStorageManager.removePendingComment(applicationContext, productId, comment)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return Result.retry()
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    companion object {
        fun createConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Solo se ejecuta si hay conexión a Internet
                .build()
        }
    }
}
