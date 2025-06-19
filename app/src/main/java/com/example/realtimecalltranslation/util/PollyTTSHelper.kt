package com.example.realtimecalltranslation.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.ClientConfiguration
import com.amazonaws.services.polly.AmazonPollyPresigningClient
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class PollyTTSHelper(
    private val context: Context,
    private val accessKey: String,
    private val secretKey: String
) {
    private val pollyClient: AmazonPollyPresigningClient by lazy {
        AmazonPollyPresigningClient(
            object : AWSCredentialsProvider {
                override fun getCredentials() = BasicAWSCredentials(accessKey, secretKey)
                override fun refresh() {}
            },
            ClientConfiguration()
        )
    }
    private val mediaPlayer: MediaPlayer by lazy { MediaPlayer() }

    suspend fun speak(text: String, voiceId: String = "Joanna") = withContext(Dispatchers.IO) {
        try {
            val synthReq = SynthesizeSpeechPresignRequest()
                .withText(text)
                .withVoiceId(voiceId)
                .withOutputFormat(OutputFormat.Mp3)
            val presignedUrl: URL = pollyClient.getPresignedSynthesizeSpeechUrl(synthReq)
            mediaPlayer.reset()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            mediaPlayer.setDataSource(presignedUrl.toString())
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
    }

    fun release() {
        mediaPlayer.release()
    }
}