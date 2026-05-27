package com.qfs.pagan
import android.view.KeyEvent.*
import com.qfs.pagan.KeyboardInputInterface.Context
import com.qfs.pagan.KeyboardInputInterface.Context.*
import com.qfs.pagan.KeyboardInputInterface.FunctionAlias

object KeyboardMap {
    data class AliasKey(val key_code: Int, val is_shift_pressed: Boolean = false, val is_ctrl_pressed: Boolean = false)
    class QuickMapEntry(val alias: FunctionAlias, val cursor_context: KeyboardInputInterface.Context, vararg _input_chain: AliasKey) {
        val input_chain: Array<AliasKey> = Array(_input_chain.size) { i -> _input_chain[i] }
    }

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
    private val quick_map: Array<QuickMapEntry> = arrayOf(
        QuickMapEntry(
            FunctionAlias.EscapeContext,
            Global,
            AliasKey(KEYCODE_ESCAPE),
        ),
        QuickMapEntry(
            FunctionAlias.SelectBeat,
            Global,
            AliasKey(KEYCODE_B),
        ),
        QuickMapEntry(
            FunctionAlias.SelectChannel,
            Global,
            AliasKey(KEYCODE_C, true),
        ),
        QuickMapEntry(
            FunctionAlias.SelectLine,
            Global,
            AliasKey(KEYCODE_L, true)
        ),
        QuickMapEntry(
            FunctionAlias.ZoomIn,
            Global,
            AliasKey(KEYCODE_EQUALS)
        ),
        QuickMapEntry(
            FunctionAlias.ZoomOut,
            Global,
            AliasKey(KEYCODE_MINUS)
        ),
        // ------------------ UNSET ---------------------//
        QuickMapEntry(
            FunctionAlias.SelectLineNext,
            Context.Unset,
            AliasKey(KEYCODE_J)
        ),
        QuickMapEntry(
            FunctionAlias.SelectBeatNext,
            Context.Unset,
            AliasKey(KEYCODE_L)
        ),
        // ------------------ LINE  ---------------------- //
        QuickMapEntry(
            FunctionAlias.SelectLineNext,
            Context.Line,
            AliasKey(KEYCODE_J)
        ),
        QuickMapEntry(
            FunctionAlias.SelectLinePrev,
            Context.Line,
            AliasKey(KEYCODE_K)
        ),
        QuickMapEntry(
            FunctionAlias.SelectFirstLineNextChannel,
            Context.Line,
            AliasKey(KEYCODE_J, true)
        ),
        QuickMapEntry(
            FunctionAlias.SelectFirstLinePrevChannel,
            Context.Line,
            AliasKey(KEYCODE_K, true)
        ),
        // ------------------ LEAF  ---------------------- //
        // ----------------- CHANNEL --------------------- //
        // ------------------ RANGE ---------------------- //
    )

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
        for (entry in this.quick_map) {
            val context = entry.cursor_context
            if (! this.search_trees.containsKey(context)) {
                this.search_trees[context] = SearchTree()
            }
            var top = this.search_trees[context]!!
            for (key in entry.input_chain) {
                if (! top.branches.containsKey(key)) {
                    top.branches[key] = SearchTree()
                }
                top = top.branches[key]!!
            }
            top.value = entry.alias
        }
    }

    operator fun get(context: Context, key_codes: List<AliasKey>): Pair<Boolean, FunctionAlias?> {
        this.search_trees[context]?.let { search_tree ->
            return search_tree.find(key_codes)
        }

        return Pair(false, null)
    }
}