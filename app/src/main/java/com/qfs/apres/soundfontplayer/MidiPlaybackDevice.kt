package com.qfs.apres.soundfontplayer

import com.qfs.apres.soundfont.SoundFont

class MidiPlaybackDevice(soundfont: SoundFont, sample_rate: Int = 44100): MappedPlaybackDevice(
    SampleHandleManager(soundfont, sample_rate),
    MidiFrameMap()
) {


}