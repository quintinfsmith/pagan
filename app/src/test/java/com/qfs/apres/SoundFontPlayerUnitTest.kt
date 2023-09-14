package com.qfs.apres

import android.media.AudioFormat
import android.media.AudioTrack
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.soundfont.SoundFont
import com.qfs.apres.soundfontplayer.SampleHandleGenerator
import com.qfs.apres.soundfontplayer.SoundFontWavPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Test
import org.junit.Assert.*

class SoundFontPlayerUnitTest {
    fun get_soundfont(): SoundFont {
        val sffont = "FluidR3_GM_GS.sf2"
        return SoundFont(sffont)
    }
    @Test
    fun test_generator() {
        mockkStatic(AudioTrack::class) {
            every { AudioTrack.getMinBufferSize(22050, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.CHANNEL_OUT_STEREO) } returns 1

            val soundfont = this.get_soundfont()
            val test_on = NoteOn(0, 64,64)
            val preset = soundfont.get_preset(0,0)
            val preset_instrument = preset.get_instruments(test_on.note, test_on.velocity).first()
            val samples = preset_instrument.instrument!!.get_samples(test_on.note, test_on.velocity).toList()
            var sample_handle_generator = SampleHandleGenerator()
            sample_handle_generator.get(
                test_on,
                samples.first(),
                preset_instrument,
                preset
            )

            assertEquals(
                "Sample Handle Generator didn't cache sample data",
                1,
                sample_handle_generator.sample_data_map.size
            )
            val size = sample_handle_generator.sample_data_map.size

            sample_handle_generator.get(
                test_on,
                samples.first(),
                preset_instrument,
                preset
            )

            assertEquals(
                "Sample Handle Generator didn't cache sample data",
                size,
                sample_handle_generator.sample_data_map.size
            )
        }
    }
}