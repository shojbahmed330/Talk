package com.example.realtimecalltranslation.ui

import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimecalltranslation.agora.AgoraManager
// import com.example.realtimecalltranslation.aws.S3Uploader // Removed
// import com.example.realtimecalltranslation.network.RetrofitInstance // Potentially keep if Google Translate uses it, for now remove
// import com.example.realtimecalltranslation.network.TranslationRequest // Potentially keep if Google Translate uses it, for now remove
// import com.example.realtimecalltranslation.util.AmazonTranscribeHelper // Removed
import com.example.realtimecalltranslation.util.AudioRecorderHelper // Might be removed later if not used
// import com.example.realtimecalltranslation.util.PollyTTSHelper // Removed
import kotlinx.coroutines.launch
import java.io.File // Keep for AudioRecorderHelper if it saves files
import java.util.UUID // Keep for CallLogUtils or other ID generation
import com.example.realtimecalltranslation.util.CallLogUtils
import com.example.realtimecalltranslation.agora.AudioFrameListener
import android.media.AudioAttributes
import android.media.MediaPlayer // Keep for playing audio from Google TTS
// Removed RapidAPI service imports
import kotlinx.coroutines.Job // Keep for managing coroutines
import java.io.ByteArrayOutputStream // Keep for audio buffering
import java.util.concurrent.ConcurrentHashMap // Keep for audio buffering
import kotlinx.coroutines.Dispatchers
import com.example.realtimecalltranslation.firebase.CallSignalingManager
import com.example.realtimecalltranslation.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Google Cloud STT imports
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1p1beta1.RecognitionConfig
import com.google.cloud.speech.v1p1beta1.SpeechClient
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest
import com.google.cloud.speech.v1p1beta1.SpeechSettings
import com.google.api.gax.rpc.ClientStream
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import com.example.realtimecalltranslation.R // For loading credentials from raw resources
import io.grpc.StatusRuntimeException
import java.io.InputStream

// Google Cloud Translate imports
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation

// Google Cloud TTS imports
import com.google.cloud.texttospeech.v1.AudioConfig
import com.google.cloud.texttospeech.v1.AudioEncoding
import com.google.cloud.texttospeech.v1.SynthesisInput
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse
import com.google.cloud.texttospeech.v1.TextToSpeechClient
import com.google.cloud.texttospeech.v1.TextToSpeechSettings
import com.google.cloud.texttospeech.v1.VoiceSelectionParams
import com.example.realtimecalltranslation.CallRecordingService



class CallScreenViewModel(
    private val applicationContext: Context,
    private val audioRecorderHelper: AudioRecorderHelper, // Will be evaluated later
    // private val agoraManager: AgoraManager, // Removed, will use singleton AgoraManager object directly
    private val callSignalingManager: CallSignalingManager
    // Removed s3Uploader, amazonTranscribeHelper, pollyTTSHelper
    // Removed rapidSpeechToTextService, rapidTranslationService, rapidTextToSpeechService
) : ViewModel(), AudioFrameListener {

    // Access AgoraManager singleton
    private val agoraManager = AgoraManager

    // State variables for UI
    var localUserTranscribedText by mutableStateOf("")
        private set
    var localUserTranslatedText by mutableStateOf("")
        private set
    var remoteUserTranscribedText by mutableStateOf("")
        private set
    var remoteUserTranslatedText by mutableStateOf("")
        private set
    var currentStatusMessage by mutableStateOf("Idle")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var isProcessingLocalTTS by mutableStateOf(false) // To control mic data sending

    private val _isRemoteUserJoined = MutableStateFlow(false)
    val isRemoteUserJoined: StateFlow<Boolean> = _isRemoteUserJoined.asStateFlow()

    // Internal state
    private val tag = "CallScreenViewModel"
    private var callStartTimeMillis: Long = 0L // Will be set when remote user joins
    private var currentCallChannel: String? = null
    private var currentCallUserName: String? = null
    private var currentCallId: String? = null
    private var currentRemoteUserId: String? = null

    // Audio Buffering
    private val localAudioBuffer = ByteArrayOutputStream()
    private val remoteAudioBuffers = ConcurrentHashMap<Int, ByteArrayOutputStream>()
    private val audioChunkSizeThreshold = 16000 * 2 * 2 // Approx 2 seconds of 16kHz 16-bit mono audio

    private var localSttJob: Job? = null
    private val remoteSttJobs = ConcurrentHashMap<Int, Job>()

    private var mediaPlayer: MediaPlayer? = null

    // User language preferences
    // Using BCP-47 language tags. Ensure these are supported by Google Cloud STT, Translate, and TTS.
    var localUserLanguageCode = "bn-BD" // Bengali (Bangladesh) for local user (User A)
    var remoteUserExpectedLanguageCode = "en-US" // English (United States) for remote user (User B)


    // --- Google Cloud Clients ---
    private var speechClient: SpeechClient? = null
    private var translateClient: Translate? = null
    private var textToSpeechClient: TextToSpeechClient? = null

    // For Google STT Streaming
    private var localResponseObserver: ResponseObserver<StreamingRecognizeResponse>? = null
    private var localClientStream: ClientStream<StreamingRecognizeRequest>? = null
    private val remoteResponseObservers = ConcurrentHashMap<Int, ResponseObserver<StreamingRecognizeResponse>>()
    private val remoteClientStreams = ConcurrentHashMap<Int, ClientStream<StreamingRecognizeRequest>>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val credentialsStream: InputStream = applicationContext.resources.openRawResource(R.raw.google_credentials)
                val speechSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider { GoogleCredentials.fromStream(credentialsStream) } // Speech client uses this
                    .build()
                speechClient = SpeechClient.create(speechSettings)
                Log.d(tag, "SpeechClient initialized successfully.")

                // Initialize Translate client
                // Reset stream for Translate client, as it's consumed by SpeechClient's credentials provider
                val translateCredentialsStream: InputStream = applicationContext.resources.openRawResource(R.raw.google_credentials)
                val translateOptions = TranslateOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(translateCredentialsStream))
                    .build()
                translateClient = translateOptions.service
                Log.d(tag, "TranslateClient initialized successfully.")
                translateCredentialsStream.close() // Close the stream for translate client

                // Initialize TextToSpeech client
                val ttsCredentialsStream: InputStream = applicationContext.resources.openRawResource(R.raw.google_credentials)
                val ttsSettings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider { GoogleCredentials.fromStream(ttsCredentialsStream) }
                    .build()
                textToSpeechClient = TextToSpeechClient.create(ttsSettings)
                Log.d(tag, "TextToSpeechClient initialized successfully.")
                ttsCredentialsStream.close()

            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize Google Cloud clients: ${e.message}", e)
                errorMessage = "Failed to initialize translation services."
                // Ensure all opened streams are attempted to be closed in case of error
                try {
                    applicationContext.resources.openRawResource(R.raw.google_credentials).close()
                } catch (closeException: Exception) {
                    Log.w(tag, "Error closing credentials stream during error handling: ${closeException.message}")
                }
            }
        }
    }


    // Method to stop any ongoing MediaPlayer playback
    fun stopMediaPlayer() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            // Do not release here, it might be reused. Release in onCleared.
        } catch (e: IllegalStateException) {
            Log.e(tag, "MediaPlayer stop/reset error: ${e.message}")
        }
        // Set to null if you want to recreate it every time.
        // Or keep the instance and just reset. For now, let's reset and reuse.
        // mediaPlayer = null
        currentStatusMessage = "Media player stopped."
    }

    // Clean up any stray audio recording files if AudioRecorderHelper was used
    fun cleanupOldRecordings() {
        // This function might be more complex depending on how AudioRecorderHelper manages files.
        // For now, it's a placeholder if direct file recording was ever started and not cleaned up.
        // If AudioRecorderHelper is only used for direct byte processing, this might not be needed.
        Log.d(tag, "Attempting to clean up any old recording files if AudioRecorderHelper was used for saving.")
        // audioRecorderHelper.cleanupTemporaryFiles() // Assuming such a method exists or implement logic here
    }

    fun joinCall(
        channel: String,
        token: String?,
        appId: String,
        userName: String?, // This is remote user's name for this call session
        callId: String?,
        remoteUserId: String?,
        isLocalUserFromUSA: Boolean // New parameter
    ) {
        // Set language codes based on who the local user is
        if (isLocalUserFromUSA) {
            localUserLanguageCode = "en-US"
            remoteUserExpectedLanguageCode = "bn-BD"
        } else {
            localUserLanguageCode = "bn-BD"
            remoteUserExpectedLanguageCode = "en-US"
        }
        Log.d(tag, "Language codes set: localUserLanguageCode=$localUserLanguageCode, remoteUserExpectedLanguageCode=$remoteUserExpectedLanguageCode based on isLocalUserFromUSA=$isLocalUserFromUSA")

        // agoraManager is initialized by MainActivity or Service's onCreate
        // We now start the service to handle the call lifecycle, including Agora joining.
        CallRecordingService.startCallService(
            context = applicationContext,
            channelName = channel,
            token = token,
            appId = agoraManager.currentAppId ?: Constants.AGORA_APP_ID, // Assuming AGORA_APP_ID is accessible or pass explicitly
            callId = callId,
            remoteUserId = remoteUserId,
            remoteUserName = userName
        )

        // ViewModel still needs to know current call details for its logic (STT, TTS, logging)
        this.currentCallChannel = channel
        this.currentCallUserName = userName
        this.currentCallId = callId
        this.currentRemoteUserId = remoteUserId
        // this.callStartTimeMillis = System.currentTimeMillis() // Moved to notifyRemoteUserJoined
        Log.d(tag, "Joining Agora channel: $channel, User: $userName, CallID: $callId, RemoteUserID: $remoteUserId. Waiting for remote user to join to start timer.")
    }

    fun notifyRemoteUserJoined() {
        if (!_isRemoteUserJoined.value) { // Ensure this is called only once or when state is actually changing
            _isRemoteUserJoined.value = true
            callStartTimeMillis = System.currentTimeMillis()
            Log.d(tag, "Remote user joined. Call timer started. Start time: $callStartTimeMillis")
        }
    }

    fun leaveCall() {
        // Stop the CallRecordingService, which will handle leaving the Agora channel.
        CallRecordingService.stopCallService(applicationContext)

        logCurrentCall() // Log the call details

        val callIdToUpdate = currentCallId
        val remoteIdForNode = currentRemoteUserId

        if (callIdToUpdate != null && remoteIdForNode != null) {
            Log.d(tag, "Signaling call end. CallID: $callIdToUpdate, RemoteUser (owner of request): $remoteIdForNode")
            callSignalingManager.removeCallRequest(remoteIdForNode, callIdToUpdate)
        } else {
            Log.w(tag, "Cannot signal call end: callId or remoteUserId is null. CallID: $callIdToUpdate, RemoteUserID: $remoteIdForNode")
        }
        Log.d(tag, "Left Agora channel: $currentCallChannel")

        currentCallChannel = null
        currentCallUserName = null
        currentCallId = null
        currentRemoteUserId = null
        callStartTimeMillis = 0L
    }

    private fun logCurrentCall() {
        if (callStartTimeMillis > 0 && !currentCallChannel.isNullOrBlank()) {
            val durationSeconds = (System.currentTimeMillis() - callStartTimeMillis) / 1000

            CallLogUtils.addCallToDeviceLog(
                applicationContext,
                currentCallChannel,
                CallLog.Calls.OUTGOING_TYPE,
                callStartTimeMillis,
                durationSeconds,
                currentCallUserName
            )
            Log.d(tag, "Attempted to log call: Number: $currentCallChannel, Name: $currentCallUserName, Duration: $durationSeconds s")

            callStartTimeMillis = 0L
            currentCallChannel = null
            currentCallUserName = null
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
        Log.d(tag, "onCleared: Starting cleanup.")

        // Stop and release MediaPlayer
        stopMediaPlayer() // Ensures it's stopped before release
        try {
            mediaPlayer?.release()
            Log.d(tag, "MediaPlayer released.")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing mediaPlayer: ${e.message}")
        }
        mediaPlayer = null

        // Cancel Coroutine Jobs
        try {
            localSttJob?.cancel()
            remoteSttJobs.values.forEach { it.cancel() }
            remoteSttJobs.clear()
            Log.d(tag, "STT Coroutine jobs cancelled.")
        } catch (e: Exception) {
            Log.e(tag, "Error cancelling STT jobs: ${e.message}")
        }

        // Close Google STT streams and client
        try {
            localClientStream?.closeSend()
            remoteClientStreams.values.forEach { it.closeSend() }
            remoteClientStreams.clear()
            Log.d(tag, "All STT client streams closed.")
        } catch (e: Exception) {
            Log.e(tag, "Error closing STT client streams: ${e.message}")
        }

        try {
            speechClient?.close()
            speechClient = null
            Log.d(tag, "SpeechClient closed.")
        } catch (e: Exception) {
            Log.e(tag, "Error closing SpeechClient: ${e.message}")
        }

        try {
            textToSpeechClient?.close()
            textToSpeechClient = null
            Log.d(tag, "TextToSpeechClient closed.")
        } catch (e: Exception) {
            Log.e(tag, "Error closing TextToSpeechClient: ${e.message}")
        }
        // TODO: Close Google Translate client when its close method is identified (if any)

        leaveCall() // Ensure call is logged and signaling is handled
        Log.d(tag, "onCleared: Cleanup finished.")
    }

    override fun onLocalAudioFrame(audioFrame: ByteArray) {
        // This method is called by AgoraManager when a local audio frame is recorded.
        // It should decide whether to process it for STT (if user is speaking)
        // or send it directly if no TTS is being processed for the local user.

        if (isProcessingLocalTTS) {
            // If local TTS audio is being prepared/sent, don't send raw mic data.
            // Log.d(tag, "onLocalAudioFrame: Currently processing/sending local TTS, skipping raw mic data.")
            return
        }

        // Option 1: Send raw audio directly if not processing TTS
        // This ensures the other user hears the local user's voice when not translating.
        // agoraManager.sendCustomAudioBytes(audioFrame)
        // Log.d(tag, "onLocalAudioFrame: Sent raw local audio frame as no TTS is active.")

        // Option 2: Buffer for STT (current approach)
        // This means user's direct voice is only heard via STT->Translate->TTS at the other end,
        // or if we decide to send raw audio (Option 1) when STT is not producing final results.
        if (localSttJob?.isActive == true && localClientStream == null) {
            // If a job is active but stream is null, it might be finishing up.
            // Avoid starting a new stream immediately to prevent overlap.
            Log.d(tag, "onLocalAudioFrame: localSttJob active but stream is null, possibly finishing. Buffering.")
        } else if (localClientStream == null && audioFrame.isNotEmpty()) {
            // If no stream is active, and we have new audio, start a new STT stream.
            // This typically happens after a previous phrase was finalized and stream closed.
            Log.d(tag, "onLocalAudioFrame: No active local STT stream, starting new one for STT.")
            // The first chunk of a new utterance will initialize the stream in processLocalAudioChunkWithGoogle
        }


        synchronized(localAudioBuffer) {
            localAudioBuffer.write(audioFrame)
            if (localAudioBuffer.size() >= audioChunkSizeThreshold) {
                val audioDataToProcess = localAudioBuffer.toByteArray()
                localAudioBuffer.reset()
                if (speechClient != null) { // Ensure speech client is ready
                    processLocalAudioChunkWithGoogle(audioDataToProcess)
                } else {
                    Log.w(tag, "onLocalAudioFrame: SpeechClient not ready, cannot process audio chunk.")
                }
            }
        }
    }

    private fun processLocalAudioChunkWithGoogle(audioData: ByteArray) {
        if (audioData.isEmpty() || speechClient == null) {
            if (speechClient == null) Log.w(tag, "SpeechClient not initialized. Skipping local STT.")
            return
        }

        localSttJob = viewModelScope.launch(Dispatchers.IO) {
            currentStatusMessage = "Transcribing your voice..."
            Log.d(tag, "processLocalAudioChunkWithGoogle: Processing ${audioData.size} bytes. Language: $localUserLanguageCode")

            if (localClientStream == null) {
                localResponseObserver = object : ResponseObserver<StreamingRecognizeResponse> {
                    override fun onStart(controller: StreamController) {}
                    override fun onResponse(response: StreamingRecognizeResponse) {
                        viewModelScope.launch {
                            if (response.resultsList.isNotEmpty()) {
                                val result = response.resultsList.first()
                                if (result.alternativesList.isNotEmpty()) {
                                    val transcript = result.alternativesList.first().transcript
                                    if (result.isFinal) {
                                        localUserTranscribedText = transcript
                                        Log.d(tag, "Local STT Final: $transcript")
                                        currentStatusMessage = "You: $transcript. Translating..."

                                        val targetLangForRemote = if (localUserLanguageCode.startsWith("bn")) "en" else "bn"
                                        try {
                                            if (translateClient != null) {
                                                val translation: Translation = translateClient!!.translate(
                                                    transcript,
                                                    Translate.TranslateOption.sourceLanguage(localUserLanguageCode.substringBefore("-")), // e.g., "bn"
                                                    Translate.TranslateOption.targetLanguage(targetLangForRemote) // e.g., "en"
                                                )
                                                val translatedText = translation.translatedText
                                                localUserTranslatedText = translatedText
                                                currentStatusMessage = "Translation (local to remote): $translatedText"
                                                Log.d(tag, "Local to Remote Translation: $translatedText. Synthesizing speech...")
                                                isProcessingLocalTTS = true // Start blocking mic data

                                                try {
                                                    if (textToSpeechClient != null) {
                                                        val input = SynthesisInput.newBuilder().setText(translatedText).build()
                                                        val voice = VoiceSelectionParams.newBuilder()
                                                            .setLanguageCode(targetLangForRemote) // e.g., "en-US"
                                                            .setName(if (targetLangForRemote.startsWith("en")) "en-US-Neural2-J" else "bn-IN-Standard-A") // Example voices
                                                            .build()
                                                        val audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(16000).build() // For Agora

                                                        val response = textToSpeechClient!!.synthesizeSpeech(input, voice, audioConfig)
                                                        val audioContents = response.audioContent
                                                        Log.d(tag, "Local to Remote TTS successful. Audio size: ${audioContents.size()} bytes.")
                                                        // Send the synthesized audio to the remote user via AgoraManager
                                                        if (audioContents.size() > 0) {
                                                            agoraManager.sendCustomAudioBytes(audioContents.toByteArray())
                                                            Log.d(tag, "Sent translated audio bytes to remote user via Agora.")
                                                        }
                                                    } else {
                                                        Log.e(tag, "TextToSpeechClient is null. Cannot synthesize speech.")
                                                        errorMessage = "Text-to-Speech service not available."
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(tag, "Error during local TTS synthesis or sending: ${e.message}", e)
                                                    errorMessage = "Error generating or sending your translated speech."
                                                } finally {
                                                    isProcessingLocalTTS = false // Stop blocking mic data
                                                }
                                            } else {
                                                Log.e(tag, "TranslateClient is null. Cannot translate.")
                                                errorMessage = "Translation service not available."
                                            }
                                        } catch (e: Exception) {
                                            Log.e(tag, "Local to Remote Translation failed: ${e.message}", e)
                                            errorMessage = "Error translating your speech: ${e.localizedMessage}"
                                        }
                                    } else {
                                        // Interim results can be shown if needed
                                        Log.d(tag, "Local STT Interim: $transcript")
                                    }
                                }
                            }
                        }
                    }
                    override fun onError(t: Throwable) {
                        Log.e(tag, "Local STT onError: ${t.message}", t)
                        errorMessage = "Speech recognition error (local): ${t.localizedMessage}"
                        currentStatusMessage = "Local STT Error."
                        stopLocalSttStream() // Clean up stream on error
                    }
                    override fun onComplete() {
                        Log.d(tag, "Local STT onComplete.")
                        currentStatusMessage = "Local STT stream ended."
                        // Stream can complete if silence is detected or max duration reached.
                        // Consider restarting the stream if appropriate for continuous transcription.
                        stopLocalSttStream()
                    }
                }
                localClientStream = speechClient!!.streamingRecognizeCallable().splitCall(localResponseObserver)

                val recognitionConfig = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000) // Agora default is 16kHz
                    .setLanguageCode(localUserLanguageCode) // e.g., "bn-BD" or "en-US"
                    .setEnableAutomaticPunctuation(true)
                    .build()
                val streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(recognitionConfig)
                    .setInterimResults(true) // Get interim results
                    .build()
                val request = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build()
                localClientStream?.send(request)
                Log.d(tag, "Local STT stream started with language: $localUserLanguageCode")
            }

            try {
                val request = StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioData))
                    .build()
                localClientStream?.send(request)
            } catch (e: Exception) {
                Log.e(tag, "Error sending local audio data to STT: ${e.message}", e)
                errorMessage = "Error sending audio for transcription."
                stopLocalSttStream()
            }
        }
    }

    private fun stopLocalSttStream() {
        viewModelScope.launch {
            try {
                localClientStream?.closeSend()
            } catch (e: Exception) {
                Log.w(tag, "Exception while closing local STT send stream: ${e.message}")
            }
            localClientStream = null
            localResponseObserver = null // Clear observer
            Log.d(tag, "Local STT stream resources cleaned up.")
        }
    }


    override fun onRemoteAudioFrame(audioFrame: ByteArray, uid: Int) {
        if (speechClient == null) {
            Log.w(tag, "SpeechClient not initialized. Skipping remote STT for UID: $uid.")
            return
        }
        if (remoteSttJobs[uid]?.isActive == true) return

        var userBuffer = remoteAudioBuffers[uid]
        if (userBuffer == null) {
            val newBuffer = ByteArrayOutputStream()
            val existingBuffer = remoteAudioBuffers.putIfAbsent(uid, newBuffer)
            userBuffer = existingBuffer ?: newBuffer
        }

        synchronized(userBuffer!!) {
            userBuffer.write(audioFrame)
            Log.d(tag, "Remote audio frame received UID $uid, buffer size: ${userBuffer.size()}")
            if (userBuffer.size() >= audioChunkSizeThreshold) {
                val audioData = userBuffer.toByteArray()
                userBuffer.reset()
                processRemoteAudioChunkWithGoogle(audioData, uid)
            }
        }
    }

    private fun processRemoteAudioChunkWithGoogle(audioData: ByteArray, uid: Int) {
        if (audioData.isEmpty() || speechClient == null) {
            if (speechClient == null) Log.w(tag, "SpeechClient not initialized. Skipping remote STT for UID: $uid.")
            return
        }

        remoteSttJobs[uid] = viewModelScope.launch(Dispatchers.IO) {
            currentStatusMessage = "Transcribing remote voice (UID: $uid)..."
            Log.d(tag, "processRemoteAudioChunkWithGoogle UID $uid: Processing ${audioData.size} bytes. Language: $remoteUserExpectedLanguageCode")

            var clientStream = remoteClientStreams[uid]
            if (clientStream == null) {
                val responseObserver = object : ResponseObserver<StreamingRecognizeResponse> {
                    override fun onStart(controller: StreamController) {}
                    override fun onResponse(response: StreamingRecognizeResponse) {
                        viewModelScope.launch {
                            if (response.resultsList.isNotEmpty()) {
                                val result = response.resultsList.first()
                                if (result.alternativesList.isNotEmpty()) {
                                    val transcript = result.alternativesList.first().transcript
                                    if (result.isFinal) {
                                        remoteUserTranscribedText = transcript // Assuming one remote user for now for UI
                                        Log.d(tag, "Remote STT Final (UID $uid): $transcript")
                                        currentStatusMessage = "Remote (UID $uid): $transcript. Translating..."

                                        val targetLangForLocal = if (remoteUserExpectedLanguageCode.startsWith("en")) "bn" else "en"
                                        try {
                                            if (translateClient != null) {
                                                val translation: Translation = translateClient!!.translate(
                                                    transcript,
                                                    Translate.TranslateOption.sourceLanguage(remoteUserExpectedLanguageCode.substringBefore("-")), // e.g., "en"
                                                    Translate.TranslateOption.targetLanguage(targetLangForLocal) // e.g., "bn"
                                                )
                                                val translatedText = translation.translatedText
                                                remoteUserTranslatedText = translatedText // Update UI for local user
                                                currentStatusMessage = "Translation (remote to local): $translatedText"
                                                Log.d(tag, "Remote to Local Translation: $translatedText. Synthesizing speech...")

                                                if (textToSpeechClient != null) {
                                                    val input = SynthesisInput.newBuilder().setText(translatedText).build()
                                                    val voice = VoiceSelectionParams.newBuilder()
                                                        .setLanguageCode(targetLangForLocal) // e.g., "bn-IN"
                                                        .setName(if (targetLangForLocal.startsWith("bn")) "bn-IN-Wavenet-A" else "en-US-Neural2-A") // Example voices
                                                        .build()
                                                    val audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(16000).build()

                                                    val response = textToSpeechClient!!.synthesizeSpeech(input, voice, audioConfig)
                                                    val audioContents = response.audioContent
                                                    Log.d(tag, "Remote to Local TTS successful. Audio size: ${audioContents.size()} bytes.")
                                                    playAudioBytes(audioContents.toByteArray()) // Play the synthesized audio locally
                                                } else {
                                                    Log.e(tag, "TextToSpeechClient is null. Cannot synthesize speech.")
                                                    errorMessage = "Text-to-Speech service not available."
                                                }
                                            } else {
                                                Log.e(tag, "TranslateClient is null. Cannot translate.")
                                                errorMessage = "Translation service not available."
                                            }
                                        } catch (e: Exception) {
                                            Log.e(tag, "Remote to Local Translation failed: ${e.message}", e)
                                            errorMessage = "Error translating remote speech: ${e.localizedMessage}"
                                        }
                                    } else {
                                        Log.d(tag, "Remote STT Interim (UID $uid): $transcript")
                                    }
                                }
                            }
                        }
                    }
                    override fun onError(t: Throwable) {
                        Log.e(tag, "Remote STT onError (UID $uid): ${t.message}", t)
                        errorMessage = "Speech recognition error (remote $uid): ${t.localizedMessage}"
                        currentStatusMessage = "Remote STT Error (UID $uid)."
                        stopRemoteSttStream(uid)
                    }
                    override fun onComplete() {
                        Log.d(tag, "Remote STT onComplete (UID $uid).")
                        currentStatusMessage = "Remote STT stream ended (UID $uid)."
                        stopRemoteSttStream(uid)
                    }
                }
                clientStream = speechClient!!.streamingRecognizeCallable().splitCall(responseObserver)
                remoteClientStreams[uid] = clientStream
                remoteResponseObservers[uid] = responseObserver // Store observer if needed for direct interaction

                val recognitionConfig = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode(remoteUserExpectedLanguageCode) // e.g., "en-US"
                    .setEnableAutomaticPunctuation(true)
                    .build()
                val streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(recognitionConfig)
                    .setInterimResults(true)
                    .build()
                val request = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build()
                clientStream.send(request)
                Log.d(tag, "Remote STT stream started for UID $uid with language: $remoteUserExpectedLanguageCode")
            }

            try {
                val request = StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioData))
                    .build()
                clientStream.send(request)
            } catch (e: Exception) {
                Log.e(tag, "Error sending remote audio data to STT (UID $uid): ${e.message}", e)
                errorMessage = "Error sending remote audio for transcription."
                stopRemoteSttStream(uid)
            }
        }
    }

    private fun stopRemoteSttStream(uid: Int) {
        viewModelScope.launch {
            try {
                remoteClientStreams[uid]?.closeSend()
            } catch (e: Exception) {
                Log.w(tag, "Exception while closing remote STT send stream for UID $uid: ${e.message}")
            }
            remoteClientStreams.remove(uid)
            remoteResponseObservers.remove(uid)
            remoteSttJobs.remove(uid) // Also remove the job associated with this stream
            Log.d(tag, "Remote STT stream resources cleaned up for UID $uid.")
        }
    }

    // Adapted to play byte array directly, or could take a URL
    private fun playAudioBytes(audioData: ByteArray) {
        // This method would need to save bytes to a temp file to use MediaPlayer with setDataSource(filePath)
        // Or, use AudioTrack for direct PCM byte playback if Google TTS provides raw PCM.
        // For simplicity, let's assume we get a URL for now and rename this or add a new method.
        // The existing playAudioFromUrl is fine if Google TTS returns a URL or can generate one.
        // If Google TTS returns bytes, we need a different playback mechanism or save to temp file.
        Log.d(tag, "playAudioBytes called with ${audioData.size} bytes. Needs implementation (e.g., save to temp file and use MediaPlayer, or use AudioTrack).")
        // For now, let's simulate playing by logging and setting status.
        // In a real scenario, you'd convert these bytes to playable audio.
        currentStatusMessage = "Playing audio bytes (Google)... (Simulated)"

        // Example of saving to a temp file and playing (if audioData is e.g. MP3 bytes)
        viewModelScope.launch(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                tempFile = File.createTempFile("tts_audio_bytes", ".mp3", applicationContext.cacheDir)
                tempFile.writeBytes(audioData)

                mediaPlayer?.release() // Release previous instance
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    prepareAsync()
                    setOnPreparedListener {
                        Log.d(tag, "MediaPlayer prepared from bytes, starting playback.")
                        start()
                        currentStatusMessage = "Playing translated speech (Google)."
                    }
                    setOnCompletionListener {
                        Log.d(tag, "MediaPlayer playback from bytes completed.")
                        currentStatusMessage = "Finished playing speech (Google)."
                        it.release() // Release after completion
                        mediaPlayer = null
                        tempFile?.delete() // Clean up temp file
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(tag, "MediaPlayer error from bytes: What: $what, Extra: $extra")
                        currentStatusMessage = "Error playing speech (Google)."
                        mp.release()
                        mediaPlayer = null
                        tempFile?.delete() // Clean up temp file
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error playing audio bytes: ${e.message}", e)
                errorMessage = "Error playing audio: ${e.message}"
                currentStatusMessage = "Playback error (Google)."
                tempFile?.delete()
            }
        }
    }


    private fun playAudioFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    prepareAsync()
                    setOnPreparedListener {
                        Log.d(tag, "MediaPlayer prepared, starting playback.")
                        start()
                        currentStatusMessage = "Playing translated speech."
                    }
                    setOnCompletionListener {
                        Log.d(tag, "MediaPlayer playback completed.")
                        currentStatusMessage = "Finished playing speech."
                        it.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(tag, "MediaPlayer error: What: $what, Extra: $extra")
                        currentStatusMessage = "Error playing speech."
                        mp.release()
                        mediaPlayer = null
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error setting up MediaPlayer: ${e.message}", e)
                errorMessage = "Error playing audio: ${e.message}"
                currentStatusMessage = "Playback error."
            }
        }
    }

    // Implementation for AudioFrameListener
    override fun onRemoteUserJoinedChannel(uid: Int) {
        Log.d(tag, "ViewModel notified that remote user $uid joined.")
        notifyRemoteUserJoined()
    }
}