package com.qfs.pagan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor

class PaganBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.qfs.pagan.CANCEL_EXPORT_WAV" -> {
                (p0!! as ComponentActivityEditor).export_wav_cancel()
            }
            else -> {}
        }
    }
}
