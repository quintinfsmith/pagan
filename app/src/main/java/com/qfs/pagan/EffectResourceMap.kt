package com.qfs.pagan

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

object EffectResourceMap {
    data class EffectData(val icon: Int, val name: Int, val test_tag: TestTag)
    private val hashmap = hashMapOf<EffectType, EffectData>(
        EffectType.Tempo to EffectData(R.drawable.icon_tempo, R.string.ctl_desc_tempo, TestTag.EffectMenuTempo),
        EffectType.Delay to EffectData(R.drawable.icon_echo, R.string.ctl_desc_delay, TestTag.EffectMenuDelay),
        EffectType.Volume to EffectData(R.drawable.icon_volume, R.string.ctl_desc_volume, TestTag.EffectMenuVolume),
        EffectType.Pan to EffectData(R.drawable.icon_pan, R.string.ctl_desc_pan, TestTag.EffectMenuPan),
        EffectType.Velocity to EffectData(R.drawable.icon_velocity, R.string.ctl_desc_velocity, TestTag.EffectMenuVelocity),
        EffectType.LowPass to EffectData(R.drawable.icon_lowpass, R.string.ctl_desc_lowpass, TestTag.EffectMenuLowPass),
        EffectType.HighPass to EffectData(R.drawable.icon_highpass, R.string.ctl_desc_highpass, TestTag.EffectMenuHighPass)
        //     EffectType.Reverb -> TODO()
        //     EffectType.Pitch -> TODO()
    )

    operator fun get(effect_type: EffectType): EffectData {
        return this.hashmap[effect_type]!!
    }
}