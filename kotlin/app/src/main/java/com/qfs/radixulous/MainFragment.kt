package com.qfs.radixulous

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.qfs.radixulous.databinding.FragmentMainBinding
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlin.math.abs

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private var active_context_menu_index: ContextMenu = ContextMenu.None
    private var ticking: Boolean = false // Lock to prevent multiple attempts at updating from happening at once

    private var focus_row = false
    private var focus_column = false
    private var linking_beat: BeatKey? = null
    private var linking_beat_b: BeatKey? = null
    private var relative_mode: Boolean = false
    private var is_loaded: Boolean = false

    private var _binding: FragmentMainBinding? = null
    private var cache = ViewCache()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hsvTable: HorizontalScrollView = view.findViewById(R.id.hsvTable)
        val svTable: ScrollView = view.findViewById(R.id.svTable)
        val hsvColumnLabels: HorizontalScrollView = view.findViewById(R.id.hsvColumnLabels)
        val svLineLabels: ScrollView = view.findViewById(R.id.svLineLabels)

        hsvTable.viewTreeObserver.addOnScrollChangedListener {
            hsvColumnLabels.scrollX = hsvTable.scrollX
        }
        svTable.viewTreeObserver.addOnScrollChangedListener {
            svLineLabels.scrollY = svTable.scrollY
        }
        hsvColumnLabels.viewTreeObserver.addOnScrollChangedListener {
            hsvColumnLabels.scrollX = hsvTable.scrollX
        }
        svLineLabels.viewTreeObserver.addOnScrollChangedListener {
            svLineLabels.scrollY = svTable.scrollY
        }

        val btnChannelCtrl: TextView = view.findViewById(R.id.btnChannelCtrl)
        btnChannelCtrl.setOnClickListener{
            findNavController().navigate(R.id.action_MainFragment_to_ConfigFragment)
        }

        setFragmentResultListener("LOAD") { _, bundle: Bundle? ->
            var main = this.getMain()
            bundle!!.getString("PATH")?.let { path: String ->
                this.takedownCurrent()
                main.getOpusManager().load(path)
                main.update_title_text()

                this.setContextMenu(ContextMenu.Leaf)
                this.tick()
            }
            main.update_menu_options()
        }

        setFragmentResultListener("RETURNED") { _, bundle: Bundle? ->
            var main = this.getMain()
            this.takedownCurrent()
            main.getOpusManager().reflag()
            main.update_title_text()

            this.setContextMenu(ContextMenu.Leaf)
            this.tick()
            main.update_menu_options()
        }

        //binding.buttonFirst.setOnClickListener {
        //    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        //}
    }

    override fun onStart() {
        super.onStart()

        var main = this.getMain()
        if (!this.is_loaded) {
            main.newProject()
            main.update_title_text()
            this.setContextMenu(ContextMenu.Leaf)
            this.tick()

            this.is_loaded = true
        }

    }

    //override fun onResume() {
    //    super.onResume()
    //}
    //override fun onStop() {
    //    super.onStop()
    //}

    //override fun onSaveInstanceState(savedInstanceState: Bundle) {
    //    super.onSaveInstanceState(savedInstanceState)
    //}
    //override fun onViewStateRestored(savedInstanceState: Bundle?) {
    //    super.onViewStateRestored(savedInstanceState)
    //}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getMain(): MainActivity {
        return this.activity!! as MainActivity
    }


    fun undo() {
        var opus_manager = this.getMain().getOpusManager()
        if (opus_manager.has_history()) {
            opus_manager.apply_undo()
            this.tick()
            this.setContextMenu(ContextMenu.Leaf)
        } else {
            Toast.makeText(activity?.applicationContext, getString(R.string.msg_undo_none), Toast.LENGTH_SHORT).show()
        }
    }

    fun takedownCurrent() {
        this.setContextMenu(ContextMenu.None)
        if (this.activity != null) {
            (this.activity!!.findViewById(R.id.tlOpusLines) as ViewGroup).removeAllViews()
            (this.activity!!.findViewById(R.id.llLineLabels) as ViewGroup).removeAllViews()
            (this.activity!!.findViewById(R.id.llColumnLabels) as ViewGroup).removeAllViews()
        }
        this.cache = ViewCache()
    }


    private fun newColumnLabel() {
        var opus_manager = this.getMain().getOpusManager()
        val parent: ViewGroup = this.activity!!.findViewById(R.id.llColumnLabels)
        val headerCellView = LayoutInflater.from(parent.context).inflate(
            R.layout.table_column_label,
            parent,
            false
        ) as TextView
        headerCellView.setBackgroundColor(
            ContextCompat.getColor(headerCellView.context, R.color.label_bg)
        )
        headerCellView.setTextColor(
            ContextCompat.getColor(headerCellView.context, R.color.label_fg)
        )
        val x = parent.childCount
        headerCellView.text = "$x"
        headerCellView.setOnClickListener {
            this.focus_column = true
            this.focus_row = false
            val cursor = opus_manager.get_cursor()
            opus_manager.set_cursor_position(cursor.y, x, listOf())
            this.setContextMenu(ContextMenu.Beat)
            this.tick()
            this.play_beat(x)
        }
        this.cache.addColumnLabel(headerCellView)
        parent.addView(headerCellView)
    }

    private fun buildLineView(y: Int): TableRow {
        var opus_manager = this.getMain().getOpusManager()
        val clo = opus_manager.get_channel_index(y)
        val channel = clo.first
        val line_offset = clo.second

        var tlOpusLines: TableLayout = this.activity!!.findViewById(R.id.tlOpusLines)
        var llLineLabels: LinearLayout = this.activity!!.findViewById(R.id.llLineLabels)
        val rowView = TableRow(tlOpusLines.context)
        rowView.setPadding(0,0,0,0)

        tlOpusLines.addView(rowView, y)
        this.cache.cacheLine(rowView, y)

        val rowLabel = LayoutInflater.from(llLineLabels.context).inflate(
            R.layout.table_line_label,
            llLineLabels,
            false
        ) as TextView


        if (!opus_manager.is_percussion(channel)) {
            if (line_offset == 0) {
                rowLabel.text = "$channel:$line_offset"
            } else {
                rowLabel.text = "  :$line_offset"
            }
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            rowLabel.text = "P:$instrument"
        }

        rowLabel.setOnClickListener {
            this.interact_rowLabel(it)
        }

        rowLabel.layoutParams.height = 125

        this.cache.addLineLabel(rowLabel)
        llLineLabels.addView(rowLabel)

        return rowView
    }

    private fun buildTreeView(parent: ViewGroup, y: Int, x: Int, position: List<Int>): View {
        var opus_manager = this.getMain().getOpusManager()
        val channel_index = opus_manager.get_channel_index(y)
        val tree = opus_manager.get_tree(BeatKey(channel_index.first, channel_index.second, x), position)


        if (tree.is_leaf()) {
            val leafView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_opusbutton,
                parent,
                false
            )
            var tvLeaf: TextView = leafView.findViewById(R.id.tvLeaf)

            tvLeaf.background = resources.getDrawable(
                if (!tree.is_event()) {
                    R.drawable.leaf
                } else {
                    R.drawable.leaf_active
                }
            )

            if (tree.is_event()) {
                val event = tree.get_event()!!
                tvLeaf.text = if (event.relative) {
                    if (event.note == 0 || event.note % event.radix != 0) {
                        val prefix = if (event.note < 0) {
                            getString(R.string.pfx_subtract)
                        } else {
                            getString(R.string.pfx_add)
                        }
                        "$prefix${get_number_string(abs(event.note), event.radix, 1)}"
                    } else {
                        val prefix = if (event.note < 0) {
                            getString(R.string.pfx_log)
                        } else {
                            getString(R.string.pfx_pow)
                        }
                        "$prefix${get_number_string(abs(event.note) / event.radix, event.radix, 1)}"
                    }
                } else if (!opus_manager.is_percussion(channel_index.first)) {
                    get_number_string(event.note, event.radix, 2)
                } else {
                    "!!"
                }
            } else {
                tvLeaf.text = getString(R.string.empty_note)
            }

            tvLeaf.setOnClickListener {
                this.interact_leafView_click(it)
            }

            tvLeaf.setOnLongClickListener {
                this.interact_leafView_longclick(it)
                true
            }

            if (position.isEmpty()) {
                parent.addView(leafView, x)
            } else {
                parent.addView(leafView)
            }

            this.cache.cacheTree(tvLeaf, y, x, position)
            return leafView
        } else {
            val cellLayout = LinearLayout(parent.context)
            this.cache.cacheTree(cellLayout, y, x, position)

            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(cellLayout as ViewGroup, y, x, new_position)
            }

            if (position.isEmpty()) {
                parent.addView(cellLayout, x)
            } else {
                parent.addView(cellLayout)
            }

            return cellLayout
        }
    }

    fun setContextMenu(menu_index: ContextMenu) {
        var opus_manager = this.getMain().getOpusManager()
        this.active_context_menu_index = menu_index
        val view_to_remove = this.cache.getActiveContextMenu()
        (view_to_remove?.parent as? ViewGroup)?.removeView(view_to_remove)
        var llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        when (menu_index) {
            ContextMenu.Line -> {
                val view = LayoutInflater.from(llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    llContextMenu,
                    false
                )

                var btnRemoveLine: TextView = view.findViewById(R.id.btnRemoveLine)
                var btnInsertLine: TextView = view.findViewById(R.id.btnInsertLine)
                var btnChoosePercussion: TextView = view.findViewById(R.id.btnChoosePercussion)

                if (opus_manager.line_count() == 1) {
                    btnRemoveLine.visibility = View.GONE
                }
                var beatkey = opus_manager.get_cursor().get_beatkey()
                var channel = beatkey.channel
                var line_offset = beatkey.line_offset

                if (!opus_manager.is_percussion(channel)) {
                    btnChoosePercussion.visibility = View.GONE
                } else {
                    btnChoosePercussion.setOnClickListener {
                        this.interact_btnChoosePercussion(it)
                    }

                    val drums = resources.getStringArray(R.array.midi_drums)

                    var instrument = opus_manager.get_percussion_instrument(line_offset)
                    btnChoosePercussion.text = "$instrument: ${drums[instrument]}"
                }

                btnRemoveLine.setOnClickListener {
                    this.interact_btnRemoveLine(it)
                }

                btnInsertLine.setOnClickListener {
                    this.interact_btnInsertLine(it)
                }


                llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            ContextMenu.Beat -> {
                val view = LayoutInflater.from(llContextMenu.context).inflate(
                    R.layout.contextmenu_column,
                    llContextMenu,
                    false
                )
                var btnInsertBeat: TextView = view.findViewById(R.id.btnInsertBeat)
                var btnRemoveBeat: TextView = view.findViewById(R.id.btnRemoveBeat)

                btnInsertBeat.setOnClickListener {
                    this.interact_btnInsertBeat(it)
                }

                btnRemoveBeat.setOnClickListener {
                    this.interact_btnRemoveBeat(it)
                }

                llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            ContextMenu.Leaf -> {
                this.setContextMenu_leaf()
            }

            ContextMenu.Linking -> {
                val view = LayoutInflater.from(llContextMenu.context).inflate(
                    R.layout.contextmenu_linking,
                    llContextMenu,
                    false
                )
                var btnUnLink: TextView = view.findViewById(R.id.btnUnLink)
                var btnUnLinkAll: TextView = view.findViewById(R.id.btnUnLinkAll)
                var btnCancelLink: TextView = view.findViewById(R.id.btnCancelLink)

                val cursor_key = opus_manager.get_cursor().get_beatkey()
                if (opus_manager.is_networked(cursor_key.channel, cursor_key.line_offset, cursor_key.beat)) {
                    btnUnLink.setOnClickListener {
                        this.interact_btnUnlink(it)
                    }
                    btnUnLinkAll.setOnClickListener {
                        this.interact_btnUnlinkAll(it)
                    }
                } else {
                    btnUnLink.visibility = View.GONE
                    btnUnLinkAll.visibility = View.GONE
                }

                btnCancelLink.setOnClickListener {
                    this.interact_btnCancelLink(it)
                }

                llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            else -> { }
        }
    }

    private fun setContextMenu_leaf() {
        var opus_manager = this.getMain().getOpusManager()
        var llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_cell,
            llContextMenu,
            false
        )
        var sRelative: ToggleButton = view.findViewById(R.id.sRelative)
        var llAbsolutePalette: LinearLayout = view.findViewById(R.id.llAbsolutePalette)
        var llRelativePalette: LinearLayout = view.findViewById(R.id.llRelativePalette)

        var btnUnset: TextView = view.findViewById(R.id.btnUnset)
        var btnSplit: TextView = view.findViewById(R.id.btnSplit)
        var btnRemove: TextView = view.findViewById(R.id.btnRemove)
        var btnInsert: TextView = view.findViewById(R.id.btnInsert)
        var nsOctave: NumberSelector = view.findViewById(R.id.nsOctave)
        var nsOffset: NumberSelector = view.findViewById(R.id.nsOffset)
        var nsRelativeValue: NumberSelector = view.findViewById(R.id.nsRelativeValue)
        var rosRelativeOption: RelativeOptionSelector = view.findViewById(R.id.rosRelativeOption)



        llContextMenu.addView(view)
        this.cache.setActiveContextMenu(view)

        val current_tree = opus_manager.get_tree_at_cursor()
        if (current_tree.is_event()) {
            this.relative_mode = current_tree.get_event()!!.relative
        }

        val cursor = opus_manager.get_cursor()

        if (opus_manager.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())) {
            sRelative.isChecked = this.relative_mode
            sRelative.setOnCheckedChangeListener { it, isChecked ->
                this.interact_sRelative_changed(it, isChecked)
            }
        } else {
            this.relative_mode = false
            sRelative.visibility = View.GONE
        }

        if (opus_manager.is_percussion(cursor.get_beatkey().channel)) {
            llAbsolutePalette.visibility = View.GONE
            llRelativePalette.visibility = View.GONE
            sRelative.visibility = View.GONE

            if (!opus_manager.get_tree_at_cursor().is_event()) {
                btnUnset.text = "Set"
            }

            btnUnset.setOnClickListener {
                this.interact_btnUnset(it)
            }

        } else if (!this.relative_mode) {
            llRelativePalette.visibility = View.GONE

            if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                if (!event.relative) {
                    nsOffset.setState(event.note % event.radix)
                    nsOctave.setState(event.note / event.radix)
                }
            }
            nsOffset.setOnChange(this::interact_nsOffset)
            nsOctave.setOnChange(this::interact_nsOctave)
        } else {
            llAbsolutePalette.visibility = View.GONE
            var selected_button = 0
            var new_progress: Int? = if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                if (event.relative) {
                    if (event.note > 0) {
                        if (event.note >= event.radix) {
                            selected_button = 2
                            event.note / event.radix
                        } else {
                            event.note
                        }
                    } else if (event.note < 0) {
                        if (event.note <= 0 - event.radix) {
                            selected_button = 3
                            event.note / (0 - event.radix)
                        } else {
                            selected_button = 1
                            0 - event.note
                        }
                    } else {
                        0
                    }
                } else {
                    null
                }
            } else {
                null
            }

            rosRelativeOption.setState(selected_button)

            this.resize_relative_value_selector()
            if (new_progress != null) {
                try {
                    nsRelativeValue.setState(new_progress)
                } catch (e: Exception) {
                    nsRelativeValue.unset_active_button()
                }
            }

            rosRelativeOption.setOnChange(this::interact_rosRelativeOption)
            nsRelativeValue.setOnChange(this::interact_nsRelativeValue)
        }

        btnSplit.setOnClickListener {
            this.interact_btnSplit(it)
        }

        btnUnset.setOnClickListener {
            this.interact_btnUnset(it)
        }
        var channel = opus_manager.get_cursor().get_beatkey().channel
        if (!opus_manager.is_percussion(channel) && current_tree.is_leaf() && !current_tree.is_event()) {
            btnUnset.visibility = View.GONE
        }

        if (opus_manager.get_cursor().get_position().isEmpty()) {
            btnRemove.visibility = View.GONE
        } else {
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener {
                this.interact_btnRemove(it)
            }
        }

        btnInsert.setOnClickListener {
            this.interact_btnInsert(it)
        }
    }

    private fun resize_relative_value_selector() {
        var llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        var opus_manager = this.getMain().getOpusManager()
        var maximum_note = 95

        var cursor = opus_manager.get_cursor()
        var mainMax = maximum_note
        var mainMin = 0
        var options_to_hide: MutableSet<Int> = mutableSetOf()
        var selector: RelativeOptionSelector = llContextMenu.findViewById(R.id.rosRelativeOption)
        // Need to consider all linked values as preceding values may differ
        for (linked_beat in opus_manager.get_all_linked(cursor.get_beatkey())) {
            var preceding_leaf = opus_manager.get_preceding_leaf_position(linked_beat, cursor.get_position())!!
            while (!opus_manager.get_tree(preceding_leaf.first, preceding_leaf.second).is_event()) {
                preceding_leaf = opus_manager.get_preceding_leaf_position(preceding_leaf.first, preceding_leaf.second)!!
            }
            var preceding_value = opus_manager.get_absolute_value(preceding_leaf.first, preceding_leaf.second)!!

            // Hide Relative options if they can't be used
            if (preceding_value > maximum_note - opus_manager.RADIX) {
                options_to_hide.add(2)
            }

            if (preceding_value < opus_manager.RADIX) {
                options_to_hide.add(3)
            }

            if (preceding_value > maximum_note) {
                options_to_hide.add(0)
            }

            if (preceding_value == 0) {
                options_to_hide.add(1)
            }

            var relMin = 1
            var relMax = maximum_note / opus_manager.RADIX
            when (selector.getState()) {
                0 -> {
                    relMin = 0
                    relMax =
                        Integer.min(maximum_note - preceding_value, opus_manager.RADIX - 1)
                }
                1 -> {
                    relMax = Integer.min(opus_manager.RADIX - 1, preceding_value)
                    relMin = Integer.min(relMin, relMax)
                }
                2 -> {
                    relMax = Integer.min(
                        (maximum_note - preceding_value) / opus_manager.RADIX,
                        relMax
                    )
                    relMin = Integer.min(relMin, relMax)
                }
                3 -> {
                    relMax = Integer.min(preceding_value / opus_manager.RADIX, relMax)
                    relMin = Integer.min(relMin, relMax)
                }
                else -> { }
            }
            mainMax = Integer.min(mainMax, relMax)
            mainMin = Integer.max(mainMin, relMin)
        }

        for (option in options_to_hide) {
            selector.hideOption(option)
        }
        var view: NumberSelector = llContextMenu.findViewById(R.id.nsRelativeValue)
        view.setRange(mainMin, mainMax)
        view.unset_active_button()
    }

    private fun rebuildBeatView(y: Int, x: Int) {
        var opus_manager = this.getMain().getOpusManager()
        val pair = opus_manager.get_channel_index(y)
        val main_beatkey = BeatKey(pair.first, pair.second, x)
        for (beatkey in opus_manager.get_all_linked(main_beatkey)) {
            val new_y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)
            val new_x = beatkey.beat
            this.cache.removeBeatView(new_y, new_x)
            val rowView = this.cache.getLine(new_y)
            this.buildTreeView(rowView, new_y, new_x, listOf())
        }
    }

    fun tick() {
        if (! this.ticking) {
            this.ticking = true
            this.tick_unapply_focus()
            this.tick_manage_lines()
            this.tick_manage_beats() // new/pop
            this.tick_update_beats() // changes
            this.tick_apply_focus()

            this.ticking = false
        }
    }

    private fun tick_manage_lines() {
        var opus_manager = this.getMain().getOpusManager()
        var lines_changed = false
        while (true) {
            val (channel, index, operation) = opus_manager.fetch_flag_line() ?: break
            when (operation) {
                0 -> {
                    val counts = opus_manager.get_channel_line_counts()

                    var y = 0
                    for (i in 0 until channel) {
                        y += counts[i]
                    }

                    this.cache.detachLine(y + index)
                    lines_changed = true
                }
                1 -> {
                    val y = opus_manager.get_y(channel, index)

                    val rowView = this.buildLineView(y)
                    for (x in 0 until opus_manager.opus_beat_count) {
                        this.buildTreeView(rowView, y, x, listOf())
                        opus_manager.flag_beat_change(BeatKey(channel, index, x))
                    }
                    lines_changed = true
                }
                2 -> {
                    val y = opus_manager.get_y(channel, index)
                    this.buildLineView(y)
                    lines_changed = true
                }
            }
        }

        if (lines_changed) { // Redraw labels
            var y = 0
            var line_counts = opus_manager.get_channel_line_counts()
            line_counts.forEachIndexed { channel, line_count ->
                for (i in 0 until line_count) {
                    val label = this.cache.getLineLabel(y) ?: continue
                    var textView: TextView = label.findViewById(R.id.textView)

                    // TODO: fix naming to reflect changes to channel handling
                    if (!opus_manager.is_percussion(channel)) {
                        textView.text = "$channel:$i"
                    } else {
                        val instrument = opus_manager.get_percussion_instrument(i)
                        textView.text = "P:$instrument"
                    }

                    for (x in 0 until opus_manager.opus_beat_count) {
                        for ((leaf, leaf_pos) in this.cache.get_all_leafs(y, x, listOf())) {
                            leaf.background = resources.getDrawable(
                                if (!opus_manager.get_tree(BeatKey(channel, i, x), leaf_pos).is_event()) {
                                    R.drawable.leaf
                                } else {
                                    R.drawable.leaf_active
                                }
                            )
                        }
                    }

                    y += 1
                }
            }
        }
    }

    private fun tick_manage_beats() {
        var opus_manager = this.getMain().getOpusManager()
        val updated_beats: MutableSet<Int> = mutableSetOf()
        var beats_changed = false
        var min_changed = opus_manager.opus_beat_count
        while (true) {
            val (index, operation) = opus_manager.fetch_flag_beat() ?: break
            min_changed = Integer.min(min_changed, index)
            when (operation) {
                1 -> {
                    this.newColumnLabel()
                    for (y in 0 until opus_manager.line_count()) {
                        val rowView = this.cache.getLine(y)
                        this.buildTreeView(rowView, y, index, listOf())
                    }
                    updated_beats.add(index)
                    this.cache.add_column_width(index)
                    beats_changed = true
                }
                0 -> {
                    this.cache.detachColumnLabel()
                    this.cache.remove_column_width(index)
                    for (y in 0 until opus_manager.line_count()) {
                        this.cache.removeBeatView(y, index)
                    }
                    beats_changed = true
                }
            }
        }
        if (beats_changed) {
            this.tick_resize_beats(updated_beats.toList())
            for (i in min_changed until opus_manager.opus_beat_count) {
                this.__tick_update_column_label_size(i)
            }
        }
    }

    private fun tick_update_beats() {
        var opus_manager = this.getMain().getOpusManager()
        val updated_beats: MutableSet<Int> = mutableSetOf()
        var beats_changed = false
        while (true) {
            val beatkey = opus_manager.fetch_flag_change() ?: break
            var y = opus_manager.get_y(
                beatkey.channel,
                beatkey.line_offset
            )
            this.rebuildBeatView(y, beatkey.beat )

            for (linked_beatkey in opus_manager.get_all_linked(beatkey)) {
                updated_beats.add(linked_beatkey.beat)
            }
            beats_changed = true
        }
        if (beats_changed) {
            this.tick_resize_beats(updated_beats.toList())
        }
    }

    private fun tick_resize_beats(updated_beats: List<Int>) {
        var opus_manager = this.getMain().getOpusManager()
        // resize Columns
        for (b in updated_beats) {
            var max_width = 0
            for (channel in 0 until opus_manager.channels.size) {
                for (line_offset in 0 until opus_manager.channels[channel].size) {
                    val tree = opus_manager.get_beat_tree(BeatKey(channel, line_offset, b))
                    val size = Integer.max(1, tree.size) * tree.get_max_child_weight()
                    max_width = Integer.max(max_width, size)
                }
            }

            var y = 0
            for (channel in 0 until opus_manager.channels.size) {
                for (line_offset in 0 until opus_manager.channels[channel].size) {
                    this.__tick_resize_beat_cell(channel, line_offset, b, max_width)
                    y += 1
                }
            }

            this.cache.set_column_width(b, max_width)
            this.__tick_update_column_label_size(b)
        }
    }

    private fun __tick_update_column_label_size(beat: Int) {
        var width = this.cache.get_column_width(beat)
        // Kludge: Need to remove/reattach label so it will shrink to a smaller
        // size if necessary
        val label_view = this.cache.getColumnLabel(beat)
        var label_row = label_view.parent as ViewGroup
        label_row.removeView(label_view)
        label_view.layoutParams.width = (width * 100) - 5
        label_row.addView(label_view, beat)
    }

    private fun __tick_resize_beat_cell(channel: Int, line_offset: Int, beat: Int, new_width: Int) {
        var opus_manager = this.getMain().getOpusManager()
        val stack: MutableList<Pair<Float, List<Int>>> = mutableListOf(Pair(new_width.toFloat(), listOf()))
        val key = BeatKey(channel, line_offset, beat)
        var y = opus_manager.get_y(channel, line_offset)
        while (stack.isNotEmpty()) {
            val (new_size, current_position) = stack.removeFirst()
            val current_tree = opus_manager.get_tree(key, current_position)

            val current_view = this.cache.getTreeView(y, beat, current_position)
            val param = current_view!!.layoutParams as ViewGroup.MarginLayoutParams

            if (!current_tree.is_leaf()) {
                for (i in 0 until current_tree.size) {
                    val next_pos = current_position.toMutableList()
                    next_pos.add(i)
                    stack.add(Pair(new_size / current_tree.size.toFloat(), next_pos))
                }

                param.width = (new_size * 100.toFloat()).toInt()
                param.height = 130
            } else {
                param.width = (new_size * 100.toFloat()).toInt() - 5
                param.height = 125
                param.setMargins(0,0,5,5)

                // TODO: Move this somewhere better
                current_view.background = resources.getDrawable(
                    if (!current_tree.is_event()) {
                        R.drawable.leaf
                    } else {
                        R.drawable.leaf_active
                    }
                )

            }

            current_view.layoutParams = param
        }
    }

    private fun tick_apply_focus() {
        var opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        var focused: MutableSet<Pair<BeatKey, List<Int>>> = mutableSetOf()
        if (this.focus_column) {
            for (y in 0 until opus_manager.line_count()) {
                var (channel, index) = opus_manager.get_channel_index(y)
                focused.add(
                    Pair(
                        BeatKey(channel, index, cursor.get_beatkey().beat),
                        listOf()
                    )
                )
            }
        } else if (this.focus_row) {
            for (x in 0 until opus_manager.opus_beat_count) {
                var beatkey = cursor.get_beatkey()
                focused.add(
                    Pair(
                        BeatKey(beatkey.channel, beatkey.line_offset, x),
                        listOf()
                    )
                )
            }
        } else {
            focused.add(Pair(cursor.get_beatkey(), cursor.get_position()))
        }

        for ((beatkey, position) in focused) {
            var linked_beats = opus_manager.get_all_linked(beatkey)

            for (linked_beat in linked_beats) {
                var y = opus_manager.get_y(linked_beat.channel, linked_beat.line_offset)
                for ((view, leaf_pos) in this.cache.get_all_leafs(y, linked_beat.beat, position)) {
                    if (view is LinearLayout) {
                        continue
                    }

                    view.background = resources.getDrawable(
                        if (opus_manager.get_tree(linked_beat, leaf_pos).is_event()) {
                            R.drawable.focus_leaf_active
                        } else {
                            R.drawable.focus_leaf
                        }
                    )

                    this.cache.addFocusedLeaf(y, linked_beat.beat, leaf_pos)
                }
            }
        }

        if (this.linking_beat_b != null) {
            var cursor_diff = opus_manager.get_cursor_difference(this.linking_beat!!, this.linking_beat_b!!)

            for (y in 0 .. cursor_diff.first) {
                var new_y = y + opus_manager.get_y(
                    this.linking_beat!!.channel,
                    this.linking_beat!!.line_offset
                )

                var target_pair = opus_manager.get_channel_index(new_y)
                for (x in 0 .. cursor_diff.second) {
                    var new_x = x + this.linking_beat!!.beat
                    var new_beatkey = BeatKey(target_pair.first, target_pair.second, new_x)
                    for ((view, leaf_pos) in this.cache.get_all_leafs(new_y, new_x, listOf())) {
                        view.background = resources.getDrawable(
                            if (opus_manager.get_tree(new_beatkey, leaf_pos).is_event()) {
                                R.drawable.focus_leaf_active
                            } else {
                                R.drawable.focus_leaf
                            }
                        )
                        this.cache.addFocusedLeaf(new_y, new_x, leaf_pos)
                    }
                }
            }
        }
    }

    private fun tick_unapply_focus() {
        var opus_manager = this.getMain().getOpusManager()
        while (true) {
            var (y, x, cached_position) = this.cache.popFocusedLeaf() ?: break
            var view = this.cache.getTreeView(y, x, cached_position) ?: continue
            if (y >= opus_manager.line_count()) {
                continue
            }
            var pair = opus_manager.get_channel_index(y)

            try {
                if (opus_manager.get_tree(BeatKey(pair.first, pair.second, x), cached_position).is_event()) {
                    view.background = resources.getDrawable(R.drawable.leaf_active)
                } else {
                    view.background = resources.getDrawable(R.drawable.leaf)
                }
            } catch (e: Exception) {
                continue
            }
        }
    }

    fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        var opus_manager = this.getMain().getOpusManager()
        this.resize_relative_value_selector()
        var current_tree = opus_manager.get_tree_at_cursor()
        if (! current_tree.is_event() || ! current_tree.get_event()!!.relative) {
            return
        }

        val event = current_tree.get_event()!!
        var checkstate_and_value: Pair<Int, Int> = if (event.note >= event.radix) {
            Pair(2, event.note / event.radix)
        } else if (event.note > 0) {
            Pair(0, event.note)
        } else if (event.note <= 0 - event.radix) {
            Pair(3, 0 - (event.note / event.radix))
        } else if (event.note < 0) {
            Pair(1, 0 - event.note)
        } else {
            Pair(0, 0)
        }

        var llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        if (checkstate_and_value.first == view.getState()) {
            var valueSelector: NumberSelector = llContextMenu.findViewById(R.id.nsRelativeValue)
            try {
                valueSelector.setState(checkstate_and_value.second)
            } catch (e: Exception) {
                valueSelector.unset_active_button()
            }
        }
    }

    private fun change_relative_value(progress: Int) {
        var llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        var opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        val beat_key = cursor.get_beatkey()
        var relativeOptionSelector: RelativeOptionSelector = llContextMenu.findViewById(R.id.rosRelativeOption)
        val new_value = when (relativeOptionSelector.getState()) {
            0 -> { progress }
            1 -> { 0 - progress }
            2 -> { opus_manager.RADIX * progress }
            3 -> { 0 - (opus_manager.RADIX * progress) }
            else -> { progress }
        }

        val event = OpusEvent(
            new_value,
            opus_manager.RADIX,
            beat_key.channel,
            true
        )

        this.getMain().play_event(beat_key.channel, event.note)

        opus_manager.set_event(beat_key, cursor.position, event)
        this.tick()
    }

    private fun interact_btnSplit(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        opus_manager.split_tree_at_cursor()
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnUnset(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        this.setContextMenu(ContextMenu.Leaf)
        var cursor = opus_manager.get_cursor()
        var channel = cursor.get_beatkey().channel
        if (!opus_manager.is_percussion(channel) || opus_manager.get_tree_at_cursor().is_event()) {
            Log.e("AAA", "Attempingunset")
            opus_manager.unset_at_cursor()
        } else {
            opus_manager.set_percussion_event_at_cursor()
        }

        this.tick()
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_btnUnlink(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.remove_link_from_network(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnUnlinkAll(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.clear_links_in_network(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnCancelLink(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        opus_manager.get_cursor().settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnInsertBeat(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        opus_manager.insert_beat_at_cursor()
        this.tick()
    }

    private fun interact_btnRemoveBeat(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        if (opus_manager.opus_beat_count > 1) {
            opus_manager.remove_beat_at_cursor()
        }
        this.tick()
    }

    private fun interact_btnRemoveLine(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        if (opus_manager.line_count() > 1) {
            opus_manager.remove_line_at_cursor()
        }
        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        this.tick()
    }

    private fun interact_btnInsertLine(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        opus_manager.new_line_at_cursor()
        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        this.tick()
    }

    private fun interact_leafView_click(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        this.focus_column = false
        this.focus_row = false

        val key = this.cache.getTreeViewYXPosition(view) ?: return
        opus_manager.set_cursor_position(key.first, key.second, key.third)

        var cursor_beatkey = opus_manager.get_cursor().get_beatkey()
        if (this.linking_beat != null) {
            // If a second link point hasn't been selected, assume just one beat is being linked
            if (this.linking_beat_b == null) {
                opus_manager.link_beats(cursor_beatkey, this.linking_beat!!)
            } else {
                opus_manager.link_beat_range(cursor_beatkey, this.linking_beat!!, this.linking_beat_b!!)
            }

            this.linking_beat = null
            this.linking_beat_b = null
        }

        this.tick()
        this.setContextMenu(ContextMenu.Leaf)

        var cursor_tree = opus_manager.get_tree_at_cursor()
        if (cursor_tree.is_event()) {
            this.getMain().play_event(cursor_beatkey.channel, cursor_tree.get_event()!!.note)
        }
    }

    fun interact_leafView_longclick(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        val key = this.cache.getTreeViewYXPosition(view) ?: return
        val pair = opus_manager.get_channel_index(key.first)
        if (this.linking_beat == null) {
            this.linking_beat = BeatKey(pair.first, pair.second, key.second)
        } else {
            this.linking_beat_b = BeatKey(pair.first, pair.second, key.second)
        }
        opus_manager.set_cursor_position(key.first, key.second, listOf())
        this.setContextMenu(ContextMenu.Linking)
        this.tick()
    }

    public fun interact_nsOffset(view: NumberSelector) {
        var opus_manager = this.getMain().getOpusManager()
        val progress = view.getState()!!
        var current_tree = opus_manager.get_tree_at_cursor()

        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()

        val event = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            val old_octave = event.note / event.radix
            event.note = (old_octave * event.radix) + progress

            event
        } else {
            OpusEvent(
                progress,
                opus_manager.RADIX,
                beatkey.channel,
                false
            )
        }

        opus_manager.set_event(beatkey, position, event)

        this.getMain().play_event(beatkey.channel, event.note)

        var nsOctave: NumberSelector = this.activity!!.findViewById(R.id.nsOctave)
        if (nsOctave.getState() == null) {
            nsOctave.setState(event.note / event.radix)
        }

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    public fun interact_nsOctave(view: NumberSelector) {
        var opus_manager = this.getMain().getOpusManager()
        val progress = view.getState() ?: return

        var current_tree = opus_manager.get_tree_at_cursor()
        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()

        val event = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            event.note = (progress * event.radix) + (event.note % event.radix)
            event
        } else {
            OpusEvent(
                progress * opus_manager.RADIX,
                opus_manager.RADIX,
                beatkey.channel,
                false
            )
        }

        opus_manager.set_event(beatkey, position, event)
        var nsOffset: NumberSelector = this.activity!!.findViewById(R.id.nsOffset)
        if (nsOffset.getState() == null) {
            nsOffset.setState(event.note % event.radix)
        }

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_rowLabel(view: View) {
        this.focus_column = false
        this.focus_row = true

        var abs_y: Int = 0
        val label_column = view.parent!! as ViewGroup
        for (i in 0 until label_column.childCount) {
            if (label_column.getChildAt(i) == view) {
                abs_y = i
                break
            }
        }

        var opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(abs_y, cursor.x, listOf())
        this.tick()
        this.setContextMenu(ContextMenu.Line)
    }

    private fun interact_sRelative_changed(view: View, isChecked: Boolean) {
        var opus_manager = this.getMain().getOpusManager()
        if (isChecked) {
            opus_manager.convert_event_at_cursor_to_relative()
        } else {
            try {
                opus_manager.convert_event_at_cursor_to_absolute()
            } catch (e: Exception) {
                Toast.makeText(activity?.applicationContext, "Can't convert event", Toast.LENGTH_SHORT).show()
                (view as ToggleButton).isChecked = true
                return
            }
        }
        this.relative_mode = isChecked
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnRemove(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        if (cursor.get_position().isNotEmpty()) {
            opus_manager.remove_tree_at_cursor()
        }
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnInsert(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        val position = opus_manager.get_cursor().get_position()
        if (position.isEmpty()) {
            opus_manager.split_tree_at_cursor()
        } else {
            opus_manager.insert_after_cursor()
        }
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    fun interact_nsRelativeValue(view: NumberSelector) {
        this.change_relative_value(view.getState()!!)
    }
    private fun interact_btnChoosePercussion(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        val popupMenu = PopupMenu(this.activity?.window?.decorView?.rootView?.context, view)
        val cursor = opus_manager.get_cursor()
        val drums = resources.getStringArray(R.array.midi_drums)
        drums.forEachIndexed { i, string ->
            popupMenu.menu.add(0, i, i, "$i: $string")
        }
        popupMenu.setOnMenuItemClickListener {
            opus_manager.set_percussion_instrument(
                cursor.get_beatkey().line_offset,
                it.itemId
            )
            this.tick()
            val y = opus_manager.get_cursor().get_y()
            var textView: TextView = this.cache.getLineLabel(y)!!.findViewById(R.id.textView)
            textView.text = "P:${it.itemId}"
            this.setContextMenu(ContextMenu.Line) // TODO: overkill?
            true
        }
        popupMenu.show()
    }

    fun play_beat(beat: Int) {
        var opus_manager = this.getMain().getOpusManager()
        var midi = opus_manager.get_midi(beat, beat + 1)
        this.getMain().play_midi(midi)
    }
}