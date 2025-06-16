package com.example.realtimecalltranslation.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.transcribe.AmazonTranscribeClient
import com.amazonaws.services.transcribe.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var outputFile: String? = null

    fun startRecording() {
        outputFile = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/recorded_audio_${UUID.randomUUID()}.mp4"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }

    fun stopRecording(): String? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile
    }
}

// Transcribe utility (simplified, assumes audio file already uploaded to S3)
class AmazonTranscribeHelper(
    private val accessKey: String,
    private val secretKey: String
) {
    private val client = AmazonTranscribeClient(
        BasicAWSCredentials(accessKey, secretKey)
    ).apply {
        setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1))
    }

    // audioFileUri: S3 URI, e.g. "s3://your-bucket/audio.mp4"
    suspend fun startTranscriptionJob(audioFileUri: String, languageCode: String = "en-US"): String? = withContext(Dispatchers.IO) {
        val jobName = "android-transcribe-job-" + UUID.randomUUID().toString().take(8)
        val request = StartTranscriptionJobRequest()
            .withTranscriptionJobName(jobName)
            .withLanguageCode(languageCode)
            .withMedia(Media().withMediaFileUri(audioFileUri))
            .withMediaFormat("mp4")
            .withOutputBucketName("your-output-bucket") // Must exist in your AWS account

        client.startTranscriptionJob(request)

        // Poll until job completes
        while (true) {
            val job = client.getTranscriptionJob(GetTranscriptionJobRequest().withTranscriptionJobName(jobName)).transcriptionJob
            if (job.transcriptionJobStatus == TranscriptionJobStatus.COMPLETED.toString()) {
                return@withContext job.transcript.transcriptFileUri // This is a URL to the transcript JSON
            }
            if (job.transcriptionJobStatus == TranscriptionJobStatus.FAILED.toString()) {
                throw Exception("Transcription failed")
            }
            Thread.sleep(2000)
        }
        null
    }
}