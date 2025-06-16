package com.example.realtimecalltranslation.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.*

class SpeechToTextHelper(
    val context: Context,
    val onResult: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        if (speechRecognizer == null)
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }
}

class TextToSpeechHelper(
    context: Context
) {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String, language: String = "en") {
        tts?.language = Locale(language)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.shutdown()
    }
}