package com.qfs.apres

// class SoundFontUnitTest {
//     init {
//         System.loadLibrary("pagan")
//     }
//
//     fun get_soundfont(): SoundFont {
//         val sffont = "FluidR3_GM_GS.sf2"
//         return SoundFont(sffont)
//     }
//
//     fun get_preset(): Preset {
//         return this.get_soundfont().get_preset(0, 0)
//     }
//
//     fun get_instrument(): Instrument {
//         return this.get_preset().get_instruments(64, 64).first().instrument!!
//     }
//
//     fun get_instrument_sample(): SampleDirective {
//         val samples = this.get_instrument().get_samples(20, 64).toList()
//         return if (samples[0].sample!![0].name == "P200 Piano D2(L)") {
//             samples[0]
//         } else {
//             samples[1]
//         }
//     }
//
//     @Test
//     fun init_test() {
//         //val midi = Midi()
//         //for (i in 0 until 4) {
//         //    midi.insert_event(0, i * 120, NoteOn(0, 64, 64))
//         //    midi.insert_event(0, (i + 1) * 120, NoteOff(0, 64, 64))
//         //}
//         //val preset = soundfont.get_preset(0,0)
//         //val instruments = preset.get_instruments(64,64)
//         //val sample = instruments.first().instrument!!.get_samples(64,64)
//     }
//
//     @Test
//     fun test_get_preset() {
//         val some_presets = listOf<Pair<String, Pair<Int, Int>>>(
//             Pair("Yamaha Grand Piano", Pair(0, 0)),
//             Pair("Bright Yamaha Grand", Pair(0, 1)),
//             Pair("Bird  2", Pair(3, 123)),
//             Pair("Room", Pair(128, 8)),
//             Pair("Standard", Pair(128, 0)),
//             Pair("SFX", Pair(128, 56))
//         )
//         val soundfont = this.get_soundfont()
//
//         for ((name, k) in some_presets) {
//             val (program, bank) = k
//             val test_preset = soundfont.get_preset(bank, program)
//             assertEquals(
//                 "Didn't get correct Preset @ $bank, $program",
//                 name,
//                 test_preset.name
//             )
//         }
//
//     }
//
//
//     @Test
//     fun test_get_instruments() {
//         var soundfont = this.get_soundfont()
//         var preset = soundfont.get_preset(0, 0) // Yamaha Grand Piano
//         val instruments = preset.get_instruments(0, 0)
//         assertEquals(
//             "Didn't get Correct instrument set",
//             instruments.size,
//             1
//         )
//         var yamaha = instruments.first().instrument!!
//         assertEquals(
//             "Didn't get Correct instrument set",
//             yamaha.name,
//             "Yamaha Grand Piano"
//         )
//
//         val global_settings = yamaha.global_zone
//         assertNotEquals(
//             "Didn't load global sample on instrument",
//             null,
//             global_settings
//         )
//
//     }
//
//     @Test
//     fun test_vol_env_attack() {
//         val instrument = this.get_instrument()
//         val glob = instrument.global_zone
//         assertEquals(
//             "vol_env attack is wrong",
//             8,
//             (glob.vol_env_attack!! * 1000).roundToInt()
//         )
//     }
//
//     @Test
//     fun test_vol_env_sustain() {
//         val instrument = this.get_instrument()
//         val glob = instrument.global_zone
//         assertEquals(
//             "vol_env_sustain is wrong",
//             100f,
//             glob.vol_env_sustain
//         )
//     }
//
//     @Test
//     fun test_vol_env_release() {
//         val instrument = this.get_instrument()
//         val glob = instrument.global_zone
//         assertEquals(
//             "vol_env_release is wrong",
//             1f,
//             glob.vol_env_release
//         )
//     }
//
//     @Test
//     fun test_mod_env_release() {
//         val instrument = this.get_instrument()
//         val glob = instrument.global_zone
//         assertEquals(
//             "mod_env_release is wrong",
//             100021,
//             (glob.mod_env_release!! * 1000).toInt()
//         )
//     }
//
//     @Test
//     fun test_mod_env_filter() {
//         val instrument = this.get_instrument()
//         val glob = instrument.global_zone
//         assertEquals(
//             "mod_env_filter is wrong",
//             -1000,
//             glob.mod_env_filter
//         )
//     }
//
//     //@Test
//     //fun test_filter_cutoff() {
//     //    val instrument = this.get_instrument()
//     //    val glob = instrument.global_zone
//     //    assertEquals(
//     //        "Filter cutoff is wrong",
//     //        11998,
//     //        (2F.pow(glob.filter_cutoff!!.toFloat() / 1200F) * 8.176).toInt()
//     //    )
//     //}
//
//     @Test
//     fun test_attenuation() {
//         val preset = this.get_preset()
//         val glob = preset.global_zone
//
//         assertEquals(
//             "Attenuation is Wrong",
//             14f,
//             glob.attenuation
//         )
//     }
//
//     @Test
//     fun test_pan() {
//         val sample = this.get_instrument_sample()
//         assertEquals(
//             "Pan is wrong",
//             -50f,
//             sample.pan
//         )
//     }
//
//     @Test
//     fun test_root_key() {
//         val sample = this.get_instrument_sample()
//         assertEquals(
//             "Root Key is wrong",
//             26,
//             sample.root_key
//         )
//     }
//
//     @Test
//     fun test_vol_env_hold() {
//         val sample = this.get_instrument_sample()
//         assertEquals(
//             "vol_env_hold is wrong",
//             6000,
//             (sample.vol_env_hold!! * 1000).toInt()
//         )
//     }
//
//     @Test
//     fun test_vol_env_decay() {
//         val sample = this.get_instrument_sample()
//         assertEquals(
//             "vol_env_decay is wrong",
//             40998,
//             (sample.vol_env_decay!! * 1000).toInt()
//         )
//     }
//
//     @Test
//     fun test_sampleMode() {
//         val sample = this.get_instrument_sample()
//         assertEquals(
//             "sampleMode is wrong",
//             1,
//             sample.sampleMode
//         )
//     }
//
//     @Test
//     fun test_sample_rate() {
//         val sample = this.get_instrument_sample().sample!![0]
//         assertEquals(
//             "sample rate is wrong",
//             32000,
//             sample.sample_rate
//         )
//     }
//
//     @Test
//     fun test_sample_size() {
//         val sample = this.get_instrument_sample().sample!![0]
//         assertEquals(
//             "sample size is wrong",
//             219502,
//             sample.data!!.size
//         )
//     }
//
//     @Test
//     fun test_sample_type() {
//         val sample = this.get_instrument_sample().sample!![0]
//         assertEquals(
//             "sample type is wrong",
//             4, // Left
//             sample.sample_type
//         )
//     }
//
//     @Test
//     fun test_sample_link() {
//         val soundfont = this.get_soundfont()
//         val preset = soundfont.get_preset(124, 4)
//         val instrument = preset.get_instruments(20,64).first().instrument!!
//         val samples = instrument.get_samples(20, 64).toList()
//
//         val linked_sample = if (samples[0].sample!![0].name == "Scratchgs(R)") {
//             samples[1].sample!!
//         } else {
//             samples[0].sample!!
//         }[0]
//
//         val (compare_sample, _) = soundfont.get_sample(1444)
//         soundfont.apply_sample_data(compare_sample)
//
//         assertEquals(
//             "sample link is wrong",
//             compare_sample,
//             linked_sample
//         )
//     }
//
//     @Test
//     fun test_instrument_sample_scale_tuning() {
//         val preset = this.get_soundfont().get_preset(124, 4)
//         val instrument = preset.get_instruments(20,64).first().instrument!!
//         val samples = instrument.get_samples(20, 64).toList()
//
//         val instrument_sample = if (samples[0].sample!![0].name == "Scratchgs(R)") {
//             samples[0]
//         } else {
//             samples[1]
//         }
//
//         assertEquals(
//             "Scale Tuning is off",
//             20,
//             instrument_sample.scale_tuning
//         )
//     }
// }
//