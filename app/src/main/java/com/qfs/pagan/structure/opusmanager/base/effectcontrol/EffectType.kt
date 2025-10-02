package com.qfs.pagan.structure.opusmanager.base.effectcontrol

enum class EffectType(val i: Int) {
    Tempo(0),
    Velocity(4),

    Volume(1),
    LowPass(2),
    Delay(3),
    Pan(5),

    Reverb(1024 + 2)
 //   Equalizer(1024 + 3),
}
