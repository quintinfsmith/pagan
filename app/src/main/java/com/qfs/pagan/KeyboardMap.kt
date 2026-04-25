package com.qfs.pagan
import android.view.KeyEvent.*
import com.qfs.pagan.KeyboardInputInterface.Context
import com.qfs.pagan.KeyboardInputInterface.Context.*
import com.qfs.pagan.KeyboardInputInterface.FunctionAlias

object KeyboardMap {
    data class AliasKey(val key_code: Int, val is_shift_pressed: Boolean = false, val is_ctrl_pressed: Boolean = false)
    class SearchTree {
        val branches = hashMapOf<AliasKey, SearchTree>()
        var value: FunctionAlias? = null

        /**
         * find the FunctionAlias.
         */
        fun find(key_codes: List<AliasKey>): Pair<Boolean, FunctionAlias?> {
            return if (key_codes.isEmpty()) {
                Pair(true, this.value)
            } else if (this.branches.containsKey(key_codes[0])) {
                val sub_key_codes = key_codes.subList(1, key_codes.size)
                this.branches[key_codes[0]]!!.find(sub_key_codes)
            } else {
                Pair(false, null)
            }
        }
    }

    private val quick_map = listOf(
        Triple(
            Global,
            listOf(AliasKey(KEYCODE_ESCAPE)),
            FunctionAlias.EscapeContext
        ),
        Triple(
            Global,
            listOf(AliasKey(KEYCODE_B)),
            FunctionAlias.SelectColumn,
        )
    )

    //    AliasKey(listOf(KEYCODE_C), Global, true) to FunctionAlias.SelectChannel,
    //    AliasKey(listOf(KEYCODE_SPACE), Leaf) to FunctionAlias.LeafUnset,
    //    AliasKey(listOf(KEYCODE_A), Leaf) to FunctionAlias.LeafAdd,
    //    AliasKey(listOf(KEYCODE_S), Leaf) to FunctionAlias.LeafSplit,
    //    AliasKey(listOf(KEYCODE_X), Leaf) to FunctionAlias.LeafRemove,
    //    AliasKey(listOf(KEYCODE_LEFT_BRACKET), LeafStandard) to FunctionAlias.SetOctave,
    //    AliasKey(listOf(KEYCODE_RIGHT_BRACKET), LeafStandard) to FunctionAlias.SetOffset,
    //    AliasKey(listOf(KEYCODE_SPACE), LeafPercussion) to FunctionAlias.TogglePercussion,
    //)

    val search_trees = HashMap<Context, SearchTree>()
    init {
        for ((context, keys, alias) in this.quick_map) {
            if (! this.search_trees.containsKey(context)) {
                this.search_trees[context] = SearchTree()
            }
            var top = this.search_trees[context]!!
            for (key in keys) {
                if (! top.branches.containsKey(key)) {
                    top.branches[key] = SearchTree()
                }
                top = top.branches[key]!!
            }
            top.value = alias
        }

    }

    operator fun get(context: Context, key_codes: List<AliasKey>): Pair<Boolean, FunctionAlias?> {
        this.search_trees[context]?.let { search_tree ->
            return search_tree.find(key_codes)
        }

        return Pair(false, null)
    }
}