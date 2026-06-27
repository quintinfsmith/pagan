package com.qfs.pagan

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

object EffectResourceMap {
    data class EffectData(val icon: Int, val name: Int)
    private val hashmap = hashMapOf<EffectType, EffectData>(
        EffectType.Tempo to EffectData(R.drawable.icon_tempo, R.string.ctl_desc_tempo),
        EffectType.Delay to EffectData(R.drawable.icon_echo, R.string.ctl_desc_delay),
        EffectType.Volume to EffectData(R.drawable.icon_volume, R.string.ctl_desc_volume),
        EffectType.Pan to EffectData(R.drawable.icon_pan, R.string.ctl_desc_pan),
        EffectType.Velocity to EffectData(R.drawable.icon_velocity, R.string.ctl_desc_velocity)
        //     EffectType.LowPass -> TODO()
        //     EffectType.Reverb -> TODO()
        //     EffectType.Pitch -> TODO()
    )

    operator fun get(effect_type: EffectType): EffectData {
        return this.hashmap[effect_type]!!
    }
}