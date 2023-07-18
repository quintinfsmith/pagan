package com.qfs.apres.soundfontplayer

import com.qfs.apres.SoundFont

class SoundFontPlayer(var sound_font: SoundFont) {

}

class SampleHandleMap(var sample_handles: List<SampleHandle>, var activity_map: List<Pair<Int, Set<Pair<Int, Boolean>>>>) { }
