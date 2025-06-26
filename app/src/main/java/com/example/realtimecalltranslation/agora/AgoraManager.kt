package com.example.realtimecalltranslation.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.IAudioFrameObserver
import io.agora.rtc2.audio.AudioParams
import java.nio.ByteBuffer

// Audio Frame Listener Interface
interface AudioFrameListener {
    fun onLocalAudioFrame(audioFrame: ByteArray)
    fun onRemoteAudioFrame(audioFrame: ByteArray, uid: Int)
    fun onRemoteUserJoinedChannel(uid: Int)
}

object AgoraManager : IRtcEngineEventHandler() {
    private var rtcEngine: RtcEngine? = null
    private const val tag = "AgoraManager"
    var audioListener: AudioFrameListener? = null // Changed from audioFrameListener to audioListener
        private set // Keep setter private
    private var isEngineInitialized = false
    private var appContext: Context? = null
    var currentAppId: String? = null
        private set

    private const val SAMPLE_RATE_HZ = 16000
    private const val NUM_CHANNELS = 1
    private const val SAMPLES_PER_CALL = 1024

    fun initialize(context: Context, appId: String) {
        if (isEngineInitialized && appContext != null && currentAppId == appId) {
            Log.d(tag, "AgoraManager already initialized with the same app ID.")
            return
        }
        if (isEngineInitialized) {
            destroy()
        }
        this.appContext = context.applicationContext
        this.currentAppId = appId
        init()
    }

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.d(tag, "Local user $uid successfully joined channel: $channel")
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        Log.d(tag, "Local user left channel. Stats: $stats")
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.d(tag, "Remote user $uid joined channel.")
        audioListener?.onRemoteUserJoinedChannel(uid) // Updated to audioListener
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(tag, "Remote user $uid offline. Reason: $reason")
    }

    override fun onError(err: Int) {
        Log.e(tag, "Agora RTC error: $err, message: ${RtcEngine.getErrorDescription(err)}")
    }

    private val iAudioFrameObserver = object : IAudioFrameObserver {
        override fun onRecordAudioFrame(
            channelId: String?,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsyncType: Int
        ): Boolean {
            buffer?.let {
                val data = ByteArray(it.remaining())
                it.get(data)
                Log.d(tag, "Local audio frame received, size: ${data.size}")
                audioListener?.onLocalAudioFrame(data) // Updated to audioListener
            }
            return true
        }

        override fun onPlaybackAudioFrame(
            channelId: String?,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsyncType: Int
        ): Boolean {
            buffer?.let {
                val data = ByteArray(it.remaining())
                it.get(data)
                audioListener?.onRemoteAudioFrame(data, 0) // Updated to audioListener
            }
            return true
        }

        override fun onPlaybackAudioFrameBeforeMixing(
            channelId: String,
            uid: Int,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer,
            renderTimeMs: Long,
            avsync_type: Int,
            rtpTimestamp: Int,
            presentationMs: Long
        ): Boolean {
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            Log.d(tag, "Remote audio frame received for uid: $uid, size: ${data.size}")
            audioListener?.onRemoteAudioFrame(data, uid) // Updated to audioListener
            return true
        }

        override fun getObservedAudioFramePosition(): Int {
            return 1 or 2 or 8
        }

        override fun getRecordAudioParams(): AudioParams {
            return AudioParams(SAMPLE_RATE_HZ, NUM_CHANNELS, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, SAMPLES_PER_CALL)
        }

        override fun getPlaybackAudioParams(): AudioParams {
            return AudioParams(SAMPLE_RATE_HZ, NUM_CHANNELS, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, SAMPLES_PER_CALL)
        }

        override fun getMixedAudioParams(): AudioParams {
            return AudioParams(SAMPLE_RATE_HZ, NUM_CHANNELS, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, SAMPLES_PER_CALL)
        }

        override fun getEarMonitoringAudioParams(): AudioParams? {
            return null
        }

        override fun onMixedAudioFrame(
            channelId: String?,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsyncType: Int
        ): Boolean {
            return true
        }

        override fun onEarMonitoringAudioFrame(
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsyncType: Int
        ): Boolean {
            return true
        }
    }

    private fun setupAudioFrameParameters() {
        rtcEngine?.let { engine ->
            engine.setRecordingAudioFrameParameters(
                SAMPLE_RATE_HZ,
                NUM_CHANNELS,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                SAMPLES_PER_CALL
            )
            engine.setPlaybackAudioFrameParameters(
                SAMPLE_RATE_HZ,
                NUM_CHANNELS,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                SAMPLES_PER_CALL
            )
            Log.d(tag, "Audio frame parameters set.")
        }
    }

    fun setAudioFrameListener(listener: AudioFrameListener?) {
        this.audioListener = listener // Updated to audioListener
        if (isInitialized() && rtcEngine != null) {
            if (listener != null) {
                setupAudioFrameParameters()
                val ret = rtcEngine?.registerAudioFrameObserver(iAudioFrameObserver)
                Log.d(tag, "AudioFrameObserver registered via setter: $ret")
            } else {
                rtcEngine?.registerAudioFrameObserver(null)
                Log.d(tag, "AudioFrameObserver unregistered via setter.")
            }
        }
    }

    fun isInitialized(): Boolean = isEngineInitialized && rtcEngine != null

    private fun init() {
        if (isEngineInitialized) {
            Log.d(tag, "Agora RTC Engine already initialized (internal check).")
            return
        }
        if (appContext == null || currentAppId == null) {
            Log.e(tag, "Context or AppId not set. Call AgoraManager.initialize(context, appId) first.")
            return
        }
        try {
            val config = RtcEngineConfig()
            config.mContext = appContext
            config.mAppId = currentAppId
            config.mEventHandler = this
            rtcEngine = RtcEngine.create(config)

            if (rtcEngine == null) {
                Log.e(tag, "RtcEngine.create returned null")
                throw RuntimeException("RtcEngine.create returned null")
            }

            rtcEngine?.enableAudio()
            rtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD, Constants.AUDIO_SCENARIO_DEFAULT)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

            val extAudioResult = rtcEngine?.setExternalAudioSource(
                true,
                SAMPLE_RATE_HZ,
                NUM_CHANNELS
            )
            Log.d(tag, "setExternalAudioSource result: $extAudioResult")

            if (audioListener != null) { // Updated to audioListener
                setupAudioFrameParameters()
                val ret = rtcEngine?.registerAudioFrameObserver(iAudioFrameObserver)
                Log.d(tag, "AudioFrameObserver registered during init: $ret")
            }

            isEngineInitialized = true
            Log.d(tag, "Agora RTC Engine initialization complete. isEngineInitialized = true")

        } catch (e: Exception) {
            isEngineInitialized = false
            Log.e(tag, "Could not initialize Agora RTC Engine: ${e.message}")
            throw e
        }
    }

    fun joinChannel(channelName: String, token: String?, uid: Int) {
        if (!isEngineInitialized) {
            Log.e(tag, "RTC Engine not initialized. Call init() successfully before joining channel.")
            return
        }
        if (rtcEngine == null) {
            Log.e(tag, "RTC Engine is null despite isEngineInitialized being true. This should not happen.")
            return
        }
        val options = ChannelMediaOptions()
        options.publishMicrophoneTrack = true
        options.autoSubscribeAudio = true

        val result = rtcEngine?.joinChannel(token, channelName, uid, options)
        if (result != Constants.ERR_OK) {
            Log.e(tag, "Failed to join channel: $result")
        } else {
            Log.d(tag, "Attempting to join channel: $channelName with uid: $uid")
        }
    }

    fun leaveChannel() {
        if (rtcEngine == null) {
            Log.e(tag, "RTC Engine not initialized or already destroyed.")
            return
        }
        val result = rtcEngine?.leaveChannel()
        if (result != Constants.ERR_OK) {
            Log.e(tag, "Failed to leave channel: $result")
        } else {
            Log.d(tag, "Channel left successfully.")
        }
    }

    fun muteLocalAudioStream(muted: Boolean) {
        if (rtcEngine == null) {
            Log.e(tag, "RTC Engine not initialized.")
            return
        }
        rtcEngine?.muteLocalAudioStream(muted)
        Log.d(tag, "Local audio muted: $muted")
    }

    fun setEnableSpeakerphone(enabled: Boolean) {
        if (rtcEngine == null) {
            Log.e(tag, "RTC Engine not initialized.")
            return
        }
        rtcEngine?.setEnableSpeakerphone(enabled)
        Log.d(tag, "Speakerphone enabled: $enabled")
    }

    fun sendCustomAudioBytes(audioBytes: ByteArray) {
        if (rtcEngine == null || !isInitialized()) {
            Log.e(tag, "RTC Engine not initialized or ready. Cannot push external audio frame.")
            return
        }
        if (audioBytes.isEmpty()) {
            Log.w(tag, "Attempted to send empty audio bytes.")
            return
        }

        val result = rtcEngine?.pushExternalAudioFrame(
            audioBytes,
            System.currentTimeMillis()
        )

        if (result == Constants.ERR_OK) {
            Log.d(tag, "Pushed external audio frame successfully, size: ${audioBytes.size}")
        } else {
            Log.e(tag, "Failed to push external audio frame. Error code: $result, Description: ${RtcEngine.getErrorDescription(result ?: -1)}")
        }
    }

    fun destroy() {
        if (rtcEngine != null) {
            RtcEngine.destroy()
            rtcEngine = null
        }
        isEngineInitialized = false
        Log.d(tag, "Agora RTC Engine destroyed. isEngineInitialized = false")
    }
}

object DefaultRtcEngineEventHandler : IRtcEngineEventHandler() {
    private val tag = "AgoraEventHandler"
    var audioListener: AudioFrameListener? = null // Updated to audioListener

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.d(tag, "Successfully joined channel: $channel with uid: $uid")
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        Log.d(tag, "Successfully left channel. Stats: $stats")
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.d(tag, "User joined: $uid")
        audioListener?.onRemoteUserJoinedChannel(uid) // Updated to audioListener
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(tag, "User offline: $uid, Reason: $reason")
    }

    override fun onError(err: Int) {
        Log.e(tag, "Agora RTC error: $err, message: ${RtcEngine.getErrorDescription(err)}")
    }
}