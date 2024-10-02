package com.example.ecostyle.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ecostyle.R


class Notification : Application() {

    override fun onCreate() {
        super.onCreate()

        // Registra el listener para el ciclo de vida de la aplicación
        registerActivityLifecycleCallbacks(AppLifecycleListener())
    }

    public fun checkCartAndSendNotification() {
        val sharedPreferences = getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
        val hasItemsInCart = sharedPreferences.getBoolean("hasItemsInCart", false)

        if (hasItemsInCart) {
            sendAbandonedCartNotification()
        }
    }

    private fun sendAbandonedCartNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "cart_channel"

        // Crear el Intent para abrir la HomeActivity y dirigir al CheckoutFragment
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("openFragment", "checkout") // Indicamos que debe abrir el CheckoutFragment
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Crear el PendingIntent con el Intent anterior
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear el canal de notificación para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorio de Carrito",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Crear la notificación
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_cart) // Icono de la notificación
            .setContentTitle("Carrito Abandonado")
            .setContentText("Tienes productos en tu carrito. ¡No olvides completar tu compra!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Asignamos el PendingIntent para cuando se haga click

        // Mostrar la notificación
        notificationManager.notify(1, notificationBuilder.build())
    }

}