package com.example.realtimecalltranslation.aws

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class S3Uploader(
    private val context: Context, // Context might be needed for TransferUtility or other AWS SDK features
    private val awsAccessKey: String,
    private val awsSecretKey: String,
    private val bucketName: String,
    private val region: Regions
) {
    private var s3Client: AmazonS3Client? = null
    private val tag = "S3Uploader"

    fun initS3Client() {
        try {
            val credentials = BasicAWSCredentials(awsAccessKey, awsSecretKey)
            s3Client = AmazonS3Client(credentials, com.amazonaws.regions.Region.getRegion(region))
            // s3Client?.setRegion(com.amazonaws.regions.Region.getRegion(region)) // Alternative way to set region
            Log.d(tag, "AmazonS3Client initialized successfully for region: $region")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing AmazonS3Client: ${e.message}", e)
            // Optionally rethrow or handle as per application's error handling strategy
        }
    }

    suspend fun uploadFileToS3(file: File, objectKey: String): String? {
        if (s3Client == null) {
            Log.e(tag, "S3 client not initialized. Call initS3Client() first.")
            return null
        }
        if (!file.exists()) {
            Log.e(tag, "File to upload does not exist: ${file.absolutePath}")
            return null
        }

        return withContext(Dispatchers.IO) { // Perform network operation on IO dispatcher
            try {
                // Using PutObjectRequest for direct upload
                // For more robust uploads (background, progress, multi-part), consider TransferUtility
                // Note: AWSMobileClient.getInstance().initialize(context) would be needed for TransferUtility's default setup.
                
                val putObjectRequest = PutObjectRequest(bucketName, objectKey, file)
                // Optional: Set public read access if needed, or manage permissions via bucket policy
                // putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead)
                
                s3Client?.putObject(putObjectRequest)
                
                val s3Uri = "s3://$bucketName/$objectKey"
                Log.d(tag, "Successfully uploaded ${file.name} to $s3Uri")
                s3Uri
            } catch (e: Exception) {
                Log.e(tag, "Error uploading file ${file.name} to S3: ${e.message}", e)
                null // Return null on failure
            }
        }
    }

    // Companion object for notes or constants if needed in future
    companion object {
        // Note: If using TransferUtility, AWSMobileClient.getInstance().initialize(context, callback)
        // typically needs to be called once during app startup (e.g., in MainActivity or Application class).
        // This initialization sets up the necessary AWSConfiguration.
        // Example:
        // AWSMobileClient.getInstance().initialize(context, object : Callback<UserStateDetails> {
        //     override fun onResult(result: UserStateDetails?) { Log.d("S3Uploader", "AWSMobileClient initialized.") }
        //     override fun onError(e: Exception?) { Log.e("S3Uploader", "AWSMobileClient initialization error.", e) }
        // })
    }
}
