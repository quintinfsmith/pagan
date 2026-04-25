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
    }

    private val quick_map = listOf(
        Triple(
            Global,
            listOf(AliasKey(KEYCODE_ESCAPE)),
            FunctionAlias.EscapeContext
        )
    )

    //    AliasKey(listOf(KEYCODE_ESCAPE), Global) to FunctionAlias.EscapeContext,
    //    AliasKey(listOf(KEYCODE_B), Global, true) to FunctionAlias.SelectColumn,
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

    operator fun get(context: Context, key_code: List<Int>, is_shift_pressed: Boolean, is_ctrl_pressed: Boolean): Pair<Boolean, FunctionAlias?> {
        return this.map[AliasKey(key_code, context, is_shift_pressed, is_ctrl_pressed)]
    }
}