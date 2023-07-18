package com.qfs.apres.event

import com.qfs.apres.get_chord_name_from_mi_sf
import com.qfs.apres.get_mi_sf

data class KeySignature(var key: String): MIDIEvent {
    override fun as_bytes(): ByteArray {
        val misf = get_mi_sf(this.key)
        return byteArrayOf(
            0xFF.toByte(),
            0x59.toByte(),
            0x02.toByte(),
            misf.second,
            misf.first
        )
    }

    companion object {
        fun from_mi_sf(mi: Byte, sf: Byte): KeySignature {
            val chord_name = get_chord_name_from_mi_sf(mi, sf)
            return KeySignature(chord_name)
        }
    }

    fun get_key(): String {
        return this.key
    }

    fun set_key(key: String) {
        this.key = key
    }
}