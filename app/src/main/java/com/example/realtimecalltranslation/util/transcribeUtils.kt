package com.example.realtimecalltranslation.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.transcribe.AmazonTranscribeClient
import com.amazonaws.services.transcribe.model.*
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import kotlinx.coroutines.withContext
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
    private val secretKey: String,
    private val outputBucketName: String, // Added outputBucketName
    private val awsRegion: Regions // Added awsRegion for S3 client consistency
) {
    private val transcribeClient = AmazonTranscribeClient(
        BasicAWSCredentials(accessKey, secretKey)
    ).apply {
        setRegion(com.amazonaws.regions.Region.getRegion(awsRegion)) // Use provided region
    }

    // S3 client for downloading transcription results
    private val s3Client = AmazonS3Client(
        BasicAWSCredentials(accessKey, secretKey)
    ).apply {
        setRegion(com.amazonaws.regions.Region.getRegion(awsRegion)) // Use provided region
    }

    private suspend fun getTranscriptionResult(transcriptFileUri: String): String? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("TranscribeHelper", "Transcript file URI: $transcriptFileUri")
            // Example URI: s3://your-output-bucket/job-name.json or https://s3.region.amazonaws.com/bucket/key
            val uri = java.net.URI(transcriptFileUri)
            val bucket: String
            val key: String

            if (uri.scheme == "s3") {
                bucket = uri.host
                key = uri.path.removePrefix("/")
            } else if (uri.scheme == "https" && uri.host.startsWith("s3")) {
                // s3.region.amazonaws.com/bucket/key
                // or bucket.s3.region.amazonaws.com/key
                val pathSegments = uri.path.removePrefix("/").split("/")
                if (uri.host == "$outputBucketName.s3.${awsRegion.getName()}.amazonaws.com" || uri.host == "$outputBucketName.s3.amazonaws.com" ) { // Path style or virtual host style
                    bucket = outputBucketName
                    key = pathSegments.joinToString("/")
                } else if (pathSegments.size > 1 && uri.host.contains("s3")) { // Assuming path style s3.region.amazonaws.com/bucket/key
                    bucket = pathSegments[0]
                    key = pathSegments.subList(1, pathSegments.size).joinToString("/")
                }
                else {
                    android.util.Log.e("TranscribeHelper", "Cannot parse S3 URI (HTTPS): $transcriptFileUri")
                    return@withContext null
                }
            } else {
                android.util.Log.e("TranscribeHelper", "Unsupported URI scheme: $transcriptFileUri")
                return@withContext null
            }

            android.util.Log.d("TranscribeHelper", "Fetching transcript from bucket: $bucket, key: $key")
            if (bucket != outputBucketName) {
                android.util.Log.w("TranscribeHelper", "Transcript bucket ($bucket) differs from configured output bucket ($outputBucketName). This might be an issue.")
            }

            val s3Object = s3Client.getObject(bucket, key)
            val jsonContent = s3Object.objectContent.bufferedReader().use { it.readText() }
            android.util.Log.d("TranscribeHelper", "Transcript JSON content: $jsonContent")

            val json = JSONObject(jsonContent)
            val transcript = json.getJSONObject("results")
                .getJSONArray("transcripts")
                .getJSONObject(0)
                .getString("transcript")
            transcript
        } catch (e: Exception) {
            android.util.Log.e("TranscribeHelper", "Error getting transcription result: ${e.message}", e)
            null
        }
    }

    // audioFileUri: S3 URI, e.g. "s3://your-bucket/audio.mp4"
    suspend fun startTranscriptionJob(audioFileUri: String, languageCode: String = "en-US"): String? = withContext(Dispatchers.IO) {
        val jobName = "android-transcribe-job-" + UUID.randomUUID().toString().take(8)
        val media = Media().withMediaFileUri(audioFileUri)

        val request = StartTranscriptionJobRequest()
            .withTranscriptionJobName(jobName)
            .withLanguageCode(languageCode)
            .withMedia(media)
            // .withMediaFormat("mp4") // MediaFormat can often be inferred by Transcribe. If issues, specify.
            .withOutputBucketName(outputBucketName) // Use the constructor parameter

        transcribeClient.startTranscriptionJob(request)
        android.util.Log.d("TranscribeHelper", "Transcription job $jobName started for $audioFileUri. Output to $outputBucketName")

        // Poll until job completes
        while (true) {
            try {
                Thread.sleep(5000) // Polling interval
                val jobResult = transcribeClient.getTranscriptionJob(GetTranscriptionJobRequest().withTranscriptionJobName(jobName))
                val job = jobResult.transcriptionJob
                android.util.Log.d("TranscribeHelper", "Job $jobName status: ${job.transcriptionJobStatus}")

                when (TranscriptionJobStatus.fromValue(job.transcriptionJobStatus)) {
                    TranscriptionJobStatus.COMPLETED -> {
                        val transcriptFileUri = job.transcript?.transcriptFileUri
                        return@withContext if (transcriptFileUri != null) {
                            getTranscriptionResult(transcriptFileUri)
                        } else {
                            android.util.Log.e("TranscribeHelper", "Transcription completed but transcriptFileUri is null for job $jobName")
                            null
                        }
                    }
                    TranscriptionJobStatus.FAILED -> {
                        val failureReason = job.failureReason
                        android.util.Log.e("TranscribeHelper", "Transcription job $jobName failed: $failureReason")
                        // For now, returning null to align with String? and let ViewModel handle 'no result'
                        return@withContext null
                    }
                    else -> { // IN_PROGRESS, QUEUED, etc.
                        // Do nothing here, the loop will continue to the next iteration (Thread.sleep)
                        // This ensures continuous polling.
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TranscribeHelper", "Exception during polling/processing for job $jobName: ${e.message}", e)
                return@withContext null // Exit if polling itself fails (e.g., network issue during getTranscriptionJob)
            }
        }
        // Unreachable code, but needed for compiler if it can't infer exit via while(true) + returns in all paths.
        // However, with proper returns in COMPLETED, FAILED, and catch, this line should ideally not be necessary.
        // If a compiler error about missing return for String? occurs, this might be a point of investigation.
        // For now, assuming the returns within the loop cover all logical exits.

        // Explicit return statement for the withContext block,
        // even if logically unreachable, to satisfy compiler type inference if it's struggling.
        return@withContext null
    }
}