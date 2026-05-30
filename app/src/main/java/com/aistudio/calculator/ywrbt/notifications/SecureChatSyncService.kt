package com.aistudio.calculator.ywrbt.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.calculator.ywrbt.MainActivity
import com.aistudio.calculator.ywrbt.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SecureChatSyncService : Service() {

    private var messagesListener: ListenerRegistration? = null
    private var isListenerRegistered = false

    companion object {
        private const val TAG = "SecureChatSyncService"
        const val CHANNEL_ID = "secure_sync_channel"
        const val NOTIFICATION_ID = 4567

        @Volatile
        var isServiceRunning = false

        fun startService(context: Context) {
            val intent = Intent(context, SecureChatSyncService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SecureChatSyncService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service: ${e.message}", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: SecureChatSyncService created")
        isServiceRunning = true
        createNotificationChannel()
        startForegroundWithNotification()
        setupFirestoreListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: service started")
        setupFirestoreListener()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val title = "कैलकुलेटर वॉल्ट (Secure Chat Sync)"
        val message = "रीयल-टाइम संदेशों के लिए बैकग्राउंड सिंक सक्रिय है"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startForeground: ${e.message}", e)
            // Fallback without service type to try and avoid crashes on non-compliant systems
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback startForeground failed: ${fallbackEx.message}", fallbackEx)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Secure Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keep background synchronization running for real-time secure messaging"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupFirestoreListener() {
        if (isListenerRegistered) return

        val prefs = getSharedPreferences("calculator_vault_prefs", Context.MODE_PRIVATE)
        val activeUser = prefs.getString("active_user", "") ?: ""

        if (activeUser.isEmpty()) {
            Log.d(TAG, "setupFirestoreListener: No active logged-in user. Listening bypassed.")
            return
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }

            val db = FirebaseFirestore.getInstance()
            messagesListener?.remove()

            Log.d(TAG, "setupFirestoreListener: Starting background Firestore listener for user: $activeUser")
            
            messagesListener = db.collection("messages")
                .whereEqualTo("recipient", activeUser)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "setupFirestoreListener error: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            if (change.type == DocumentChange.Type.ADDED) {
                                val doc = change.document
                                val sender = doc.getString("sender") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val isSeen = doc.getBoolean("isSeen") ?: false
                                val text = doc.getString("text") ?: ""

                                val now = System.currentTimeMillis()
                                if (sender.isNotEmpty() && sender != activeUser && !isSeen && (now - timestamp < 15_000)) {
                                    if (!MainActivity.isAppInForeground) {
                                        Log.d(TAG, "Showing background notification for new message from $sender")
                                        showNotification(
                                            this@SecureChatSyncService, 
                                            "New message from $sender", 
                                            text
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            isListenerRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "setupFirestoreListener exception: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: SecureChatSyncService destroyed")
        messagesListener?.remove()
        messagesListener = null
        isListenerRegistered = false
        isServiceRunning = false
        super.onDestroy()
    }
}
