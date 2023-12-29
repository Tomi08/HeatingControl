@file:Suppress("DEPRECATION")

package com.heatingcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

class MyNotificationService : JobIntentService() {
    companion object {
        private const val JOB_ID = 123
        const val ACTION_SHOW_NOTIFICATION = "com.example.action.SHOW_NOTIFICATION"
        const val channelId = "my_channel_id"
        private const val PERMISSION_REQUEST_CODE = 123

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun showNotification(context: Context) {
            if (ActivityCompat.checkSelfPermission(
                    context as Activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(
                        Manifest.permission.VIBRATE,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED,
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                enqueueWork(
                    context,
                    MyNotificationService::class.java,
                    JOB_ID,
                    Intent(ACTION_SHOW_NOTIFICATION)
                )
            }
        }
    }

    override fun onHandleWork(intent: Intent) {

        when (intent.action) {
            ACTION_SHOW_NOTIFICATION -> {
                createNotificationChannel()
                createAndShowNotification()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAndShowNotification() {
        val largeIconBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.app_logo2)
        // Notification format
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo2)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle("Warning")
            .setContentText("Ventilate the room, the room humidity is too high")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Unique Id for notification
        val notificationId = Random.nextInt()

        // Showing the notification

        Handler(Looper.getMainLooper()).post {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(notificationId, notificationBuilder.build())
        }

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.createNotificationChannel(channel)
        }
    }
}