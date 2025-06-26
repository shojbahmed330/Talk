package com.example.realtimecalltranslation.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File
import java.util.*

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var outputFile: String? = null
        private set

    /**
     * Start recording audio from MIC and save to a temporary .mp4 file
     */
    fun startRecording() {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val fileName = "recorded_audio_${UUID.randomUUID()}.mp4"
        outputFile = File(outputDir, fileName).absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }

    /**
     * Stop the recording and return the output file path
     */
    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            recorder = null
            null
        }
    }

    /**
     * Optionally: Delete any previous recording (if needed)
     */
    fun cleanupLastRecording() {
        outputFile?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
        outputFile = null
    }
}
