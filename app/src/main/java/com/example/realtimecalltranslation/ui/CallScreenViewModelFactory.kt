package com.example.realtimecalltranslation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.aws.S3Uploader
import com.example.realtimecalltranslation.util.AmazonTranscribeHelper
import com.example.realtimecalltranslation.util.AudioRecorderHelper
import com.example.realtimecalltranslation.util.PollyTTSHelper

@Suppress("UNCHECKED_CAST")
class CallScreenViewModelFactory(
    private val audioRecorderHelper: AudioRecorderHelper,
    private val s3Uploader: S3Uploader,
    private val amazonTranscribeHelper: AmazonTranscribeHelper,
    private val pollyHelper: PollyTTSHelper,
    private val agoraManager: AgoraManager, // AgoraManager might be used for some VM logic if needed
    private val rapidApiKey: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallScreenViewModel::class.java)) {
            return CallScreenViewModel(
                audioRecorderHelper,
                s3Uploader,
                amazonTranscribeHelper,
                pollyHelper,
                agoraManager,
                rapidApiKey
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
