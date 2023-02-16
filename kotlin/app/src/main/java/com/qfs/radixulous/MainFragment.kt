package com.qfs.radixulous

import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.qfs.radixulous.databinding.FragmentMainBinding
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import kotlin.math.abs

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private var active_context_menu_index: ContextMenu = ContextMenu.None

    var linking_beat: BeatKey? = null
    var linking_beat_b: BeatKey? = null
    private var relative_mode: Boolean = false

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

        setFragmentResultListener("LOAD") { _, bundle: Bundle? ->
            val main = this.getMain()

            bundle!!.getString("PATH")?.let { path: String ->
                this.takedownCurrent()

                main.getOpusManager().load(path)
                main.update_title_text()

                this.setContextMenu(ContextMenu.Leaf)
                main.tick()
            }
            main.update_menu_options()
            main.setup_config_drawer()
        }

        setFragmentResultListener("RETURNED") { _, bundle: Bundle? ->
            val main = this.getMain()
            this.takedownCurrent()
            main.getOpusManager().reflag()
            main.update_title_text()

            this.setContextMenu(ContextMenu.Leaf)
            main.tick()
            main.update_menu_options()
        }
        //binding.buttonFirst.setOnClickListener {
        //    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        //}
    }

    override fun onStart() {
        super.onStart()
        val main = this.getMain()
        main.setup_config_drawer()
        if (main.get_current_project_title() == null) {
            main.newProject()
            main.update_title_text()
            this.setContextMenu(ContextMenu.Leaf)
            main.tick()
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
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        if (opus_manager.has_history()) {
            opus_manager.apply_undo()
            main.tick()
            this.setContextMenu(ContextMenu.Leaf)
        } else {
            main.feedback_msg(getString(R.string.msg_undo_none))
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
        val parent: ViewGroup = this.activity!!.findViewById(R.id.llColumnLabels)
        val headerCellView = LayoutInflater.from(parent.context).inflate(
            R.layout.table_column_label,
            parent,
            false
        ) as LinearLayout
        var textView: TextView = headerCellView.findViewById(R.id.textView)

        val x = parent.childCount
        //textView.setBackgroundColor(
        //    ContextCompat.getColor(
        //        textView.context,
        //        if (x % 2 == 0) {
        //            R.color.column_label_even
        //        } else {
        //            R.color.column_label_odd
        //        }
        //    )
        //)
        //textView.setTextColor(
        //    ContextCompat.getColor(headerCellView.context, R.color.label_fg)
        //)
        textView.text = "$x"
        headerCellView.setOnClickListener {
            this.interact_column_header(it)
        }
        this.cache.addColumnLabel(headerCellView)
        parent.addView(headerCellView)
    }

    private fun buildLineView(y: Int): TableRow {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val clo = opus_manager.get_channel_index(y)
        val channel = clo.first
        val line_offset = clo.second

        val tlOpusLines: TableLayout = this.activity!!.findViewById(R.id.tlOpusLines)
        val llLineLabels: LinearLayout = this.activity!!.findViewById(R.id.llLineLabels)

        var rowView: TableRow = LayoutInflater.from(tlOpusLines.context).inflate(
            R.layout.table_row,
            tlOpusLines,
            false
        ) as TableRow

        tlOpusLines.addView(rowView, y)
        this.cache.cacheLine(rowView, y)

        for (i in 0 until opus_manager.opus_beat_count) {
            var wrapper = LayoutInflater.from(rowView.context).inflate(
                R.layout.beat_node,
                rowView,
                false
            )
            rowView.addView(wrapper)
        }

        /////////////////////////////////

        val rowLabel = LayoutInflater.from(llLineLabels.context).inflate(
            R.layout.table_line_label,
            llLineLabels,
            false
        ) as LinearLayout

        var rowLabelText = rowLabel.getChildAt(0) as TextView

        if (!opus_manager.is_percussion(channel)) {
            if (line_offset == 0) {
                rowLabelText.text = "$channel:$line_offset"
            } else {
                rowLabelText.text = "  :$line_offset"
            }
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            rowLabelText.text = "P:$instrument"
        }

        rowLabel.setOnClickListener {
            this.interact_rowLabel(it)
        }

        this.cache.addLineLabel(rowLabel)
        llLineLabels.addView(rowLabel)

        return rowView
    }

    private fun buildTreeView(parent: ViewGroup, y: Int, x: Int, position: List<Int>): View {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val (channel, index) = opus_manager.get_channel_index(y)
        var beatKey = BeatKey(channel, index, x)
        val tree = opus_manager.get_tree(beatKey, position)

        if (tree.is_leaf()) {
            val tvLeaf = LeafButton(parent.context, main, tree.get_event(), opus_manager.is_percussion(channel))

            tvLeaf.setOnClickListener {
                this.interact_leafView_click(it)
            }

            tvLeaf.setOnLongClickListener {
                this.interact_leafView_longclick(it)
                true
            }

            parent.addView(tvLeaf)
            val param = tvLeaf!!.layoutParams as LinearLayout.LayoutParams
            param.gravity = Gravity.CENTER
            param.height = MATCH_PARENT
            tvLeaf.setLayoutParams(param)

            this.cache.cacheTree(tvLeaf, y, x, position)
            return tvLeaf
        } else {
            val cellLayout: LinearLayout = LayoutInflater.from(parent.context).inflate(
                R.layout.tree_node,
                parent,
                false
            ) as LinearLayout

            this.cache.cacheTree(cellLayout, y, x, position)

            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(cellLayout as ViewGroup, y, x, new_position)
            }

            parent.addView(cellLayout)

            return cellLayout
        }
    }

    fun setContextMenu(menu_index: ContextMenu) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val view_to_remove = this.cache.getActiveContextMenu()
        (view_to_remove?.parent as? ViewGroup)?.removeView(view_to_remove)
        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        when (menu_index) {
            ContextMenu.Line -> {
                val view = LayoutInflater.from(llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    llContextMenu,
                    false
                )

                val btnRemoveLine: TextView = view.findViewById(R.id.btnRemoveLine)
                val btnInsertLine: TextView = view.findViewById(R.id.btnInsertLine)
                val btnChoosePercussion: TextView = view.findViewById(R.id.btnChoosePercussion)

                if (opus_manager.line_count() == 1) {
                    btnRemoveLine.visibility = View.GONE
                }
                val beatkey = opus_manager.get_cursor().get_beatkey()
                val channel = beatkey.channel
                val line_offset = beatkey.line_offset

                if (!opus_manager.is_percussion(channel)) {
                    btnChoosePercussion.visibility = View.GONE
                } else {
                    btnChoosePercussion.setOnClickListener {
                        this.interact_btnChoosePercussion(it)
                    }

                    val drums = resources.getStringArray(R.array.midi_drums)

                    val instrument = opus_manager.get_percussion_instrument(line_offset)
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
                val btnInsertBeat: TextView = view.findViewById(R.id.btnInsertBeat)
                val btnRemoveBeat: TextView = view.findViewById(R.id.btnRemoveBeat)

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
                val btnUnLink: TextView = view.findViewById(R.id.btnUnLink)
                val btnUnLinkAll: TextView = view.findViewById(R.id.btnUnLinkAll)
                val btnCancelLink: TextView = view.findViewById(R.id.btnCancelLink)

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
        this.active_context_menu_index = menu_index
    }

    private fun setContextMenu_leaf() {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_cell,
            llContextMenu,
            false
        )
        llContextMenu.addView(view)
        this.cache.setActiveContextMenu(view)

        val sRelative: ToggleButton = view.findViewById(R.id.sRelative)
        val llAbsolutePalette: LinearLayout = view.findViewById(R.id.llAbsolutePalette)
        val llRelativePalette: LinearLayout = view.findViewById(R.id.llRelativePalette)

        val btnUnset: TextView = view.findViewById(R.id.btnUnset)
        val btnSplit: TextView = view.findViewById(R.id.btnSplit)
        val btnRemove: TextView = view.findViewById(R.id.btnRemove)
        val btnInsert: TextView = view.findViewById(R.id.btnInsert)
        val nsOctave: NumberSelector = view.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = view.findViewById(R.id.nsOffset)
        val nsRelativeValue: NumberSelector = view.findViewById(R.id.nsRelativeValue)
        val rosRelativeOption: RelativeOptionSelector = view.findViewById(R.id.rosRelativeOption)

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
                    nsOffset.setState(event.note % event.radix, true, true)
                    nsOctave.setState(event.note / event.radix, true, true)
                }
            }
            nsOffset.setOnChange(this::interact_nsOffset)
            nsOctave.setOnChange(this::interact_nsOctave)
        } else {
            llAbsolutePalette.visibility = View.GONE
            var selected_button = 0
            val new_progress: Int? = if (current_tree.is_event()) {
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
                    nsRelativeValue.setState(new_progress, true)
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
        val channel = opus_manager.get_cursor().get_beatkey().channel
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
        var main = this.getMain()
        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        val opus_manager = main.getOpusManager()
        val maximum_note = 95

        val cursor = opus_manager.get_cursor()
        var mainMax = maximum_note
        var mainMin = 0
        val options_to_hide: MutableSet<Int> = mutableSetOf()
        val selector: RelativeOptionSelector = llContextMenu.findViewById(R.id.rosRelativeOption)
        // Need to consider all linked values as preceding values may differ
        for (linked_beat in opus_manager.get_all_linked(cursor.get_beatkey())) {
            var preceding_leaf = opus_manager.get_preceding_leaf_position(linked_beat, cursor.get_position())!!
            while (!opus_manager.get_tree(preceding_leaf.first, preceding_leaf.second).is_event()) {
                preceding_leaf = opus_manager.get_preceding_leaf_position(preceding_leaf.first, preceding_leaf.second)!!
            }
            val preceding_value = opus_manager.get_absolute_value(preceding_leaf.first, preceding_leaf.second)!!

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
        val view: NumberSelector = llContextMenu.findViewById(R.id.nsRelativeValue)
        view.setRange(mainMin, mainMax)
        view.unset_active_button()
    }

    private fun rebuildBeatView(y: Int, x: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val pair = opus_manager.get_channel_index(y)
        val main_beatkey = BeatKey(pair.first, pair.second, x)
        for (beatkey in opus_manager.get_all_linked(main_beatkey)) {
            val new_y = opus_manager.get_y(beatkey.channel, beatkey.line_offset)
            val new_x = beatkey.beat

            this.cache.removeBeatView(new_y, new_x)
            val rowView = this.cache.getLine(new_y)

            var new_wrapper = LayoutInflater.from(rowView.context).inflate(
                R.layout.beat_node,
                rowView,
                false
            )

            rowView.addView(new_wrapper, new_x)
            this.buildTreeView(new_wrapper as ViewGroup, new_y, new_x, listOf())
            new_wrapper.measure(0, 0)
            this.cache.set_column_width(new_x, new_wrapper.measuredWidth)
        }
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        this.resize_relative_value_selector()
        val current_tree = opus_manager.get_tree_at_cursor()
        if (! current_tree.is_event() || ! current_tree.get_event()!!.relative) {
            return
        }

        val event = current_tree.get_event()!!
        val checkstate_and_value: Pair<Int, Int> = if (event.note >= event.radix) {
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

        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        if (checkstate_and_value.first == view.getState()) {
            val valueSelector: NumberSelector = llContextMenu.findViewById(R.id.nsRelativeValue)
            try {
                valueSelector.setState(checkstate_and_value.second, true, true)
            } catch (e: Exception) {
                valueSelector.unset_active_button()
            }
        }
    }

    private fun change_relative_value(progress: Int) {
        var main = this.getMain()
        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        val beat_key = cursor.get_beatkey()
        val relativeOptionSelector: RelativeOptionSelector = llContextMenu.findViewById(R.id.rosRelativeOption)
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

        this.set_event(beat_key, cursor.position, event)

        val note = opus_manager.get_absolute_value(beat_key, cursor.get_position())
        if (note != null) {
            main.play_event(beat_key.channel, note!! + new_value)
        }

        main.tick()
    }

    // Wrapper around the OpusManager::set_event(). needed in order to validate proceding events
    private fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.set_event(beat_key, position, event)
    }

    private fun interact_btnSplit(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.split_tree_at_cursor()
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_btnUnset(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        this.setContextMenu(ContextMenu.Leaf)
        val cursor = opus_manager.get_cursor()
        val channel = cursor.get_beatkey().channel
        if (!opus_manager.is_percussion(channel) || opus_manager.get_tree_at_cursor().is_event()) {
            opus_manager.unset_at_cursor()
        } else {
            opus_manager.set_percussion_event_at_cursor()
        }

        main.tick()
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_btnUnlink(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.remove_link_from_network(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_btnUnlinkAll(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.clear_links_in_network(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_btnCancelLink(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.get_cursor().settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_btnInsertBeat(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.insert_beat_at_cursor()
        main.tick()
    }

    private fun interact_btnRemoveBeat(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        if (opus_manager.opus_beat_count > 1) {
            opus_manager.remove_beat_at_cursor()
        }
        main.tick()
    }

    private fun interact_btnRemoveLine(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        if (opus_manager.line_count() > 1) {
            opus_manager.remove_line_at_cursor()
        }
        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        main.tick()
    }

    private fun interact_btnInsertLine(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.new_line_at_cursor()
        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        main.tick()
    }

    private fun interact_leafView_click(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        main.focus_column = false
        main.focus_row = false

        val key = this.cache.getTreeViewYXPosition(view) ?: return
        opus_manager.set_cursor_position(key.first, key.second, key.third)

        val cursor_beatkey = opus_manager.get_cursor().get_beatkey()
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

        main.tick()
        this.setContextMenu(ContextMenu.Leaf)

        val cursor_tree = opus_manager.get_tree_at_cursor()
        if (cursor_tree.is_event()) {
            val abs_value = opus_manager.get_absolute_value(cursor_beatkey, opus_manager.get_cursor().get_position())
            if (abs_value != null) {
                main.play_event(cursor_beatkey.channel, abs_value)
            }
        }
    }

    private fun interact_leafView_longclick(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val key = this.cache.getTreeViewYXPosition(view) ?: return
        val pair = opus_manager.get_channel_index(key.first)
        if (this.linking_beat == null) {
            this.linking_beat = BeatKey(pair.first, pair.second, key.second)
        } else {
            this.linking_beat_b = BeatKey(pair.first, pair.second, key.second)
        }
        opus_manager.set_cursor_position(key.first, key.second, listOf())
        this.setContextMenu(ContextMenu.Linking)
        main.tick()
    }

    private fun interact_nsOffset(view: NumberSelector) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree_at_cursor()

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

        main.play_event(beatkey.channel, event.note)

        val nsOctave: NumberSelector = this.activity!!.findViewById(R.id.nsOctave)
        if (nsOctave.getState() == null) {
            nsOctave.setState(event.note / event.radix, true)
        }

        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_nsOctave(view: NumberSelector) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val progress = view.getState() ?: return

        val current_tree = opus_manager.get_tree_at_cursor()
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
        val nsOffset: NumberSelector = this.activity!!.findViewById(R.id.nsOffset)
        if (nsOffset.getState() == null) {
            nsOffset.setState(event.note % event.radix, true)
        }

        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_rowLabel(view: View) {
        var main = this.getMain()
        main.focus_column = false
        main.focus_row = true

        var abs_y: Int = 0
        val label_column = view.parent!! as ViewGroup
        for (i in 0 until label_column.childCount) {
            if (label_column.getChildAt(i) == view) {
                abs_y = i
                break
            }
        }

        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(abs_y, cursor.x, listOf())
        main.tick()
        this.setContextMenu(ContextMenu.Line)
    }

    private fun interact_sRelative_changed(view: View, isChecked: Boolean) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        if (isChecked) {
            opus_manager.convert_event_at_cursor_to_relative()
        } else {
            try {
                opus_manager.convert_event_at_cursor_to_absolute()
            } catch (e: Exception) {
                main.feedback_msg("Can't convert event")
                (view as ToggleButton).isChecked = true
                return
            }
        }
        this.relative_mode = isChecked
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_btnRemove(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        if (cursor.get_position().isNotEmpty()) {
            opus_manager.remove_tree_at_cursor()
        }
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_btnInsert(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val position = opus_manager.get_cursor().get_position()
        if (position.isEmpty()) {
            opus_manager.split_tree_at_cursor()
        } else {
            opus_manager.insert_after_cursor()
        }
        this.setContextMenu(ContextMenu.Leaf)
        main.tick()
    }

    private fun interact_nsRelativeValue(view: NumberSelector) {
        this.change_relative_value(view.getState()!!)
    }

    private fun interact_btnChoosePercussion(view: View) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val wrapper = ContextThemeWrapper(this.activity?.window?.decorView?.rootView?.context, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, view)
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
            main.tick()
            val y = opus_manager.get_cursor().get_y()
            val textView: TextView = this.cache.getLineLabel(y)!!.findViewById(R.id.textView)
            textView.text = "P:${it.itemId}"
            this.setContextMenu(ContextMenu.Line) // TODO: overkill?
            true
        }
        popupMenu.show()
    }

    fun play_beat(beat: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        val midi = opus_manager.get_midi(beat, beat + 1)
        main.play_midi(midi)
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        var main = this.getMain()

        val hsvTable: HorizontalScrollView = main.findViewById(R.id.hsvTable)
        val llColumnLabels: LinearLayout = main.findViewById(R.id.llColumnLabels)

        val view = llColumnLabels.getChildAt(beat)
        if (view != null) {
            hsvTable.smoothScrollTo(view.left, hsvTable.scrollY)
            if (select) {
                this.interact_column_header(view)
            }
        }
    }


    fun interact_column_header(view: View) {
        var main = this.getMain()
        val x = (view.parent as ViewGroup).indexOfChild(view)
        val opus_manager = main.getOpusManager()
        main.focus_column = true
        main.focus_row = false
        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(cursor.y, x, listOf())

        this.setContextMenu(ContextMenu.Beat)
        main.tick()
    }

    fun line_remove(index: Int) {
        this.cache.detachLine(index)
    }

    fun line_new(index: Int, beat_count: Int) {
        val rowView = this.buildLineView(index)
        for (x in 0 until beat_count) {
            this.buildTreeView(rowView.getChildAt(x) as ViewGroup, index, x, listOf())
        }
    }

    fun line_update_labels(opus_manager: OpusManager) {
        var y = 0
        val line_counts = opus_manager.get_channel_line_counts()
        var channel_offset = 0
        line_counts.forEachIndexed { channel, line_count ->
            for (i in 0 until line_count) {
                val label = this.cache.getLineLabel(y) ?: continue
                val textView: TextView = label.findViewById(R.id.textView)
                //textView.setBackgroundColor(
                //    ContextCompat.getColor(
                //        textView.context,
                //        if (channel_offset % 2 == 0) {
                //            if (i % 2 == 0) {
                //                R.color.line_label_channel_even_line_even
                //            } else {
                //                R.color.line_label_channel_even_line_odd
                //            }
                //        } else {
                //            if (i % 2 == 0) {
                //                R.color.line_label_channel_odd_line_even
                //            } else {
                //                R.color.line_label_channel_odd_line_odd
                //            }
                //        }
                //    )
                //)


                // TODO: fix naming to reflect changes to channel handling
                if (!opus_manager.is_percussion(channel)) {
                    textView.text = "$channel:$i"
                } else {
                    val instrument = opus_manager.get_percussion_instrument(i)
                    textView.text = "P:$instrument"
                }

                y += 1
            }
            channel_offset += 1
        }
    }

    fun beat_new(index: Int) {
        this.newColumnLabel()

        this.cache.getLines().forEachIndexed { y: Int, rowView: LinearLayout ->
            var new_wrapper = LayoutInflater.from(rowView.context).inflate(
                R.layout.beat_node,
                rowView,
                false
            ) as ViewGroup

            rowView.addView(new_wrapper, index)
            this.buildTreeView(new_wrapper, y, index, listOf())
        }

        this.cache.add_column_width(index)
        this.update_column_label_size(index)
    }

    fun beat_remove(index: Int) {
        this.cache.detachColumnLabel()
        for (y in 0 until this.cache.getLineCount()) {
            this.cache.removeBeatView(y, index)
        }

        this.cache.remove_column_width(index)
    }

    fun beat_update(y: Int, x: Int) {
        this.rebuildBeatView(y, x)
        this.update_column_label_size(x)
    }

    fun update_column_labels(from: Int, to: Int) {
        for (i in from until to) {
            this.update_column_label_size(i)
        }
    }

    fun update_column_label_size(beat: Int) {
        val width = this.cache.get_column_width(beat)
        // Kludge: Need to remove/reattach label so it will shrink to a smaller
        // size if necessary
        val label_view = this.cache.getColumnLabel(beat)
        val label_row = label_view.parent as ViewGroup
        label_row.removeView(label_view)
        label_view.layoutParams.width = width
        label_row.addView(label_view, beat)
    }

    //fun update_column_background_color(beat: Int) {
    //    for (line in this.cache.getLines()) {
    //        var wrapper = line.getChildAt(beat)
    //        wrapper.setBackgroundColor(
    //            ContextCompat.getColor(
    //                wrapper.context,
    //                if (beat % 2 == 0) {
    //                    R.color.column_even
    //                } else {
    //                    R.color.column_odd
    //                }
    //            )
    //        )
    //    }
    //}

    fun apply_focus(focused: Set<Pair<BeatKey, List<Int>>>, opus_manager: OpusManager) {
        for ((beatkey, position) in focused) {
            val linked_beats = opus_manager.get_all_linked(beatkey)
            for (linked_beat in linked_beats) {
                val y = opus_manager.get_y(linked_beat.channel, linked_beat.line_offset)
                for ((view, leaf_pos) in this.cache.get_all_leafs(y, linked_beat.beat, position)) {
                    if (view !is LeafButton) {
                        continue
                    }
                    view.setFocused(true)
                    this.cache.addFocusedLeaf(y, linked_beat.beat, leaf_pos)
                }
            }
        }

        if (this.linking_beat_b != null) {
            val cursor_diff = opus_manager.get_cursor_difference(this.linking_beat!!, this.linking_beat_b!!)

            for (y in 0 .. cursor_diff.first) {
                val new_y = y + opus_manager.get_y(
                    this.linking_beat!!.channel,
                    this.linking_beat!!.line_offset
                )

                for (x in 0 .. cursor_diff.second) {
                    val new_x = x + this.linking_beat!!.beat
                    for ((view, leaf_pos) in this.cache.get_all_leafs(new_y, new_x, listOf())) {
                        if (view !is LeafButton) {
                            continue
                        }

                        view.setFocused(true)
                        this.cache.addFocusedLeaf(new_y, new_x, leaf_pos)
                    }
                }
            }
        }
    }

    fun unapply_focus(opus_manager: OpusManager) {
        while (true) {
            val (y, x, cached_position) = this.cache.popFocusedLeaf() ?: break
            val view = this.cache.getTreeView(y, x, cached_position) ?: continue
            if (view !is LeafButton || y >= opus_manager.line_count()) {
                continue
            }

            try {
                view.setFocused(false)
                //TODO: check if attached instead of catching
            } catch (e: Exception) {
                continue
            }
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

    fun __tick_resize_beat_cell(channel: Int, line_offset: Int, beat: Int, new_width: Int) {
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

                param.width = (new_size * 120.toFloat()).toInt()
            } else {
                param.setMarginStart(5)
                param.setMarginEnd(5)
                param.width = (new_size * 120.toFloat()).toInt() - param.getMarginStart() - param.getMarginEnd()
            }

            current_view.layoutParams = param
        }
    }

    fun tick_resize_beats(updated_beats: List<Int>) {
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
}