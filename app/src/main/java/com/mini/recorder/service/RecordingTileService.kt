package com.mini.recorder.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.mini.recorder.MainActivity
import com.mini.recorder.R

class RecordingTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_mic)
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val intent = RecordingService.actionIntent(this, RecordingService.ACTION_TOGGLE)
            runCatching { ContextCompat.startForegroundService(this, intent) }
        } else {
            val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(open)
        }
    }
}
