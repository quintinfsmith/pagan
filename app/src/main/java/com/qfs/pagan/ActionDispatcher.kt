/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlin.math.max
import com.qfs.pagan.OpusLayerInterface as OpusManager

/**
 * Handle all (or as much of as possible) of the logic between a user action and the OpusManager.
 * Used to be used in order to record and playback actions for testing and debugging BUT is now a way to separate Compose Components from Activity
 */
class ActionDispatcher(val context: Context, var vm_controller: ViewModelEditorController) {
    class UnexpectedBranch: Exception()
    class MissingProjectManager: Exception()
    class IncompatibleEffectMerge(type_a: EffectType?, type_b: EffectType?): Exception("Can't merge types $type_a and $type_b")

    lateinit var vm_top: ViewModelPagan
    val persistent_number_input_values = HashMap<Int, Int>()

    fun attach_top_model(model: ViewModelPagan) {
        this.vm_top = model
    }

    fun new_project() {
        val opus_manager = this.get_opus_manager()
        opus_manager.project_change_new()

        for ((c, channel) in opus_manager.channels.withIndex()) {
            if (!opus_manager.is_percussion(c)) continue
            val i = this.vm_controller.audio_interface.get_minimum_instrument_index(channel.get_preset())
            for (l in 0 until opus_manager.get_channel(c).size) {
                opus_manager.percussion_set_instrument(c, l, max(0, i - 27))
            }
        }

        this.vm_controller.update_soundfont_instruments()
        opus_manager.clear_history()
    }

    fun hide_all_hidden_line_controller(effect_type: EffectType, all_channels: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        if (all_channels) {
            opus_manager.unset_all_line_controller_visibility(effect_type)
        } else {
            val cursor = opus_manager.cursor
            opus_manager.unset_all_line_controller_visibility(effect_type, cursor.channel)
        }
    }


    fun get_opus_manager(): OpusManager {
        return this.vm_controller.opus_manager
    }


}
