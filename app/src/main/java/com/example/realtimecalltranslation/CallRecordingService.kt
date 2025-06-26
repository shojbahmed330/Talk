package com.example.realtimecalltranslation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.util.Constants

class CallRecordingService : Service() {

    private lateinit var agoraManager: AgoraManager // Use singleton

    companion object {
        private const val TAG = "CallRecordingService"
        private const val CHANNEL_ID = "CallServiceChannel" // Changed name for clarity
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_CALL = "com.example.realtimecalltranslation.ACTION_START_CALL"
        const val ACTION_STOP_CALL = "com.example.realtimecalltranslation.ACTION_STOP_CALL"

        // Intent extras
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_TOKEN = "extra_token"
        const val EXTRA_APP_ID = "extra_app_id" // Though AgoraManager has it, might be good for verification
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_REMOTE_USER_ID = "extra_remote_user_id"
        const val EXTRA_REMOTE_USER_NAME = "extra_remote_user_name" // For notification

        fun startCallService(context: Context, channelName: String, token: String?, appId: String, callId: String?, remoteUserId: String?, remoteUserName: String?) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_APP_ID, appId)
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_REMOTE_USER_ID, remoteUserId)
                putExtra(EXTRA_REMOTE_USER_NAME, remoteUserName ?: "Unknown")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopCallService(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_CALL
            }
            context.startService(intent) // No need for startForegroundService to stop
        }
    }

    override fun onCreate() {
        super.onCreate()
        agoraManager = AgoraManager // Assign singleton
        if (!agoraManager.isInitialized()) {
            // Initialize AgoraManager if it hasn't been by MainActivity (e.g., service started directly)
            // This is a fallback, ideally MainActivity initializes it.
            agoraManager.initialize(applicationContext, Constants.AGORA_APP_ID)
            Log.w(TAG, "AgoraManager was not initialized, initializing in service onCreate.")
        }
        createNotificationChannel()
        Log.d(TAG, "Service created. AgoraManager ready: ${agoraManager.isInitialized()}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_CALL -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME)
                val token = intent.getStringExtra(EXTRA_TOKEN)
                // val appId = intent.getStringExtra(EXTRA_APP_ID) // Not strictly needed for join if AgoraManager is init with it
                val remoteUserName = intent.getStringExtra(EXTRA_REMOTE_USER_NAME) ?: "Ongoing Call"

                if (channelName != null) {
                    startForeground(NOTIFICATION_ID, buildNotification(remoteUserName))
                    Log.d(TAG, "Starting foreground service for call in channel: $channelName")
                    // Join Agora channel - Assuming UID 0 for simplicity or service-specific UID
                    // The actual call logic (STT, TTS) is still primarily in CallScreenViewModel.
                    // This service mainly keeps the Agora connection alive.
                    // If CallScreenViewModel is also joining, ensure they don't clash.
                    // Ideally, only one component (Service or ViewModel) manages the join/leave.
                    // For background, service MUST manage it.
                    if (agoraManager.isInitialized()) {
                        // TODO: Consider if the service needs its own AudioFrameListener or if MainActivity's listener is sufficient/problematic.
                        // For now, let's assume the ViewModel's listener setup is still active if the app is in foreground.
                        // If app is backgrounded, the ViewModel might be destroyed. The service needs to ensure Agora is active.
                        // This might require the service to also set itself as a listener or take over completely.
                        // This part needs careful design to avoid conflicts with ViewModel's Agora usage.
                        // For now, the service ensures the *channel is joined*.
                        agoraManager.joinChannel(channelName, token, 0) // UID 0 for service, or generate one
                        Log.i(TAG, "Service joined Agora channel: $channelName")
                    } else {
                        Log.e(TAG, "AgoraManager not initialized, cannot join channel from service.")
                        stopSelf() // Stop if Agora cannot be used
                    }
                } else {
                    Log.e(TAG, "Channel name is null, cannot start call.")
                    stopSelf()
                }
            }
            ACTION_STOP_CALL -> {
                Log.d(TAG, "Stopping call and service.")
                if (agoraManager.isInitialized()) {
                    agoraManager.leaveChannel()
                    Log.i(TAG, "Service left Agora channel.")
                }
                stopForeground(true)
                stopSelf()
            }
            else -> {
                Log.w(TAG, "Unknown or null action received: ${intent?.action}")
                stopSelf() // Stop if action is not recognized
            }
        }
        return START_STICKY // Keep service running until explicitly stopped
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure channel is left if service is destroyed unexpectedly
        if (agoraManager.isInitialized()) {
            // agoraManager.leaveChannel() // This might be too aggressive if ViewModel is still active
            // Consider if destroying AgoraManager here is appropriate or if it's app-lifecycle bound
            Log.d(TAG, "Service destroyed. Agora channel might still be active if not explicitly left by ACTION_STOP_CALL.")
        }
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service for now
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Call Service", // Updated name
                NotificationManager.IMPORTANCE_HIGH // Higher importance for calls
            ).apply {
                description = "Notification channel for active calls."
                setShowBadge(false) // Or true, depending on preference
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created.")
        }
    }

    private fun buildNotification(contentText: String): Notification {
        // Intent to bring app to foreground when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java) // Assuming MainActivity is the entry
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        // Action to stop the call
        val stopCallIntent = Intent(this, CallRecordingService::class.java).apply {
            action = ACTION_STOP_CALL
        }
        val stopCallPendingIntent = PendingIntent.getService(this, 0, stopCallIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Talk App Call")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with actual call icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes the notification non-dismissible by swiping
            .addAction(R.drawable.ic_call_end_red, "End Call", stopCallPendingIntent) // Replace with actual icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL) // Important for call notifications
            .build()
    }
}