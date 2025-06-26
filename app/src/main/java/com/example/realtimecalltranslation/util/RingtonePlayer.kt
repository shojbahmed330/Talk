package com.example.realtimecalltranslation.util

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log

class RingtonePlayer(private val context: Context) {

    private var ringtone: Ringtone? = null
    private val tag = "RingtonePlayer"

    fun startRingtone() {
        try {
            if (ringtone?.isPlaying == true) {
                Log.d(tag, "Ringtone already playing.")
                return
            }

            // Get default ringtone URI
            var ringtoneUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (ringtoneUri == null) {
                // Fallback to notification sound if no ringtone is set
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (ringtoneUri == null) {
                Log.e(tag, "No default ringtone or notification sound found.")
                return
            }

            ringtone = RingtoneManager.getRingtone(context, ringtoneUri)

            if (ringtone == null) {
                Log.e(tag, "Failed to get Ringtone object for URI: $ringtoneUri")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            } else {
                // For older versions, looping might need to be handled differently or might be default
                // This is a common way, but deprecated. Modern looping is preferred.
                // Alternatively, could use MediaPlayer for more control on older APIs if needed.
                // For API 21+, setLooping on Ringtone object itself if available, or rely on default.
                // This is a known limitation area for very old APIs if direct looping isn't supported well by Ringtone.
            }

            // Ensure audio focus or stream type if necessary, though default usually works.
            // Example: ringtone?.streamType = AudioManager.STREAM_RING

            ringtone?.play()
            Log.d(tag, "Ringtone started.")

        } catch (e: Exception) {
            Log.e(tag, "Error starting ringtone: ${e.message}", e)
            ringtone = null // Ensure it's null if setup failed
        }
    }

    fun stopRingtone() {
        try {
            if (ringtone?.isPlaying == true) {
                ringtone?.stop()
                Log.d(tag, "Ringtone stopped.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping ringtone: ${e.message}", e)
        } finally {
            // Release should be done, but Ringtone objects are sometimes tricky.
            // Stopping is usually enough. Re-creating on next startRingtone is safer.
            ringtone = null
        }
    }

    // Optional: Call this from Activity's onDestroy or when app is closing
    // to ensure resources are not held if a ringtone object was somehow persisted.
    // However, making ringtone a local var in start/stop or nulling it out as done
    // is generally sufficient for typical use cases.
    fun release() {
        stopRingtone() // Ensure it's stopped before releasing
        Log.d(tag, "RingtonePlayer released (currently just ensures stop).")
    }
}
