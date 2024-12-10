package com.qfs.pagan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyBroadcastReceiver: BroadcastReceiver() {
    init {
        println("init rcvr------------------------------------")
    }
    override fun onReceive(p0: Context?, intent: Intent?) {
        println("!!! ${intent?.action}")
        when (intent?.action) {
            "com.qfs.pagan.CANCEL_EXPORT_WAV" -> {
                (p0!! as MainActivity).export_wav_cancel()
            }
            else -> {}
        }
    }
}
