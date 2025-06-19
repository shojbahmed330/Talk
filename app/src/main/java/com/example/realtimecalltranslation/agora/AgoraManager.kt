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
            Log.d(tag, "Agora RTC Engine initialized successfully.")

        } catch (e: Exception) {
            Log.e(tag, "Could not initialize Agora RTC Engine: ${e.message}")
            // Propagate or handle exception as needed
            throw e
        }
    }

    fun joinChannel(channelName: String, token: String?, uid: Int) {
        if (rtcEngine == null) {
            Log.e(tag, "RTC Engine not initialized before joining channel.")
            return
        }
        val options = ChannelMediaOptions()
        options.publishMicrophoneTrack = true // Assuming we want to publish audio
        options.autoSubscribeAudio = true   // Assuming we want to subscribe to remote audio

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

    fun destroy() {
        if (rtcEngine != null) {
            RtcEngine.destroy()
            rtcEngine = null
            Log.d(tag, "Agora RTC Engine destroyed.")
        }
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

    // You might want to add other handlers as needed, for example:
    // override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {}
    // override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {}
}
