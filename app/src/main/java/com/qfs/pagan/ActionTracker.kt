package com.qfs.pagan

import android.util.Log
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import kotlin.concurrent.thread
import com.qfs.pagan.OpusLayerInterface as OpusManager

/**
 * Handle all the logic between a user action and the OpusManager
 */
class ActionTracker {
    class NoActivityException: Exception()
    enum class TrackedAction {
        ClickLeafSTD,
        ClickLeafCTLLine,
        ClickLeafCTLChannel,
        ClickLeafCTLGlobal,
        ClickColumn,
        LongClickLeafSTD,
        LongClickLeafCTLLine,
        LongClickLeafCTLChannel,
        LongClickLeafCTLGlobal,
        ClickLineLabelSTD,
        ClickLineLabelCTLLine,
        ClickLineLabelCTLChannel,
        ClickLineLabelCTLGlobal,
    }

    var activity: MainActivity? = null
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int>>>()

    fun attach_activity(activity: MainActivity) {
        this.activity = activity
    }

    fun get_activity(): MainActivity {
        return this.activity ?: throw NoActivityException()
    }

    fun click_leaf_std(beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.ClickLeafSTD,
            listOf(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat
            ) + position
        )

        val context = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_level == null) {
            try {
                when (context.configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> {
                        opus_manager.copy_to_beat(beat_key)
                    }
                    PaganConfiguration.MoveMode.MOVE -> {
                        opus_manager.move_to_beat(beat_key)
                    }
                    PaganConfiguration.MoveMode.MERGE -> {
                        opus_manager.merge_into_beat(beat_key)
                    }
                }
                // Kludge, if the temporary blocker is set, assume the cursor has already changed
                if (opus_manager.temporary_blocker == null) {
                    opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                }
            } catch (e: Exception) {
                when (e) {
                    is OpusLayerBase.MixedInstrumentException -> {
                        opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                        context.feedback_msg(context.getString(R.string.feedback_mixed_link))
                    }
                    is OpusLayerBase.RangeOverflow -> {
                        opus_manager.cursor_select(beat_key, position)
                        context.feedback_msg(context.getString(R.string.feedback_bad_range))
                    }
                    is OpusLayerCursor.InvalidCursorState -> {
                        // Shouldn't ever actually be possible
                        throw e
                    }
                    is OpusLayerBase.InvalidMergeException -> {
                        opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else {
            opus_manager.cursor_select(beat_key, position)
            val tree = opus_manager.get_tree()

            thread {
                if (tree.is_event()) {
                    val note = if (opus_manager.is_percussion(beat_key.channel)) {
                        opus_manager.get_percussion_instrument(beat_key.line_offset)
                    } else {
                        opus_manager.get_absolute_value(beat_key, position) ?: return@thread
                    }
                    if (note >= 0) {
                        context.play_event(
                            beat_key.channel,
                            note,
                            (opus_manager.get_current_line_controller_event(ControlEventType.Volume, beat_key, position) as OpusVolumeEvent).value
                        )
                    }
                }
            }
        }
    }

    fun click_leaf_ctl_line(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.ClickLeafCTLLine,
            listOf(
                type.i,
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat
            ) + position
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> opus_manager.copy_line_ctl_to_beat(beat_key)
                    PaganConfiguration.MoveMode.MOVE -> opus_manager.move_line_ctl_to_beat(beat_key)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_line(type, beat_key, position)
                    }
                    else -> throw e
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_line(type, beat_key, position)
        }
    }

    fun click_leaf_ctl_channel(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.ClickLeafCTLChannel,
            listOf(
                type.i,
                channel,
                beat
            ) + position
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> opus_manager.copy_channel_ctl_to_beat(channel, beat)
                    PaganConfiguration.MoveMode.MOVE -> opus_manager.move_channel_ctl_to_beat(channel, beat)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_channel(type, channel, beat, position)
                    }
                    else -> throw e
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_channel(type, channel, beat, position)
        }

    }

    fun click_leaf_ctl_global(type: ControlEventType, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.ClickLeafCTLGlobal,
            listOf(
                type.i,
                beat
            ) + position
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> opus_manager.copy_global_ctl_to_beat(beat)
                    PaganConfiguration.MoveMode.MOVE -> opus_manager.move_global_ctl_to_beat(beat)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_global(type, beat, position)
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_global(type, beat, position)
        }
    }

    fun click_column(beat: Int) {
        this.track(
            TrackedAction.ClickColumn,
            listOf(beat)
        )

        this.get_opus_manager().cursor_select_column(beat)
    }

    fun long_click_leaf_std(beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.LongClickLeafSTD,
            listOf(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat
            ) + position
        )

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_level == null) {
            opus_manager.cursor_select_range(opus_manager.cursor.range!!.first, beat_key)
        } else {
            opus_manager.cursor_select_range(beat_key, beat_key)
        }
    }

    fun long_click_leaf_ctl_line(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.LongClickLeafCTLLine,
            listOf(
                type.i,
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat
            ) + position
        )

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Line && cursor.range!!.first.channel == beat_key.channel && cursor.range!!.first.line_offset == beat_key.line_offset && type == cursor.ctl_type) {
            opus_manager.cursor_select_line_ctl_range(type, cursor.range!!.first, beat_key)
        } else {
            opus_manager.cursor_select_line_ctl_range(type, beat_key, beat_key)
        }
    }

    fun long_click_leaf_ctl_channel(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.track(TrackedAction.LongClickLeafCTLChannel, listOf(type.i, channel, beat) + position)

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Channel && cursor.ctl_type == type) {
            // Currently, can't select multiple channels in a range
            if (channel == cursor.range!!.first.channel) {
                opus_manager.cursor_select_channel_ctl_range(type, channel, cursor.range!!.first.beat, beat)
            }
        } else {
            opus_manager.cursor_select_channel_ctl_range(type, channel, beat, beat)
        }
    }

    fun long_click_leaf_ctl_global(type: ControlEventType, beat: Int, position: List<Int>) {
       this.track(TrackedAction.LongClickLeafCTLGlobal, listOf(type.i, beat) + position)

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Global && cursor.ctl_type == type) {
            opus_manager.cursor_select_global_ctl_range(type, cursor.range!!.first.beat, beat)
        } else {
            opus_manager.cursor_select_global_ctl_range(type, beat, beat)
        }
    }

    private fun track(token: TrackedAction, args: List<Int>) {
        this.action_queue.add(Pair(token, args))
        Log.d("PaganTracker", "$token")
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }
}