package com.aarvo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aarvo.network.AarvoApiClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RiderFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        ensureFirebaseConfigured()
        createChannel()
    }

    override fun onNewToken(token: String) {
        getSharedPreferences("aarvo_rider_fcm", Context.MODE_PRIVATE)
            .edit().putString("token", token).apply()
        registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "AARVO Rider"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "New delivery update"
        showNotification(
            message.data["notificationId"] ?: message.messageId ?: body.hashCode().toString(),
            title,
            body
        )
    }

    private fun registerToken(token: String) {
        val auth = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
            .getString("auth_token", null)
        if (auth.isNullOrBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AarvoApiClient { auth }.registerRiderFcmToken(token)
            }
        }
    }

    private fun ensureFirebaseConfigured() {
        if (runCatching { FirebaseApp.getApps(this).isNotEmpty() }.getOrDefault(false)) return
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val appId = BuildConfig.FIREBASE_APP_ID
        val senderId = BuildConfig.FIREBASE_SENDER_ID
        if (apiKey.isBlank() || projectId.isBlank() || appId.isBlank() || senderId.isBlank()) return
        runCatching {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setGcmSenderId(senderId)
                    .build()
            )
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        "aarvo_rider",
                        "Rider delivery alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
        }
    }

    private fun showNotification(id: String, title: String, body: String) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(this, RiderDashboardActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            this,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "aarvo_rider")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(id.hashCode(), notification)
    }
}
