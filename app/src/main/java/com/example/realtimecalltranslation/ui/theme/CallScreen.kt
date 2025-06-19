package com.example.realtimecalltranslation.ui

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Added import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.ui.CallScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

data class Message(
    val fromUsa: Boolean,
    val original: String,
    val translated: String
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    callScreenViewModel: CallScreenViewModel, // Use ViewModel
    agoraManager: AgoraManager, // Kept for direct lifecycle management for now
    channel: String,
    token: String?,
    localIsUsa: Boolean,
    onCallEnd: () -> Unit,
    messages: List<Message>,
    mainRed: Color,
    mainWhite: Color
) {
    // Read state from ViewModel
    val isRecording = callScreenViewModel.isRecording
    val transcriptionStatus = callScreenViewModel.transcriptionStatus
    val transcribedText = callScreenViewModel.transcribedText
    val translatedText = callScreenViewModel.translatedText
    val errorMessage = callScreenViewModel.errorMessage

    // Permission state for RECORD_AUDIO - remains in Composable as it needs recomposition
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Agora Voice Call Logic & TTS/Recording cleanup - tied to Composable lifecycle
    LaunchedEffect(key1 = channel, key2 = token) {
        // Consider moving join/leave to ViewModel if agoraManager is also primarily managed by it
        agoraManager.joinChannel(channel, token, 0)
    }

    DisposableEffect(key1 = channel) {
        onDispose {
            agoraManager.leaveChannel()
            callScreenViewModel.stopOngoingTTS()
            callScreenViewModel.stopOngoingRecordingAndCleanup() // Cleanup if screen is disposed while recording
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("In Call: $channel", style = MaterialTheme.typography.titleLarge, color = mainRed)
        Spacer(Modifier.height(8.dp))

        Text("One-Way STT (Proof of Concept)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            if (!recordAudioPermissionState.status.isGranted) {
                recordAudioPermissionState.launchPermissionRequest()
            } else {
                callScreenViewModel.handleRecordAndTranscribePressed()
            }
        }) {
            Text(if (isRecording) "Stop Recording & Transcribe" else "Record & Transcribe")
        }
        Spacer(Modifier.height(8.dp))
        Text("Status: $transcriptionStatus")
        if (transcribedText.isNotEmpty()) {
            Text("Transcription: $transcribedText", style = MaterialTheme.typography.bodyMedium)
        }
        if (translatedText.isNotEmpty()) {
            Text("Translation (en->bn): $translatedText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        if (errorMessage != null) {
            Text("Error: $errorMessage", color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(Modifier.weight(0.1f)) 

        Column(
            modifier = Modifier
                .weight(0.9f) // Adjusted weight
                .fillMaxWidth()
        ) {
            messages.forEach { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = if (msg.fromUsa) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        horizontalAlignment =
                            if (msg.fromUsa) Alignment.End else Alignment.Start
                    ) {
                        val showText = if (localIsUsa) {
                            if (msg.fromUsa) msg.original else msg.translated
                        } else {
                            if (msg.fromUsa) msg.translated else msg.original
                        }
                        Surface(
                            color = if (msg.fromUsa) mainRed.copy(alpha = 0.11f) else mainWhite.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp), // This line requires the import
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                showText,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 16.sp,
                                color = if (msg.fromUsa) mainRed else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button( // End Call Button (existing)
            onClick = {
                // pollyHelper.stop() // This was from a previous version, now handled by ViewModel
                // if (isRecording) { 
                //    audioRecorderHelper.stopRecording()
                //    isRecording = false
                // }
                callScreenViewModel.stopOngoingTTS()
                callScreenViewModel.stopOngoingRecordingAndCleanup()
                onCallEnd()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = mainRed,
                contentColor = mainWhite
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Call")
        }
    }
}
