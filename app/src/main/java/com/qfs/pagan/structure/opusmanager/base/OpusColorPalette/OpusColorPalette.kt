/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
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