package com.qfs.pagan.structure.opusmanager.base.activecontroller

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.ControlTransition
import com.qfs.pagan.structure.opusmanager.base.OpusControlEvent
import com.qfs.pagan.structure.opusmanager.base.ReducibleTreeArray
import com.qfs.pagan.structure.rationaltree.ReducibleTree

abstract class ActiveController<T: OpusControlEvent>(beat_count: Int, var initial_event: T): ReducibleTreeArray<T>(MutableList(beat_count) { ReducibleTree() }) {
    var visible = false // I don't like this logic here, but the code is substantially cleaner with it hear than in the OpusLayerInterface
    fun set_initial_event(value: T) {
        this.initial_event = value
    }
    fun get_initial_event(): T {
        return this.initial_event
    }

    fun coerce_event(beat: Int, target_position: Rational): T {
        val closest_position = this.get_tree(beat).get_closest_position(target_position)
        val (event_beat, event_position) = this.get_latest_event_position(beat, closest_position) ?: return this.initial_event

        val working_event = this.get_tree(event_beat, event_position).get_event()!!

        // If transition is instant, no need for further calculations
        when (working_event.transition) {
            ControlTransition.Instant -> {
                return working_event
            }
            else -> {}
        }

        var working_tree = this.get_tree(event_beat)
        val event_rational = Rational(event_beat, 1)
        var width = 1
        for (p in event_position) {
            width *= working_tree.size
            event_rational.numerator *= working_tree.size
            event_rational.denominator *= working_tree.size
            event_rational.numerator += p

            working_tree = working_tree[p]
        }

        var event_end = event_rational.copy()
        event_end += Rational(working_event.duration, width)

        if (target_position + beat >= event_end) {
            val event_copy = working_event.copy()
            event_copy.transition = ControlTransition.Instant
            return event_copy as T
        }

        var pair = this.get_preceding_event_position(event_beat, event_position)
        while (pair != null) {
            val event = this.get_tree(pair.first, pair.second).get_event()!!
            if (!event.is_reset_transition()) {
                break
            }

            pair = this.get_preceding_event_position(event_beat, event_position)
        }

        val preceding_event = if (pair == null) {
            this.initial_event
        } else {
            this.get_tree(pair.first, pair.second).get_event()!!
        }

        return working_event.get_event_instant(
            ((target_position + beat) - event_rational) / ((event_end - beat) - (event_rational - beat)),
            preceding_event
        ) as T
    }

    fun generate_profile(): ControllerProfile {
        data class StackItem(val position: List<Int>, val tree: ReducibleTree<T>?, val relative_width: Float, val relative_offset: Float)

        val initial_value = this.initial_event.to_float_array()
        val output = ControllerProfile()
        output.add(0F, 0F, floatArrayOf(0F), initial_value, ControlTransition.Instant)
        var previous_tail = Pair(0F, initial_value)


        val size = this.beat_count()
        val default_size = 1F / size.toFloat()
        for (b in 0 until size) {
            val stack: MutableList<StackItem> = mutableListOf(StackItem(listOf(), this.get_tree(b), default_size, 0F))
            stack_traverse@ while (stack.isNotEmpty()) {
                val working_item = stack.removeAt(0)
                val working_tree = working_item.tree ?: continue

                if (working_tree.has_event()) {
                    val working_event = working_tree.get_event()!!
                    val working_values = working_event.to_float_array()
                    for (i in 0 until working_values.size) {
                        if (working_values[i] - previous_tail.second[i] != 0F) {
                            continue@stack_traverse
                        }
                    }

                    val start_position = (b.toFloat() / size.toFloat()) + working_item.relative_offset
                    val end_position = start_position + (working_event.duration * working_item.relative_width)

                    if (start_position > previous_tail.first) {
                        output.add(previous_tail.first, start_position, previous_tail.second, previous_tail.second, ControlTransition.Instant)
                    }

                    output.add(start_position, end_position, previous_tail.second, working_values, working_event.transition)
                    previous_tail = Pair(end_position, working_values)
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

        return output
    }
}
