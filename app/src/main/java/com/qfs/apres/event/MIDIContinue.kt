package com.qfs.apres.event

class MIDIContinue: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFB.toByte())
    }
}