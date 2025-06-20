package com.example.realtimecalltranslation.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

class AgoraManager(
    private val context: Context,
    private val appId: String,
    private val eventHandler: IRtcEngineEventHandler
) {
    private var rtcEngine: RtcEngine? = null
    private val tag = "AgoraManager"
    private var isEngineInitialized = false // Renamed for clarity

    fun isInitialized(): Boolean = isEngineInitialized // Public getter

    fun init() {
        try {
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = appId
            config.mEventHandler = eventHandler
            rtcEngine = RtcEngine.create(config)

            if (rtcEngine == null) {
                Log.e(tag, "RtcEngine.create returned null")
                throw RuntimeException("RtcEngine.create returned null")
            }

            rtcEngine?.enableAudio()
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
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
        if (rtcEngine == null) { // This check might be redundant if isEngineInitialized implies rtcEngine is not null
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

    fun destroy() {
        if (rtcEngine != null) {
            RtcEngine.destroy() // This internally calls leaveChannel if in a channel.
            rtcEngine = null
        }
        isEngineInitialized = false // Set before or after RtcEngine.destroy()
        Log.d(tag, "Agora RTC Engine destroyed. isEngineInitialized = false")
    }
}

// Basic handler (can be expanded or passed in from outside)
object DefaultRtcEngineEventHandler : IRtcEngineEventHandler() {
    private val tag = "AgoraEventHandler"

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.d(tag, "Successfully joined channel: $channel with uid: $uid")
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        Log.d(tag, "Successfully left channel. Stats: $stats")
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.d(tag, "User joined: $uid")
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(tag, "User offline: $uid, Reason: $reason")
    }

    override fun onError(err: Int) {
        Log.e(tag, "Agora RTC error: $err, message: ${RtcEngine.getErrorDescription(err)}")
    }
}