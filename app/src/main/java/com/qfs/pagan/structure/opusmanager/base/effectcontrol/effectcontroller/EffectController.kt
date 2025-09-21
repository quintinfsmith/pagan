package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.ReducibleTreeArray
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.plus
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import com.qfs.pagan.structure.times

abstract class EffectController<T: EffectEvent>(beat_count: Int, var initial_event: T): ReducibleTreeArray<T>(MutableList(beat_count) { ReducibleTree() }) {
    var visible = false // I don't like this logic here, but the code is substantially cleaner with it hear than in the OpusLayerInterface
    fun set_initial_event(value: T) {
        this.initial_event = value
    }
    fun get_initial_event(): T {
        return this.initial_event
    }

    /**
     * Get the real latest event with a persistent transition OR create a theoretical
     * event with the transition of the latest event and the value of the latest persistent
     */
    fun coerce_latest_persistent_event(beat: Int, position: List<Int>): T {
        val (e_beat, e_position) = this.get_latest_event_position(beat, position) ?: return this.initial_event.copy() as T

        val latest_event = this.get_tree(e_beat, e_position).get_event()!!.copy()
        val pair = this.get_latest_persistent_position(beat, position)
        val output = if (pair == null) {
            this.initial_event.copy()
        } else {
            this.get_tree(pair.first, pair.second).get_event()!!.copy()
        }

        output.transition = latest_event.transition
        output.duration = 1

        return output as T
    }

    fun get_latest_persistent_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        var working_beat = beat
        var working_position = position
        var transition: EffectTransition? = null
        while (true) {
            val tree = this.get_tree(working_beat, working_position)

            if (tree.has_event() && tree.event!!.is_persistent()) {
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
            EffectTransition.Instant -> return working_event
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
            if (event.is_persistent()) {
                break
            }

            pair = this.get_preceding_event_position(pair.first, pair.second)
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
        data class StackItem(val position: List<Int>, val tree: ReducibleTree<T>?, val relative_width: Rational, val relative_offset: Rational)

        val initial_value = this.initial_event.to_float_array()
        val output = ControllerProfile(initial_value)
        var previous_tail = Pair(Rational(0,1), initial_value)

        for (beat_index in 0 until this.beat_count()) {
            val stack: MutableList<StackItem> = mutableListOf(
                StackItem(
                    position = listOf(),
                    tree = this.get_tree(beat_index),
                    relative_width = Rational(1,1),
                    relative_offset = Rational(0,1)
                )
            )

            while (stack.isNotEmpty()) {
                val working_item = stack.removeAt(0)
                val working_tree = working_item.tree ?: continue

                if (working_tree.has_event()) {
                    val working_event = working_tree.get_event()!!
                    val working_values = working_event.to_float_array()

                    if (working_values.contentEquals(previous_tail.second)) continue

                    val start_position = beat_index + working_item.relative_offset
                    val end_position = start_position + (working_event.duration * working_item.relative_width)

                    if (start_position > previous_tail.first) {
                        output.add(
                            ControllerProfile.ProfileEffectEvent(
                                start_position = previous_tail.first,
                                end_position = start_position,
                                start_value = previous_tail.second,
                                end_value = previous_tail.second,
                                transition = EffectTransition.Instant
                            )
                        )
                    }

                    if (!working_event.is_persistent()) {
                        output.add(
                            ControllerProfile.ProfileEffectEvent(
                                start_position = start_position,
                                end_position = start_position,
                                start_value = previous_tail.second,
                                end_value = working_values,
                                transition = EffectTransition.Instant
                            )
                        )
                        when (working_event.transition) {
                            EffectTransition.RLinear ->  {
                                output.add(
                                    ControllerProfile.ProfileEffectEvent(
                                        start_position = start_position,
                                        end_position = end_position,
                                        start_value = working_values,
                                        end_value = previous_tail.second,
                                        transition = EffectTransition.Linear
                                    )
                                )
                            }
                            EffectTransition.RInstant -> {
                                output.add(
                                    ControllerProfile.ProfileEffectEvent(
                                        start_position = end_position,
                                        end_position = end_position,
                                        start_value = working_values,
                                        end_value = previous_tail.second,
                                        transition = EffectTransition.Instant
                                    )
                                )
                            }
                            else -> {}
                        }
                        previous_tail = Pair(end_position, previous_tail.second)
                    } else {
                        output.add(
                            ControllerProfile.ProfileEffectEvent(
                                start_position = start_position,
                                end_position = end_position,
                                start_value = previous_tail.second,
                                end_value = working_values,
                                transition = working_event.transition
                            )
                        )
                        previous_tail = Pair(end_position, working_values)
                    }
                } else if (!working_tree.is_leaf()) {
                    val new_width = working_item.relative_width / working_tree.size
                    for (i in 0 until working_tree.size) {
                        val new_position = working_item.position.toMutableList()
                        new_position.add(i)
                        stack.add(
                            StackItem(
                                position = new_position,
                                tree = working_tree[i],
                                relative_width = new_width,
                                relative_offset = working_item.relative_offset + (new_width * i)
                            )
                        )
                    }
                }
            }
        }

        return output
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EffectController<T>) return false
        if (other.initial_event != this.initial_event) return false

        return super.equals(other)
    }
}
