package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.ReducibleTreeArray
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree

abstract class EffectController<T: EffectEvent>(beat_count: Int, var initial_event: T): ReducibleTreeArray<T>(MutableList(beat_count) { ReducibleTree() }) {
    var visible = false // I don't like this logic here, but the code is substantially cleaner with it hear than in the OpusLayerInterface
    fun set_initial_event(value: T) {
        this.initial_event = value
    }
    fun get_initial_event(): T {
        return this.initial_event
    }

    fun get_latest_non_reset_transition_event(beat: Int, position: List<Int>): T {
        val (e_beat, e_position) = this.get_latest_event_position(beat, position) ?: return this.initial_event.copy() as T

        val latest_event = this.get_tree(e_beat, e_position).get_event()!!.copy()
        val pair = this.get_latest_non_reset_transition_event_position(beat, position)
        val output = if (pair == null) {
            this.initial_event.copy()
        } else {
            this.get_tree(pair.first, pair.second).get_event()!!.copy()
        }

        output.transition = latest_event.transition
        output.duration = 1

        return output as T
    }
    fun get_latest_non_reset_transition_event_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        var working_beat = beat
        var working_position = position
        var transition: EffectTransition? = null
        while (true) {
            val tree = this.get_tree(working_beat, working_position)

            if (tree.has_event() && !tree.event!!.is_reset_transition()) {
                break
            }

            if (transition == null && tree.has_event()) {
                transition = tree.get_event()!!.transition
            }

            val tmp_pair = this.get_preceding_event_position(working_beat, working_position) ?: return null
            working_beat = tmp_pair.first
            working_position = tmp_pair.second
        }
        return Pair(working_beat, working_position)
    }

    fun coerce_event(beat: Int, target_position: Rational): T {
        val closest_position = this.get_tree(beat).get_closest_position(target_position)
        val (event_beat, event_position) = this.get_latest_event_position(beat, closest_position) ?: return this.initial_event

        val working_event = this.get_tree(event_beat, event_position).get_event()!!

        // If transition is instant, no need for further calculations
        when (working_event.transition) {
            EffectTransition.Instant -> {
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
            event_copy.transition = EffectTransition.Instant
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
        output.add(0F, 0F, floatArrayOf(0F), initial_value, EffectTransition.Instant)
        var previous_tail = Pair(0F, initial_value)

        val size = this.beat_count()
        val default_size = 1F
        for (b in 0 until size) {
            val stack: MutableList<StackItem> = mutableListOf(StackItem(listOf(), this.get_tree(b), default_size, 0F))
            while (stack.isNotEmpty()) {
                val working_item = stack.removeAt(0)
                val working_tree = working_item.tree ?: continue

                if (working_tree.has_event()) {
                    val working_event = working_tree.get_event()!!
                    val working_values = working_event.to_float_array()
                    var is_difference = false
                    for (i in 0 until working_values.size) {
                        if (working_values[i] - previous_tail.second[i] != 0F) {
                            is_difference = true
                            break
                        }
                    }

                    if (!is_difference) {
                        continue
                    }

                    val start_position = b.toFloat() + working_item.relative_offset
                    val end_position = start_position + (working_event.duration * working_item.relative_width)

                    if (start_position > previous_tail.first) {
                        output.add(previous_tail.first, start_position, previous_tail.second, previous_tail.second, EffectTransition.Instant)
                    }

                    if (working_event.is_reset_transition()) {
                        output.add(start_position, start_position, previous_tail.second, working_values, EffectTransition.Instant)
                        when (working_event.transition) {
                            EffectTransition.RLinear ->  {
                                output.add(start_position, end_position, working_values, previous_tail.second, EffectTransition.Linear)
                            }
                            EffectTransition.RInstant -> {
                                output.add(end_position, end_position, working_values, previous_tail.second, EffectTransition.Instant)
                            }
                            else -> {}
                        }
                        previous_tail = Pair(end_position, previous_tail.second)
                    } else {
                        output.add(
                            start_position,
                            end_position,
                            previous_tail.second,
                            working_values,
                            working_event.transition
                        )
                        previous_tail = Pair(end_position, working_values)
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

        return output
    }
}
