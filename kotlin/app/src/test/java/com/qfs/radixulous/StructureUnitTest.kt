package com.qfs.radixulous

import org.junit.Test
import org.junit.Assert.*
import com.qfs.radixulous.structure.*

class StructureUnitTest {
    @Test
    fun test_merge() {
        var tree_a = OpusTree<Int>()
        tree_a.set_size(120)
        tree_a.get(80).set_size(40)
        tree_a.get(80).get(0).set_event(1)
        tree_a.get(0).set_event(1)

        var tree_b = OpusTree<Int>()
        tree_b.set_size(3)
        tree_b.get(0).set_size(3)
        tree_b.get(0).get(0).set_event(0)

        var tree_c: OpusTree<Set<Int>> = tree_a.merge(tree_b.get_set_tree())

        assertEquals(tree_c.get(0).get_event(), setOf(0,1))
    }

    @Test
    fun test_reduce() {
        var tree = OpusTree<Int>()
        tree.set_size(120)
        tree.get(0).set_event(0)
        tree.get(30).set_event(1)
        tree.get(60).set_event(2)
        tree.get(90).set_event(3)
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
