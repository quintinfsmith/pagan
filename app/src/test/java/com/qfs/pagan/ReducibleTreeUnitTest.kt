package com.qfs.pagan

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducibleTreeUnitTest {
    @Test
    fun test_closest_position() {
        var test_tree = ReducibleTree<Int>()
        // Create tree with 3 branches with 3 branch with 3 branches (27 leaves)
        test_tree.resize(3)

        assertEquals(
            listOf(0),
            test_tree.get_closest_position(Rational(0,4))
        )
        assertEquals(
            listOf(0),
            test_tree.get_closest_position(Rational(1,4))
        )
        assertEquals(
            listOf(1),
            test_tree.get_closest_position(Rational(2,4))
        )
        assertEquals(
            listOf(2),
            test_tree.get_closest_position(Rational(3,4))
        )

        for (i in 0 until 3) {
            test_tree[i].resize(3)
        }

        assertEquals(
            listOf(0, 0),
            test_tree.get_closest_position(Rational(0,4))
        )
        assertEquals(
            listOf(0, 2),
            test_tree.get_closest_position(Rational(1,4))
        )
        assertEquals(
            listOf(1,1),
            test_tree.get_closest_position(Rational(2,4))
        )

        assertEquals(
            listOf(2, 0),
            test_tree.get_closest_position(Rational(3,4))
        )

        assertEquals(
            listOf(1, 1),
            test_tree.get_closest_position(Rational(1,2))
        )

        for (i in 0 until 3) {
            for (j in 0 until 3) {
                test_tree[i][j].resize(3)
            }
        }
        assertEquals(
            listOf(1, 1, 1),
            test_tree.get_closest_position(Rational(1,2))
        )
        assertEquals(
            listOf(0, 0, 0),
            test_tree.get_closest_position(Rational(0,4))
        )
        assertEquals(
            listOf(0, 2, 0),
            test_tree.get_closest_position(Rational(1,4))
        )
        assertEquals(
            listOf(1, 1, 1),
            test_tree.get_closest_position(Rational(2,4))
        )

        assertEquals(
            listOf(2, 0, 2),
            test_tree.get_closest_position(Rational(3,4))
        )
    }
}