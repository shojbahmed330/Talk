package com.example.realtimecalltranslation.ui

import android.content.Context // Added import
import android.provider.CallLog // Added import
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.aws.S3Uploader
import com.example.realtimecalltranslation.network.RetrofitInstance
import com.example.realtimecalltranslation.network.TranslationRequest
import com.example.realtimecalltranslation.util.AmazonTranscribeHelper
import com.example.realtimecalltranslation.util.AudioRecorderHelper
import com.example.realtimecalltranslation.util.PollyTTSHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import com.example.realtimecalltranslation.util.CallLogUtils // Added import

class CallScreenViewModel(
    private val applicationContext: Context, // Added applicationContext
    private val audioRecorderHelper: AudioRecorderHelper,
    private val s3Uploader: S3Uploader,
    private val amazonTranscribeHelper: AmazonTranscribeHelper,
    private val pollyTTSHelper: PollyTTSHelper,
    private val agoraManager: AgoraManager,
    private val rapidApiKey: String
) : ViewModel() {

    // State variables
    var isRecording by mutableStateOf(false)
        private set
    var transcriptionStatus by mutableStateOf("Idle")
        private set
    var transcribedText by mutableStateOf("")
        private set
    var translatedText by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val tag = "CallScreenViewModel"
    private var callStartTimeMillis: Long = 0L
    private var currentCallChannel: String? = null

    fun handleRecordAndTranscribePressed() {
        if (isRecording) {
            stopRecordingAndProcess()
        } else {
            startRecordingProcess()
        }
    }

    private fun startRecordingProcess() {
        pollyTTSHelper.stop()
        transcriptionStatus = "Recording..."
        errorMessage = null
        transcribedText = ""
        translatedText = ""
        audioRecorderHelper.startRecording()
        isRecording = true
        Log.d(tag, "Recording started.")
    }

    private fun stopRecordingAndProcess() {
        pollyTTSHelper.stop()
        transcriptionStatus = "Stopping recording..."
        val outputFile = audioRecorderHelper.stopRecording()
        isRecording = false
        Log.d(tag, "Recording stopped. Output file: $outputFile")

        if (outputFile != null) {
            viewModelScope.launch {
                try {
                    transcriptionStatus = "Uploading audio..."
                    errorMessage = null
                    transcribedText = ""
                    translatedText = ""

                    val fileToUpload = File(outputFile)
                    val objectKey = "audio-to-transcribe/${UUID.randomUUID()}.mp4"

                    val s3Uri = s3Uploader.uploadFileToS3(fileToUpload, objectKey)
                    if (s3Uri != null) {
                        Log.d(tag, "Audio uploaded to S3: $s3Uri")
                        transcriptionStatus = "Transcribing audio..."
                        val transcribedTextString = amazonTranscribeHelper.startTranscriptionJob(s3Uri, "en-US")

                        if (transcribedTextString != null) {
                            Log.d(tag, "Transcription successful: $transcribedTextString")
                            transcribedText = transcribedTextString
                            transcriptionStatus = "Translating text..."

                            if (transcribedTextString.isNotBlank()) {
                                try {
                                    val request = TranslationRequest(
                                        q = transcribedTextString,
                                        target = "bn",
                                        source = "en"
                                    )
                                    val response = RetrofitInstance.api.translate(
                                        body = request,
                                        apiKey = rapidApiKey
                                    )
                                    if (response.isSuccessful) {
                                        val translationResult = response.body()?.data?.translations?.firstOrNull()?.translatedText
                                        if (translationResult != null) {
                                            Log.d(tag, "Translation successful: $translationResult")
                                            translatedText = translationResult
                                            transcriptionStatus = "Speaking translated text..."
                                            try {
                                                val targetVoiceId = "Zoya"
                                                pollyTTSHelper.speak(translationResult, voiceId = targetVoiceId)
                                                transcriptionStatus = "Playing translated audio."
                                                Log.d(tag, "TTS started for: $translationResult")
                                            } catch (e: Exception) {
                                                Log.e(tag, "TTS Error: ${e.message}", e)
                                                errorMessage = "TTS Error: ${e.localizedMessage}"
                                                transcriptionStatus = "TTS failed."
                                            }
                                        } else {
                                            Log.e(tag, "Translation result was empty.")
                                            errorMessage = "Translation result was empty."
                                            transcriptionStatus = "Error translating"
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e(tag, "Translation API error: ${response.code()} - ${response.message()} - $errorBody")
                                        errorMessage = "Translation API error: ${response.code()}"
                                        transcriptionStatus = "Error translating"
                                    }
                                } catch (e: Exception) {
                                    Log.e(tag, "Translation failed: ${e.message}", e)
                                    errorMessage = "Translation failed: ${e.localizedMessage}"
                                    transcriptionStatus = "Error translating"
                                }
                            } else {
                                transcriptionStatus = "Nothing to translate"
                            }
                        } else {
                            Log.e(tag, "Transcription failed or returned null.")
                            errorMessage = "Transcription failed."
                            transcriptionStatus = "Error"
                        }
                    } else {
                        Log.e(tag, "S3 Upload failed.")
                        errorMessage = "S3 Upload failed."
                        transcriptionStatus = "Error"
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error in STT-Translate-TTS process: ${e.message}", e)
                    errorMessage = "Error: ${e.localizedMessage}"
                    transcriptionStatus = "Error"
                } finally {
                    outputFile.let { File(it).delete() }
                    Log.d(tag, "Cleaned up local audio file: $outputFile")
                }
            }
        } else {
            Log.e(tag, "Audio recording output file is null.")
            errorMessage = "Audio recording failed."
            transcriptionStatus = "Error"
        }
    }

    fun stopOngoingTTS() {
        pollyTTSHelper.stop()
    }

    fun stopOngoingRecordingAndCleanup() {
        if (isRecording) {
            val outputFile = audioRecorderHelper.stopRecording()
            isRecording = false
            if (outputFile != null) {
                File(outputFile).delete()
                Log.d(tag, "Ongoing recording stopped and file $outputFile deleted.")
            }
        }
    }

    // Agora-related methods
    fun joinCall(channel: String, token: String?, appId: String) { // Assuming appId is still needed
        agoraManager.joinChannel(channel, token, 0)
        this.currentCallChannel = channel
        this.callStartTimeMillis = System.currentTimeMillis()
        Log.d(tag, "Joined Agora channel: $channel, recording start time.")
    }

    fun leaveCall() {
        agoraManager.leaveChannel()
        logCurrentCall() // Call new private method to log the call
        Log.d(tag, "Left Agora channel")
    }

    private fun logCurrentCall() {
        if (callStartTimeMillis > 0 && !currentCallChannel.isNullOrBlank()) {
            val durationSeconds = (System.currentTimeMillis() - callStartTimeMillis) / 1000

            CallLogUtils.addCallToDeviceLog(
                applicationContext,
                currentCallChannel,
                CallLog.Calls.OUTGOING_TYPE, // Assuming outgoing, adjust if type is known differently
                callStartTimeMillis,
                durationSeconds
            )
            Log.d(tag, "Attempted to log call: Number: $currentCallChannel, Duration: $durationSeconds s")

            // Reset for next call
            callStartTimeMillis = 0L
            currentCallChannel = null
        }
    }

    fun toggleMute(isMuted: Boolean) {
        agoraManager.muteLocalAudioStream(isMuted)
        Log.d(tag, "Mute toggled: $isMuted")
    }

    fun toggleSpeaker(isSpeakerOn: Boolean) {
        agoraManager.setEnableSpeakerphone(isSpeakerOn)
        Log.d(tag, "Speaker toggled: $isSpeakerOn")
    }

    fun toggleHold(isOnHold: Boolean) {
        if (isOnHold) {
            agoraManager.muteLocalAudioStream(true)
        } else {
            agoraManager.muteLocalAudioStream(false)
        }
        Log.d(tag, "Hold toggled: $isOnHold")
    }

    override fun onCleared() {
        super.onCleared()
        // leaveCall() is called here, which now includes logCurrentCall()
        // If there's any specific order needed (e.g. log before full cleanup), ensure leaveCall handles it.
        stopOngoingRecordingAndCleanup()
        stopOngoingTTS()
        leaveCall() // This will attempt to log the call if it was active
        Log.d(tag, "ViewModel cleared.")
    }
}