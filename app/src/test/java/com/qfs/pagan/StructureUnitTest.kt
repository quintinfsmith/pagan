package com.qfs.pagan

import com.qfs.pagan.structure.greatest_common_denominator
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import org.junit.Assert.assertEquals
import org.junit.Test

class StructureUnitTest {
    @Test
    fun test_swap_sections() {
        val test_list = mutableListOf("A0", "A1", "A2", "B0", "B1", "B2", "C0", "D0", "D1", "D2", "D3", "E0")
        val expected = mutableListOf("D0", "D1", "D2", "D3", "B0", "B1", "B2", "C0", "A0", "A1", "A2", "E0")
        test_list.swap_sections(0, 3, 7, 4)
        assertEquals(
            expected,
            test_list
        )
    }

    @Test
    fun test_merge() {
        val tree_a = ReducibleTree<Int>()
        tree_a.set_size(120)
        tree_a[80].set_size(40)
        tree_a[80][0].set_event(1)
        tree_a[0].set_event(1)

        val tree_b = ReducibleTree<Int>()
        tree_b.set_size(3)
        tree_b[0].set_size(3)
        tree_b[0][0].set_event(0)
        val tree_c: ReducibleTree<Set<Int>> = tree_a.merge(ReducibleTree.get_set_tree(tree_b))

        assertEquals(
            setOf(0, 1),
            tree_c[0].get_event()
        )

    }

    // Series of bizarre behaviours i've found
    @Test
    fun test_merge_flatten_reduce_0() {

        val tree_aa = ReducibleTree<Int>()
        tree_aa.set_size(2)
        tree_aa[1].set_size(2)
        tree_aa[0].set_event(0)
        tree_aa[1][1].set_event(1)

        val tree_ab = ReducibleTree<Int>()
        tree_ab.set_size(2)
        tree_ab[0].set_size(2)
        tree_ab[0][1].set_event(2)

        val tree_ac = tree_aa.merge(ReducibleTree.get_set_tree(tree_ab))
        tree_ac.flatten()
        tree_ac.reduce(4)

        assertEquals(4, tree_ac.size)
        assertEquals(0, tree_ac[0].get_event()!!.first())
        assertEquals(2, tree_ac[1].get_event()!!.first())
        assertEquals(1, tree_ac[3].get_event()!!.first())
    }

    @Test
    fun test_merge_1() {
        val tree_aa = ReducibleTree<Int>()
        tree_aa.set_size(4)
        tree_aa[0].set_event(0)
        tree_aa[1].set_size(2)
        tree_aa[1][0].set_event(1)
        tree_aa[2].set_event(2)
        tree_aa[3].set_event(3)

        val tree_ab = ReducibleTree<Int>()
        tree_ab.set_size(1)

        val tree_ac = tree_aa.merge(ReducibleTree.get_set_tree(tree_ab))
        tree_ac.flatten()
        assertEquals(8, tree_ac.size)
        assertEquals(0, tree_ac[0].get_event()!!.first())
        assertEquals(1, tree_ac[2].get_event()!!.first())
        assertEquals(2, tree_ac[4].get_event()!!.first())
        assertEquals(3, tree_ac[6].get_event()!!.first())
    }


    @Test
    fun test_reduce() {
        val tree = ReducibleTree<Int>()
        tree.set_size(120)
        tree[0].set_event(0)
        tree[30].set_event(1)
        tree[60].set_event(2)
        tree[90].set_event(3)
        tree.reduce(4)
        assertEquals(4, tree.size)

        val tree_a = ReducibleTree<Int>()
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
        val tree = ReducibleTree<Int>()
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
