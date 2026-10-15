package com.mini.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.mini.recorder.service.RecordingService

/**
 * Invisible activity launched from the home screen widget. Because widgets cannot
 * reliably start a microphone foreground service directly on Android 14+, we route
 * through this activity (a foreground context) which is allowed to start the FGS.
 */
class QuickRecordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            ContextCompat.startForegroundService(this, RecordingService.startIntent(this))
        } else {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
    }
}
