package com.qfs.pagan

import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.structure.greatest_common_denominator
import org.junit.Assert.assertEquals
import org.junit.Test

class StructureUnitTest {
    @Test
    fun test_merge() {
        val tree_a = OpusTree<Int>()
        tree_a.set_size(120)
        tree_a[80].set_size(40)
        tree_a[80][0].set_event(1)
        tree_a[0].set_event(1)

        val tree_b = OpusTree<Int>()
        tree_b.set_size(3)
        tree_b[0].set_size(3)
        tree_b[0][0].set_event(0)

        val tree_c: OpusTree<Set<Int>> = tree_a.merge(tree_b.get_set_tree())

        assertEquals(tree_c[0].get_event(), setOf(0,1))
    }

    @Test
    fun test_reduce() {
        val tree = OpusTree<Int>()
        tree.set_size(120)
        tree[0].set_event(0)
        tree[30].set_event(1)
        tree[60].set_event(2)
        tree[90].set_event(3)
        tree.reduce(4)
        assertEquals(4, tree.size)
    }

    @Test
    fun test_gcd() {
        assertEquals(
            30,
            greatest_common_denominator(30, 60)
        )
    }
}
