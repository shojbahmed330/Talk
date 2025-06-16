package com.example.realtimecalltranslation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class IncomingCallActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_READ_CONTACTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        val tvCallerName = findViewById<TextView>(R.id.tvCallerName)
        val imgCaller = findViewById<ImageView>(R.id.imgCaller)
        val btnAnswer = findViewById<Button>(R.id.btnAnswer)
        val btnReject = findViewById<Button>(R.id.btnReject)

        val phoneNumber = intent.getStringExtra("CALLER_NUMBER") ?: ""

        // Check for contact permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_READ_CONTACTS
            )
        } else {
            val contactName = getContactName(phoneNumber, this)
            tvCallerName.text = contactName
        }

        // Optional: fallback image
        val callerImage = intent.getIntExtra("CALLER_IMAGE", R.drawable.ic_default_avatar)
        imgCaller.setImageResource(callerImage)

        btnAnswer.setOnClickListener {
            // TODO: Answer call logic here
            finish()
        }

        btnReject.setOnClickListener {
            // TODO: Reject call logic here
            finish()
        }
    }

    // Permission result callback
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_READ_CONTACTS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val phoneNumber = intent.getStringExtra("CALLER_NUMBER") ?: ""
            val contactName = getContactName(phoneNumber, this)
            findViewById<TextView>(R.id.tvCallerName).text = contactName
        }
    }

    private fun getContactName(phoneNumber: String, context: Context): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }

        return "Unknown Caller"
    }
}