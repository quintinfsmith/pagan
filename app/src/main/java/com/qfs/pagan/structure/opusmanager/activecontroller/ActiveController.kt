package com.qfs.pagan.structure.opusmanager.activecontroller

import com.qfs.pagan.structure.opusmanager.ControlTransition
import com.qfs.pagan.structure.opusmanager.OpusControlEvent
import com.qfs.pagan.structure.opusmanager.OpusTreeArray
import com.qfs.pagan.structure.rationaltree.ReducibleTree

abstract class ActiveController<T: OpusControlEvent>(beat_count: Int, var initial_event: T): OpusTreeArray<T>(MutableList(beat_count) { ReducibleTree() }) {
    var visible = false // I don't like this logic here, but the code is substantially cleaner with it hear than in the OpusLayerInterface
    fun set_initial_event(value: T) {
        this.initial_event = value
    }
    fun get_initial_event(): T {
        return this.initial_event
    }
    fun generate_profile(): ControllerProfile {
        data class StackItem(val position: List<Int>, val tree: ReducibleTree<T>?, val relative_width: Float, val relative_offset: Float)

        var initial_value = this.initial_event.to_float()
        val output = ControllerProfile()
        output.add(0F, 0F, 0F, initial_value, ControlTransition.Instant)
        var previous_tail = Pair(0F, initial_value)


        val size = this.beat_count()
        val default_size = 1F / size.toFloat()
        for (b in 0 until size) {
            val stack: MutableList<StackItem> = mutableListOf(StackItem(listOf(), this.get_tree(b), default_size, 0F))
            while (stack.isNotEmpty()) {
                val working_item = stack.removeAt(0)
                val working_tree = working_item.tree ?: continue

                if (working_tree.has_event()) {
                    val working_event = working_tree.get_event()!!
                    val working_value = working_event.to_float()
                    val diff = working_value - previous_tail.second
                    if (diff == 0F) {
                        continue
                    }

                    val start_position = (b.toFloat() / size.toFloat()) + working_item.relative_offset
                    val end_position = start_position + (working_event.duration * working_item.relative_width)

                    if (start_position > previous_tail.first) {
                        output.add(previous_tail.first, start_position, previous_tail.second, previous_tail.second, ControlTransition.Instant)
                    }

                    output.add(start_position, end_position, previous_tail.second, working_value, working_event.transition)
                    previous_tail = Pair(end_position, working_value)
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
