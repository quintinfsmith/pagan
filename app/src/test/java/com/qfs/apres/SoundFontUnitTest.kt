package com.qfs.apres

import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.soundfont.Instrument
import com.qfs.apres.soundfont.Preset
import com.qfs.apres.soundfont.SoundFont
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class SoundFontUnitTest {
    fun get_soundfont(): SoundFont {
        val sffont = "FluidR3_GM_GS.sf2"
        return SoundFont(sffont)
    }
    fun get_preset(): Preset {
        return this.get_soundfont().get_preset(0,0)
    }
    fun get_instrument(): Instrument {
        return this.get_preset().get_instruments(64, 64).first().instrument!!
    }
    @Test
    fun init_test() {
        //val midi = Midi()
        //for (i in 0 until 4) {
        //    midi.insert_event(0, i * 120, NoteOn(0, 64, 64))
        //    midi.insert_event(0, (i + 1) * 120, NoteOff(0, 64, 64))
        //}
        //val preset = soundfont.get_preset(0,0)
        //val instruments = preset.get_instruments(64,64)
        //val sample = instruments.first().instrument!!.get_samples(64,64)
    }
    @Test
    fun test_get_preset() {
        val some_presets = listOf<Pair<String, Pair<Int, Int>>>(
            Pair("Yamaha Grand Piano", Pair(0,0)),
            Pair("Bright Yamaha Grand", Pair(0,1)),
            Pair("Bird  2", Pair(3, 123)),
            Pair("Room", Pair(128, 8)),
            Pair("Standard", Pair(128, 0)),
            Pair("SFX", Pair(128, 56))
        )
        val soundfont = this.get_soundfont()

        for ((name, k) in some_presets) {
            val (program, bank) = k
            val test_preset = soundfont.get_preset(bank, program)
            assertEquals(
                "Didn't get correct Preset @ $bank, $program",
                name,
                test_preset.name
            )
        }

    }


    @Test
    fun test_get_instruments() {
        var soundfont = this.get_soundfont()
        var preset = soundfont.get_preset(0,0) // Yamaha Grand Piano
        val instruments = preset.get_instruments(0,0)
        assertEquals(
            "Didn't get Correct instrument set",
            instruments.size,
            1
        )
        var yamaha = instruments.first().instrument!!
        assertEquals(
            "Didn't get Correct instrument set",
            yamaha.name,
            "Yamaha Grand Piano"
        )

        val global_settings = yamaha.global_sample
        assertNotEquals(
            "Didn't load global sample on instrument",
            null,
            global_settings
        )

    }
    @Test
    fun test_vol_env_attack() {
        val instrument = this.get_instrument()
        val glob = instrument.global_sample!!
        assertEquals(
            "vol_env attack is wrong",
            .008,
            glob.vol_env_attack
        )
    }
    @Test
    fun test_vol_env_sustain() {
        val instrument = this.get_instrument()
        val glob = instrument.global_sample!!
        assertEquals(
            "vol_env_sustain is wrong",
            100.0,
            glob.vol_env_sustain
        )
    }
    @Test
    fun test_vol_env_release() {
        val instrument = this.get_instrument()
        val glob = instrument.global_sample!!
        assertEquals(
            "vol_env_release is wrong",
            1.0,
            glob.vol_env_release
        )
    }
    @Test
    fun test_mod_env_release() {
        val instrument = this.get_instrument()
        val glob = instrument.global_sample!!
        assertEquals(
            "mod_env_release is wrong",
            100.021,
            glob.mod_env_release
        )
    }

    @Test
    fun test_mod_env_filter() {
        val instrument = this.get_instrument()
        val glob = instrument.global_sample!!
        assertEquals(
            "mod_env_filter is wrong",
            -1000,
            glob.mod_env_filter
        )
    }

    @Test
    fun test_filter_cutoff() {
        val instrument = this.get_instrument()
        val glob = instrument.global_sample!!
        assertEquals(
            "Filter cutoff is wrong",
            11998,
            glob!!.filter_cutoff
        )
    }
}