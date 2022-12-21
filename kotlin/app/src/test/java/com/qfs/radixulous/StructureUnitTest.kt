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

        assertEquals(tree_c.get(0).is_event(), true)
        assertEquals(tree_c.get(0).get_event(), setOf(0,1))
    }
}
