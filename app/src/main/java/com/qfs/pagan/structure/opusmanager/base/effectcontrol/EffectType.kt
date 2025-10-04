package com.qfs.pagan.structure.opusmanager.base.effectcontrol
import com.qfs.apres.soundfontplayer.EffectType as ApresEffectType
enum class EffectType(val apres_type: ApresEffectType?) {
    Tempo(null),
    Velocity(null),

    Volume(ApresEffectType.Volume),
    LowPass(ApresEffectType.LowPass),
    Delay(ApresEffectType.Delay),
    Pan(ApresEffectType.Pan),

    Reverb(null)
 //   Equalizer(1024 + 3),
}
