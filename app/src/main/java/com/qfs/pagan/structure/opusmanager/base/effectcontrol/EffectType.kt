package com.qfs.pagan.structure.opusmanager.base.effectcontrol

enum class EffectType(val i: Int) {
    Tempo(0),
    Velocity(4),

    Volume(1),
    Delay(3),
    Pan(5),

    LowPass(1024 + 1),
    Reverb(1024 + 2)
 //   Equalizer(1024 + 3),
}
