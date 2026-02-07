/*
 * Apres, A Midi & Soundfont library
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.apres

import android.content.Context
import android.net.Uri
import com.qfs.apres.soundfont2.Riff

class WavFile {
    companion object {
        fun read(context: Context, uri: Uri) {
            Riff(context, uri).with { riff ->
                println("${riff.list_chunks}")
                println("${riff.sub_chunks}")
            }
        }
    }
}

    //if (!this.cancel_flagged) {
    //    // 00 Riff
    //    target_output_stream.write("RIFF".toByteArray(), 0, 4)
    //    // 04 File size
    //    target_output_stream.writeInt(Integer.reverseBytes(4 + 24 + 8 + data_byte_count))
    //    // 08 WAVE
    //    target_output_stream.writeBytes("WAVE")

    //    // 12 'fmt '
    //    target_output_stream.writeBytes("fmt ")
    //    // 16 chunk size (always 16)
    //    target_output_stream.writeInt(Integer.reverseBytes(16))
    //    // 20 (WAVE_FORMAT_PCM code == 1)
    //    target_output_stream.writeShort(0x0100)
    //    // 22 Channel Count
    //    target_output_stream.writeShort(0x0200)
    //    // 24 Sample rate
    //    target_output_stream.writeInt(Integer.reverseBytes(sample_rate))
    //    // 28 byte rate
    //    target_output_stream.writeInt(Integer.reverseBytes(sample_rate * 4))
    //    // 32 Block Alignment
    //    target_output_stream.writeByte(0x04)
    //    target_output_stream.writeByte(0x00)
    //    // 34 Bits per sample
    //    target_output_stream.writeByte(0x10)
    //    target_output_stream.writeByte(0x00)
    //    // 36 "data"
    //    target_output_stream.writeBytes("data")
    //    // 40 Chunk size
    //    target_output_stream.writeInt(Integer.reverseBytes(data_byte_count))
    //}