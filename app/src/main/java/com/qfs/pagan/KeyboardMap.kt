package com.qfs.pagan
import android.util.Log
import android.view.KeyEvent.*
import com.qfs.pagan.KeyboardInputInterface.Context
import com.qfs.pagan.KeyboardInputInterface.Context.*
import com.qfs.pagan.KeyboardInputInterface.FunctionAlias

object KeyboardMap {
    data class AliasKey(val key_code: Int, val is_shift_pressed: Boolean = false, val is_ctrl_pressed: Boolean = false)
    class QuickMapEntry(val alias: FunctionAlias, val cursor_contexts: Array<KeyboardInputInterface.Context>, vararg _input_chain: AliasKey) {
        constructor(alias: FunctionAlias, cursor_context: KeyboardInputInterface.Context, vararg _input_chain: AliasKey): this(alias, arrayOf(cursor_context), *_input_chain)
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
        QuickMapEntry(
            FunctionAlias.ZoomInFull,
            Global,
            AliasKey(KEYCODE_EQUALS, true)
        ),
        QuickMapEntry(
            FunctionAlias.ZoomOutFull,
            Global,
            AliasKey(KEYCODE_MINUS, true)
        ),
        QuickMapEntry(
          FunctionAlias.Undo,
            Global,
            AliasKey(KEYCODE_U)
        ),
        QuickMapEntry(
            FunctionAlias.Redo,
            Global,
            AliasKey(KEYCODE_U, true)
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
        // ---------------- LINE/LEAF/CHANNEL -------------------//
        QuickMapEntry(
            FunctionAlias.AdjustOctaveUp,
            arrayOf(Context.LineStandard, Context.LeafStandard, Context.Channel),
            AliasKey(KEYCODE_O, true)
        ),
        QuickMapEntry(
            FunctionAlias.AdjustOctaveDown,
            arrayOf(Context.LineStandard, Context.LeafStandard, Context.Channel),
            AliasKey(KEYCODE_M, true)
        ),
        QuickMapEntry(
            FunctionAlias.AdjustOffsetUp,
            arrayOf(Context.LineStandard, Context.LeafStandard, Context.Channel),
            AliasKey(KEYCODE_O)
        ),
        QuickMapEntry(
            FunctionAlias.AdjustOffsetDown,
            arrayOf(Context.LineStandard, Context.LeafStandard, Context.Channel),
            AliasKey(KEYCODE_M)
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
        QuickMapEntry(
            FunctionAlias.LineInsertAfter,
            Context.Line,
            AliasKey(KEYCODE_N)
        ),
        QuickMapEntry(
            FunctionAlias.LineInsert,
            Context.Line,
            AliasKey(KEYCODE_LEFT_BRACKET),
            AliasKey(KEYCODE_N),
        ),
        QuickMapEntry(
            FunctionAlias.LineRemove,
            Context.Line,
            AliasKey(KEYCODE_X, true)
        ),
        QuickMapEntry(
            FunctionAlias.LineDuplicate,
            Context.Line,
            AliasKey(KEYCODE_D, true)
        ),
        QuickMapEntry(
            FunctionAlias.LineMuteToggle,
            Context.Line,
            AliasKey(KEYCODE_GRAVE)
        ),
        QuickMapEntry(
            FunctionAlias.LineSetVolume,
            Context.Line,
            AliasKey(KEYCODE_V, true)
        ),
        QuickMapEntry(
            FunctionAlias.LineMoveUp,
            Context.Line,
            AliasKey(KEYCODE_PERIOD, true)
        ),
        QuickMapEntry(
            FunctionAlias.LineMoveDown,
            Context.Line,
            AliasKey(KEYCODE_COMMA, true)
        ),
        QuickMapEntry(
            FunctionAlias.LineMoveTo,
            Context.Line,
            AliasKey(KEYCODE_PERIOD)
        ),
        QuickMapEntry(
            FunctionAlias.LineSetChannel,
            Context.Line,
            AliasKey(KEYCODE_COMMA)
        ),
        QuickMapEntry(
            FunctionAlias.LineSetPercussionInstrument,
            Context.LinePercussion,
            AliasKey(KEYCODE_I)
        ),
        // ------------------ LEAF  ---------------------- //
        QuickMapEntry(
            FunctionAlias.LeafUnset,
            Context.Leaf,
            AliasKey(KEYCODE_Z)
        ),
        QuickMapEntry(
            FunctionAlias.LeafSplit,
            Context.Leaf,
            AliasKey(KEYCODE_S)
        ),
        QuickMapEntry(
            FunctionAlias.LeafRemove,
            Context.Leaf,
            AliasKey(KEYCODE_X)
        ),
        QuickMapEntry(
            FunctionAlias.LeafAdd,
            Context.Leaf,
            AliasKey(KEYCODE_A)
        ),
        QuickMapEntry(
            FunctionAlias.LeafAddBefore,
            Context.Leaf,
            AliasKey(KEYCODE_LEFT_BRACKET),
            AliasKey(KEYCODE_A)
        ),
        QuickMapEntry(
            FunctionAlias.SelectLeafNext,
            Context.Leaf,
            AliasKey(KEYCODE_L)
        ),
        QuickMapEntry(
            FunctionAlias.SelectLeafPrevious,
            Context.Leaf,
            AliasKey(KEYCODE_H)
        ),
        QuickMapEntry(
            FunctionAlias.SelectLeafUp,
            Context.Leaf,
            AliasKey(KEYCODE_K)
        ),
        QuickMapEntry(
            FunctionAlias.SelectLeafDown,
            Context.Leaf,
            AliasKey(KEYCODE_J)
        ),
        QuickMapEntry(
            FunctionAlias.SetOffset,
            Context.LeafStandard,
            AliasKey(KEYCODE_PERIOD)
        ),
        QuickMapEntry(
            FunctionAlias.SetOctave,
            Context.LeafStandard,
            AliasKey(KEYCODE_COMMA)
        ),
        QuickMapEntry(
            FunctionAlias.LeafSetDuration,
            Context.Leaf,
            AliasKey(KEYCODE_D)
        ),
        // ----------------- CHANNEL --------------------- //
        QuickMapEntry(
            FunctionAlias.SelectChannelNext,
            Context.Channel,
            AliasKey(KEYCODE_J),
        ),
        QuickMapEntry(
            FunctionAlias.SelectChannelPrev,
            Context.Channel,
            AliasKey(KEYCODE_K),
        ),
        QuickMapEntry(
            FunctionAlias.ChannelRemove,
            Context.Channel,
            AliasKey(KEYCODE_X)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelInsertAfter,
            Context.Channel,
            AliasKey(KEYCODE_N)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelInsert,
            Context.Channel,
            AliasKey(KEYCODE_LEFT_BRACKET),
            AliasKey(KEYCODE_N),
        ),
        QuickMapEntry(
            FunctionAlias.ChannelKitInsertAfter,
            Context.Channel,
            AliasKey(KEYCODE_P)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelKitInsert,
            Context.Channel,
            AliasKey(KEYCODE_LEFT_BRACKET),
            AliasKey(KEYCODE_P),
        ),
        QuickMapEntry(
            FunctionAlias.ChannelDuplicate,
            Context.Channel,
            AliasKey(KEYCODE_D, true)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelMuteToggle,
            Context.Channel,
            AliasKey(KEYCODE_GRAVE)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelAppendLine,
            Context.Channel,
            AliasKey(KEYCODE_L)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelSetSoundfontIndex,
            Context.Channel,
            AliasKey(KEYCODE_T)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelSetMidiBank,
            Context.Channel,
            AliasKey(KEYCODE_Y)
        ),
        QuickMapEntry(
            FunctionAlias.ChannelSetMidiProgram,
            Context.Channel,
            AliasKey(KEYCODE_U)
        )
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
            for (context in entry.cursor_contexts) {
                if (!this.search_trees.containsKey(context)) {
                    this.search_trees[context] = SearchTree()
                }
                var top = this.search_trees[context]!!
                for (key in entry.input_chain) {
                    if (!top.branches.containsKey(key)) {
                        top.branches[key] = SearchTree()
                    }
                    top = top.branches[key]!!
                }
                top.value = entry.alias
            }
        }
    }

    operator fun get(context: Context, key_codes: List<AliasKey>): Pair<Boolean, FunctionAlias?> {
        this.search_trees[context]?.let { search_tree ->
            return search_tree.find(key_codes)
        }

        return Pair(false, null)
    }
}