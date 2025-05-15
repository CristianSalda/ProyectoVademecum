package com.vacum.proyectovademecum

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nombreMedicamento = intent.getStringExtra("medicamento_nombre") ?: "Medicamento"
        val cantidadDosis = intent.getIntExtra("cantidad_dosis", 1)

        crearCanalNotificacion(context)

        val notification = NotificationCompat.Builder(context, "canal_alarmas")
            .setSmallIcon(R.drawable.notifications_active_24dp_007ac1_fill0_wght400_grad0_opsz24)
            .setContentTitle("Recordatorio de medicación")
            .setContentText("Es hora de tomar $cantidadDosis de $nombreMedicamento")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.blanco))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(nombreMedicamento.hashCode(), notification)
    }

    private fun crearCanalNotificacion(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "canal_alarmas"
            val channelName = "Recordatorios de medicación"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Canal para notificaciones de recordatorio de medicamentos"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}