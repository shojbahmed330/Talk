package com.example.realtimecalltranslation.util

object Constants {

    // Agora Credentials
    const val AGORA_APP_ID: String = "8af541a5096b4934be7e9361590ca509" // Updated App ID
    const val AGORA_CHANNEL_NAME: String = "jibon" // Keeping original channel name, can be changed if needed
    // Token: 007eJxTYGA2Ev2Vy2Qyt1tzbgabt7yz5kR+npcnHK12O/iFr1/Upq3AYJGYZmpimGhqYGmWZGJpbJKUap5qaWxmaGppkAwSfagekdEQyMhgbyTBwsgAgSC+EENyYk5OampARn5eql9pblJqkQIDAwBZSR8G
    // The older token you provided was: 007eJxTYGCIdbILkj20+sZtHuEzuSusNsQx3U+cMCdtgfCaIM4OoWAFBvMkoxTT1MQ0E2NDIxNTk5SkZDPDFAuzNANjM8NE0xSjD7zhGQ2BjAwHi98yMjJAIIjPypCVmZSfx8AAAKw8HcI=
    // Using the one associated with "Calling Agora" App ID.
    const val AGORA_TOKEN: String = "007eJxTYGA2Ev2Vy2Qyt1tzbgabt7yz5kR+npcnHK12O/iFr1/Upq3AYJGYZmpimGhqYGmWZGJpbJKUap5qaWxmaGppkAwSfagekdEQyMhgbyTBwsgAgSC+EENyYk5OampARn5eql9pblJqkQIDAwBZSR8G" // Updated Token


    // RapidAPI Credentials
    const val RAPID_API_KEY: String = "f33269e176mshd549050fd1e33c4p1c0aa7jsn5cab487ae563"

    // RapidAPI Hosts
    const val RAPID_API_HOST_SPEECH_TO_TEXT: String = "speech-to-text-ai.p.rapidapi.com"
    const val RAPID_API_HOST_TRANSLATION: String = "deep-translate1.p.rapidapi.com"
    const val RAPID_API_HOST_TEXT_TO_SPEECH: String = "realistic-text-to-speech.p.rapidapi.com"

    // RapidAPI URLs (base URLs, specific paths will be in service classes)
    const val RAPID_API_URL_SPEECH_TO_TEXT: String = "https://speech-to-text-ai.p.rapidapi.com/transcribe"
    const val RAPID_API_URL_TRANSLATION: String = "https://deep-translate1.p.rapidapi.com/language/translate/v2"
    const val RAPID_API_URL_TEXT_TO_SPEECH: String = "https://realistic-text-to-speech.p.rapidapi.com/v3/generate_voice_over_v2"

    // Firebase Realtime Database Paths
    const val FIREBASE_DB_USERS_NODE: String = "users" // To store user public info like name, phone, fcmToken
    const val FIREBASE_DB_PRESENCE_NODE: String = "presence" // To store online status and lastSeen
    const val FIREBASE_DB_CALL_REQUESTS_NODE: String = "call_requests" // To store active call requests

    // Firebase Presence Status values
    const val PRESENCE_STATUS_ONLINE: String = "online"
    const val PRESENCE_STATUS_OFFLINE: String = "offline"

    // Call Request Status values
    const val CALL_STATUS_PENDING: String = "pending"
    const val CALL_STATUS_ACCEPTED: String = "accepted"
    const val CALL_STATUS_REJECTED: String = "rejected"
    const val CALL_STATUS_MISSED: String = "missed" // Or unanswered
    const val CALL_STATUS_ONGOING: String = "ongoing" // After accepted
    const val CALL_STATUS_ENDED: String = "ended"
    const val CALL_STATUS_CANCELLED: String = "cancelled" // By caller before pickup
}
