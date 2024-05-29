package com.qfs.pagan

import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.structure.greatest_common_denominator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        assertEquals(tree_c[0].get_event(), setOf(0, 1))

    }

    // Series of bizarre behaviours i've found
    @Test
    fun test_merge_flatten_reduce_0() {

        val tree_aa = OpusTree<Int>()
        tree_aa.set_size(2)
        tree_aa[1].set_size(2)
        tree_aa[0].set_event(0)
        tree_aa[1][1].set_event(1)

        val tree_ab = OpusTree<Int>()
        tree_ab.set_size(2)
        tree_ab[0].set_size(2)
        tree_ab[0][1].set_event(2)

        val tree_ac = tree_aa.merge(tree_ab.get_set_tree())
        tree_ac.flatten()
        tree_ac.reduce()

        assertEquals(4, tree_ac.size)
        assertEquals(0, tree_ac[0].get_event()!!.first())
        assertEquals(2, tree_ac[1].get_event()!!.first())
        assertEquals(1, tree_ac[3].get_event()!!.first())
    }

    @Test
    fun test_merge_1() {
        val tree_aa = OpusTree<Int>()
        tree_aa.set_size(4)
        tree_aa[0].set_event(0)
        tree_aa[1].set_size(2)
        tree_aa[1][0].set_event(1)
        tree_aa[2].set_event(2)
        tree_aa[3].set_event(3)


        val tree_ab = OpusTree<Int>()
        tree_ab.set_size(1)

        val tree_ac = tree_aa.merge(tree_ab.get_set_tree())
        tree_ac.flatten()
        assertEquals(8, tree_ac.size)
        assertEquals(0, tree_ac[0].get_event()!!.first())
        assertEquals(1, tree_ac[2].get_event()!!.first())
        assertEquals(2, tree_ac[4].get_event()!!.first())
        assertEquals(3, tree_ac[6].get_event()!!.first())
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

        val tree_a = OpusTree<Int>()
        tree_a.set_size(8)
        tree_a[0].set_event(0)
        tree_a[2].set_event(1)
        tree_a[4].set_event(2)
        tree_a[6].set_event(3)
        tree_a.reduce()

        assertEquals(2, tree_a[0].size)
        assertEquals(2, tree_a[1].size)
        for (i in 0 until 2) {
            assertEquals(tree_a[0][i].get_event(), i)
            assertEquals(tree_a[1][i].get_event(), 2 + i)
        }
    }

    @Test
    fun test_clear_singles() {
        val tree = OpusTree<Int>()
        tree.set_size(1)
        tree[0].set_size(1)
        tree[0][0].set_size(4)
        tree.clear_singles()
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
