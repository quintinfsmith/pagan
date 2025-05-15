package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import java.time.Instant


class ControllerProfile() {
    val values = mutableListOf<Triple<Pair<Float, Float>, Pair<Float, Float>, ControlTransition>>()
    fun add(start: Float, end: Float, start_value: Float, end_value: Float, transition: ControlTransition) {
        this.values.add(
            Triple(
                Pair(start, end),
                Pair(start_value, end_value),
                transition
            )
        )
    }
}

abstract class ActiveController<T: OpusControlEvent>(beat_count: Int, var initial_event: T): OpusTreeArray<T>(MutableList(beat_count) { OpusTree() }) {
    var visible = false // I don't like this logic here, but the code is substantially cleaner with it hear than in the OpusLayerInterface
    fun set_initial_event(value: T) {
        this.initial_event = value
    }
    fun get_initial_event(): T {
        return this.initial_event
    }
    fun generate_profile(): ControllerProfile {
        data class StackItem(val position: List<Int>, val tree: OpusTree<T>?, val relative_width: Float, val relative_offset: Float)

        var working_value = this.initial_event.to_float()
        val output = ControllerProfile()

        val size = this.beat_count()
        val default_size = 1F / size.toFloat()
        for (b in 0 until size) {
            val stack: MutableList<StackItem> = mutableListOf(StackItem(listOf(), this.get_tree(b), default_size, 0F))
            while (stack.isNotEmpty()) {
                val working_item = stack.removeAt(0)
                val working_tree = working_item.tree ?: continue

                if (working_tree.is_event()) {
                    val working_event = working_tree.get_event()!!
                    val diff = working_event.to_float() - working_value
                    if (diff == 0F) {
                        continue
                    }

                    val start_position = (b.toFloat() / size.toFloat()) + working_item.relative_offset
                    val end_position = start_position + (working_event.duration * working_item.relative_width)

                    output.add(start_position, end_position, working_value, working_event.to_float(), working_event.transition)
                    working_value = working_event.to_float()

                    if (working_event.transition != ControlTransition.Instant) {
                        output.add(end_position, end_position, 0f, working_value, ControlTransition.Instant)
                    }

                } else if (!working_tree.is_leaf()) {
                    val new_width = working_item.relative_width / working_tree.size.toFloat()
                    for (i in 0 until working_tree.size) {
                        val new_position = working_item.position.toMutableList()
                        new_position.add(i)
                        stack.add(StackItem(new_position, working_tree[i], new_width, working_item.relative_offset + (new_width * i)))
                    }
                }
            }
        }

        if (output.values.isEmpty()) {
            output.add(0f, 0f, 0f, working_value, ControlTransition.Instant)
        }

        return output
    }
}

class VolumeController(beat_count: Int): ActiveController<OpusVolumeEvent>(beat_count, OpusVolumeEvent(.5F))
class TempoController(beat_count: Int): ActiveController<OpusTempoEvent>(beat_count, OpusTempoEvent(120F))
class ReverbController(beat_count: Int): ActiveController<OpusReverbEvent>(beat_count, OpusReverbEvent(1F))
class PanController(beat_count: Int): ActiveController<OpusPanEvent>(beat_count, OpusPanEvent(0F))