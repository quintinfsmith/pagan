package com.qfs.pagan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PaganBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.qfs.pagan.CANCEL_EXPORT_WAV" -> {
                (p0!! as MainActivity).export_wav_cancel()
            }
            else -> {}
        }
    }
}
