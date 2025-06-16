package com.example.realtimecalltranslation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.realtimecalltranslation.ui.theme.CallHistoryActivity

class OutgoingCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        val calleeNumber = intent.getStringExtra("CALLEE_NUMBER") ?: "Unknown Number"

        val tvCalleeName = findViewById<TextView>(R.id.tvCalleeName)
        val btnEndCall = findViewById<Button>(R.id.btnEndCall)

        tvCalleeName.text = calleeNumber

        btnEndCall.setOnClickListener {
            // Clear the back stack so user can't go back to call screen
            val intent = Intent(this, CallHistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}