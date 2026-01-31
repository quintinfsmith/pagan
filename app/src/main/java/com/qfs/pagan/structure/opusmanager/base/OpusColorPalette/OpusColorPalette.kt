package com.qfs.pagan.structure.opusmanager.base.OpusColorPalette

import androidx.compose.ui.graphics.Color
import com.qfs.json.JSONHashMap

data class OpusColorPalette(
    var event: Color? = null,
    var event_bg: Color? = null,
    var effect: Color? = null,
    var effect_bg: Color? = null
) {
    enum class ColorToken {
        Event,
        EventBg,
        Effect,
        EffectBg
    }
    companion object {

        fun from_json(input: JSONHashMap): OpusColorPalette {
            return OpusColorPalette(
                event = input.get_stringn("event")?.toColor(),
                event_bg = input.get_stringn("event_bg")?.toColor(),
                effect = input.get_stringn("effect")?.toColor(),
                effect_bg = input.get_stringn("effect_bg")?.toColor()
            )
        }
    }

    fun to_json(): JSONHashMap {
        return JSONHashMap(
            "event" to this.event?.toHexString(),
            "event_bg" to this.event_bg?.toHexString(),
            "effect" to this.effect?.toHexString(),
            "effect_bg" to this.effect_bg?.toHexString()
        )
    }
}