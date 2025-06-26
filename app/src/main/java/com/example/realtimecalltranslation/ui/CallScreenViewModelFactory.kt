package com.example.realtimecalltranslation.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.util.AudioRecorderHelper
// Removed AWS and RapidAPI imports that are no longer needed directly by the factory constructor
import com.example.realtimecalltranslation.firebase.CallSignalingManager
// Gson and OkHttpClient might still be needed if Google Cloud clients are manually created here
// For now, assuming Google Cloud clients will be instantiated directly in ViewModel or via their own factories/builders.
// import com.google.gson.Gson
// import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class CallScreenViewModelFactory(
    private val applicationContext: Context,
    private val audioRecorderHelper: AudioRecorderHelper,
    // private val agoraManager: AgoraManager, // Removed
    private val callSignalingManager: CallSignalingManager
) : ViewModelProvider.Factory {

    // OkHttpClient and Gson instances are removed as RapidAPI services are no longer created here.
    // If Google Cloud clients require manual OkHttpClient/Gson, they'll be handled differently.

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallScreenViewModel::class.java)) {
            // RapidAPI services instantiation is removed.
            // Google Cloud service clients will be instantiated inside CallScreenViewModel
            // or passed in a different manner if complex setup is needed.

            // AgoraManager is now a singleton, accessed directly in CallScreenViewModel
            return CallScreenViewModel(
                applicationContext = applicationContext,
                audioRecorderHelper = audioRecorderHelper, // Still passed, might be used for other purposes or removed later
                // agoraManager = agoraManager, // Removed
                callSignalingManager = callSignalingManager
                // AWS and RapidAPI service parameters are removed
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
