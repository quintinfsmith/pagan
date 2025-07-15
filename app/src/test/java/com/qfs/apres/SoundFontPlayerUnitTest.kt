package com.qfs.apres

import android.media.AudioFormat
import android.media.AudioTrack
import com.qfs.apres.event2.NoteOn79
import com.qfs.apres.soundfont.SampleDirective
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleGenerator
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

// class SoundFontPlayerUnitTest {
//     init {
//         System.loadLibrary("pagan")
//     }
//     fun get_soundfont(): SoundFont {
//         val sffont = "FluidR3_GM_GS.sf2"
//         println(File(sffont).absolutePath)
//         return SoundFont(sffont)
//     }
//
//     @Test
//     fun test_generator() {
//         mockkStatic(AudioTrack::class) {
//             every { AudioTrack.getMinBufferSize(22050, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.CHANNEL_OUT_STEREO) } returns 1
//
//             val soundfont = this.get_soundfont()
//             val test_on = NoteOn79(
//                 channel = 0,
//                 note = 64,
//                 velocity = 64 shr 8
//             )
//             val preset = soundfont.get_preset(0,0)
//             val preset_instrument = preset.get_instruments(test_on.note, test_on.velocity shl 8).first()
//             val samples = preset_instrument.instrument!!.get_samples(test_on.note, test_on.velocity shl 8).toList()
//             var sample_handle_generator = SampleHandleGenerator(44100, 44100)
//
//             sample_handle_generator.get(
//                 test_on,
//                 samples.first(),
//                 preset_instrument,
//                 preset
//             )
//
//             assertEquals(
//                 "Sample Handle Generator didn't cache sample data",
//                 1,
//                 sample_handle_generator.sample_data_map.size
//             )
//             val size = sample_handle_generator.sample_data_map.size
//
//             sample_handle_generator.get(
//                 test_on,
//                 samples.first(),
//                 preset_instrument,
//                 preset
//             )
//
//             assertEquals(
//                 "Sample Handle Generator didn't cache sample data",
//                 size,
//                 sample_handle_generator.sample_data_map.size
//             )
//         }
//     }
// }
//