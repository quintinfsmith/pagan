package com.qfs.apres.SoundFontPlayer

import com.qfs.apres.MIDI
import com.qfs.apres.NoteOff
import com.qfs.apres.NoteOn
import com.qfs.apres.Preset
import com.qfs.apres.SoundFont
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SoundFontPlayer(var sound_font: SoundFont) {

}

class SampleHandleMap(var sample_handles: List<SampleHandle>, var activity_map: List<Pair<Int, Set<Pair<Int, Boolean>>>>) { }
