package com.qfs.radixulous

import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.qfs.radixulous.databinding.FragmentMainBinding
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.FlagOperation
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.UpdateFlag
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private var active_context_menu_index: ContextMenu = ContextMenu.None

    var linking_beat: BeatKey? = null
    var linking_beat_b: BeatKey? = null
    private var relative_mode: Boolean = false

    private var _binding: FragmentMainBinding? = null
    internal var cache = ViewCache()

    private var block_default_return = false
    private var ticking: Boolean = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // TODO: Convert focus booleans to 1 enum SINGLE, ROW, COLUMN
    var focus_row: Boolean = false
    var focus_column: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        this.getMain().unlockDrawer()
        this.getMain().update_menu_options()
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
            this.block_default_return = true
            val main = this.getMain()

            bundle!!.getString("TITLE")?.let { title: String ->
                main.set_current_project_title( title )
            }

            bundle!!.getString("PATH")?.let { path: String ->
                this.takedownCurrent()

                main.getOpusManager().load(path)

                this.setContextMenu(ContextMenu.Leaf)
                this.tick()
            }
            main.update_menu_options()
            main.setup_config_drawer()
            main.cancel_reticle()
        }

        setFragmentResultListener("NEW") { _, bundle: Bundle? ->
            this.block_default_return = true
            this.newProject()
        }

        setFragmentResultListener("RETURNED") { _, bundle: Bundle? ->
            if (this.block_default_return) {
                this.block_default_return = false
                return@setFragmentResultListener
            }

            val main = this.getMain()
            this.takedownCurrent()
            main.getOpusManager().reset_cache()
            main.update_title_text()

            this.setContextMenu(ContextMenu.Leaf)
            this.tick()
            main.update_menu_options()
        }
    }

    override fun onStart() {
        super.onStart()
        val main = this.getMain()
        if (main.get_current_project_title() == null) {
            this.newProject()
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
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        if (opus_manager.has_history()) {
            opus_manager.apply_undo()
            this.tick()
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
        val textView: TextView = headerCellView.findViewById(R.id.textView)

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

    private fun buildLineView(channel: Int, line_offset: Int): TableRow {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        val tlOpusLines: TableLayout = this.activity!!.findViewById(R.id.tlOpusLines)
        val llLineLabels: LinearLayout = this.activity!!.findViewById(R.id.llLineLabels)

        val rowView: TableRow = LayoutInflater.from(tlOpusLines.context).inflate(
            R.layout.table_row,
            tlOpusLines,
            false
        ) as TableRow
        var y = opus_manager.get_y(channel, line_offset)
        tlOpusLines.addView(rowView, y)
        this.cache.cacheLine(rowView, channel, line_offset)

        for (i in 0 until opus_manager.opus_beat_count) {
            val wrapper = LayoutInflater.from(rowView.context).inflate(
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

        val rowLabelText = rowLabel.getChildAt(0) as TextView

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

    private fun buildTreeView(parent: ViewGroup, beatkey: BeatKey, position: List<Int>): View {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val tree = opus_manager.get_tree(beatkey, position)

        if (tree.is_leaf()) {
            val tvLeaf = LeafButton(parent.context, main, tree.get_event(), opus_manager.is_percussion(beatkey.channel))

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
            tvLeaf.layoutParams = param
            this.cache.cacheTree(tvLeaf, beatkey, position)
            return tvLeaf
        } else {
            val cellLayout: LinearLayout = LayoutInflater.from(parent.context).inflate(
                R.layout.tree_node,
                parent,
                false
            ) as LinearLayout

            this.cache.cacheTree(cellLayout, beatkey, position)

            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(cellLayout as ViewGroup, beatkey, new_position)
            }

            parent.addView(cellLayout)

            return cellLayout
        }
    }

    fun setContextMenu(menu_index: ContextMenu) {
        val main = this.getMain()
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
                    this.om_remove_line(1)
                }

                btnRemoveLine.setOnLongClickListener {
                    val main = this.getMain()
                    main.popup_number_dialog(
                        "Remove Lines",
                        1,
                        kotlin.math.max(1, opus_manager.line_count() - 1),
                        this::om_remove_line
                    )
                    true
                }

                btnInsertLine.setOnClickListener {
                    this.om_insert_line(1)
                }

                btnInsertLine.setOnLongClickListener {
                    val main = this.getMain()
                    main.popup_number_dialog(
                        "Insert Lines",
                        1,
                        9,
                        this::om_insert_line
                    )
                    true
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
                    this.om_insert_beat(1)
                }
                btnInsertBeat.setOnLongClickListener {
                    val main = this.getMain()
                    main.popup_number_dialog("Insert Beats", 1, 99, this::om_insert_beat)
                    true
                }

                btnRemoveBeat.setOnClickListener {
                    this.om_remove_beat(1)
                }

                btnRemoveBeat.setOnLongClickListener {
                    val main = this.getMain()
                    main.popup_number_dialog("Remove Beats", 1, opus_manager.opus_beat_count, this::om_remove_beat)
                    true
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
                    if (opus_manager.get_all_linked(cursor_key).size == 2) {
                        btnUnLinkAll.visibility = View.GONE
                    } else {

                        btnUnLinkAll.setOnClickListener {
                            this.interact_btnUnlinkAll(it)
                        }
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
        val main = this.getMain()
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
            this.om_split(2)
        }
        btnSplit.setOnLongClickListener {
            val main = this.getMain()
            main.popup_number_dialog("Split", 2, 29, this::om_split)
            true
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
                this.om_remove(1)
            }
            btnRemove.setOnLongClickListener {
                val main = this.getMain()
                // TODO: get max from tree
                main.popup_number_dialog("Remove", 1, 99, this::om_remove)
                true
            }
        }

        btnInsert.setOnClickListener {
            this.om_insert(1)
        }
        btnInsert.setOnLongClickListener {
            val main = this.getMain()
            main.popup_number_dialog("Insert", 1, 29, this::om_insert)
            true
        }
    }

    private fun resize_relative_value_selector() {
        val main = this.getMain()
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

    private fun rebuildBeatView(main_beatkey: BeatKey) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        for (beatkey in opus_manager.get_all_linked(main_beatkey)) {
            this.cache.removeBeatView(beatkey)

            val rowView = this.cache.getLine(main_beatkey.channel, main_beatkey.line_offset)

            val new_wrapper = LayoutInflater.from(rowView.context).inflate(
                R.layout.beat_node,
                rowView,
                false
            )

            rowView.addView(new_wrapper, beatkey.beat)
            this.buildTreeView(new_wrapper as ViewGroup, beatkey, listOf())
            new_wrapper.measure(0, 0)
            this.cache.set_column_width(beatkey.beat, new_wrapper.measuredWidth)
        }
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        val main = this.getMain()
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
        val main = this.getMain()
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

        this.tick()
    }

    // Wrapper around the OpusManager::set_event(). needed in order to validate proceding events
    private fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.set_event(beat_key, position, event)
    }

    private fun interact_btnUnset(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        this.setContextMenu(ContextMenu.Leaf)
        val cursor = opus_manager.get_cursor()
        val channel = cursor.get_beatkey().channel
        if (!opus_manager.is_percussion(channel) || opus_manager.get_tree_at_cursor().is_event()) {
            opus_manager.unset_at_cursor()
        } else {
            opus_manager.set_percussion_event_at_cursor()
        }

        this.tick()
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_btnUnlink(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.remove_link_from_network(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnUnlinkAll(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.clear_links_in_network(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnCancelLink(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.get_cursor().settle()
        this.linking_beat = null
        this.linking_beat_b = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_leafView_click(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        this.focus_column = false
        this.focus_row = false

        val (beatkey, position) = this.cache.getTreeViewPosition(view) ?: return
        opus_manager.set_cursor_position(beatkey, position)
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

        this.tick()
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_leafView_longclick(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val (beatkey, position) = this.cache.getTreeViewPosition(view) ?: return
        if (this.linking_beat == null) {
            this.linking_beat = beatkey
        } else {
            this.linking_beat_b = beatkey
        }
        opus_manager.set_cursor_position(beatkey, listOf())
        this.setContextMenu(ContextMenu.Linking)
        this.tick()
    }

    private fun interact_nsOffset(view: NumberSelector) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree_at_cursor()

        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()


        var event = OpusEvent(
            if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                val old_octave = event.note / event.radix

                (old_octave * event.radix) + progress
            } else {
                progress
            },
            opus_manager.RADIX,
            beatkey.channel,
            false
        )

        this.set_event(beatkey, position, event)

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_nsOctave(view: NumberSelector) {
        val main = this.getMain()
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

        this.set_event(beatkey, position, event)

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_rowLabel(view: View) {
        val main = this.getMain()
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

        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(abs_y, cursor.x, listOf())
        this.tick()
        this.setContextMenu(ContextMenu.Line)
    }

    private fun interact_sRelative_changed(view: View, isChecked: Boolean) {
        val main = this.getMain()
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
        this.tick()
    }

    private fun interact_nsRelativeValue(view: NumberSelector) {
        this.change_relative_value(view.getState()!!)
    }

    private fun interact_btnChoosePercussion(view: View) {
        val main = this.getMain()
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
            this.tick()
            val y = opus_manager.get_cursor().get_y()
            val textView: TextView = this.cache.getLineLabel(y)!!.findViewById(R.id.textView)
            textView.text = "P:${it.itemId}"
            this.setContextMenu(ContextMenu.Line) // TODO: overkill?
            true
        }
        popupMenu.show()
    }

    fun play_beat(beat: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        val midi = opus_manager.get_midi(beat, beat + 1)
        main.play_midi(midi)
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        val main = this.getMain()

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
        val main = this.getMain()
        val x = (view.parent as ViewGroup).indexOfChild(view)
        val opus_manager = main.getOpusManager()
        this.focus_column = true
        this.focus_row = false
        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(cursor.y, x, listOf())

        this.setContextMenu(ContextMenu.Beat)
        this.tick()
    }

    fun line_remove(channel: Int, line_offset: Int) {
        this.cache.detachLine(channel, line_offset)
    }

    fun line_new(channel: Int, line_offset: Int, beat_count: Int) {
        val rowView = this.buildLineView(channel, line_offset)
        for (x in 0 until beat_count) {
            this.buildTreeView(
                rowView.getChildAt(x) as ViewGroup,
                BeatKey(channel, line_offset, x),
                listOf()
            )
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

        this.cache.getLines().forEachIndexed { i: Int, rows: List<LinearLayout> ->
            rows.forEachIndexed { j: Int, rowView: LinearLayout ->
                val new_wrapper = LayoutInflater.from(rowView.context).inflate(
                    R.layout.beat_node,
                    rowView,
                    false
                ) as ViewGroup

                rowView.addView(new_wrapper, index)
                this.buildTreeView(new_wrapper, BeatKey(i, j, index), listOf())
            }
        }

        this.cache.add_column_width(index)
        this.update_column_label_size(index)
    }

    fun beat_remove(index: Int) {
        this.cache.detachColumnLabel()
        this.cache.getLines().forEachIndexed { i: Int, rows: List<LinearLayout> ->
            rows.forEachIndexed { j: Int, _: LinearLayout ->
                this.cache.removeBeatView(BeatKey(i, j, index))
            }
        }

        this.cache.remove_column_width(index)
    }

    fun beat_update(beatkey: BeatKey) {
        this.rebuildBeatView(beatkey)
        this.update_column_label_size(beatkey.beat)
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

    fun apply_focus(focused: Set<Pair<BeatKey, List<Int>>>, opus_manager: OpusManager) {
        for ((beatkey, position) in focused) {
            val linked_beats = opus_manager.get_all_linked(beatkey)
            for (linked_beat in linked_beats) {
                for ((view, leaf_pos) in this.cache.get_all_leafs(linked_beat, position)) {
                    if (view !is LeafButton) {
                        continue
                    }
                    view.setFocused(true)
                    this.cache.addFocusedLeaf(linked_beat, leaf_pos)
                }
            }
        }

        if (this.linking_beat_b != null) {
            val cursor_diff = opus_manager.get_cursor_difference(this.linking_beat!!, this.linking_beat_b!!)

            for (y in 0 .. cursor_diff.first) {
                for (x in 0 .. cursor_diff.second) {
                    var working_beat = BeatKey(
                        this.linking_beat!!.channel,
                        this.linking_beat!!.line_offset,
                        x + this.linking_beat!!.beat
                    )
                    for ((view, leaf_pos) in this.cache.get_all_leafs(working_beat, listOf())) {
                        if (view !is LeafButton) {
                            continue
                        }

                        view.setFocused(true)
                        this.cache.addFocusedLeaf(working_beat, leaf_pos)
                    }
                }
            }
        }
    }

    fun tick_unapply_focus() {
        var opus_manager = this.getMain().getOpusManager()

        while (true) {
            val (cached_beatkey, cached_position) = this.cache.popFocusedLeaf() ?: break
            val view = this.cache.getTreeView(cached_beatkey, cached_position) ?: continue
            if (view !is LeafButton
                || cached_beatkey.channel > opus_manager.channels.size - 1
                || cached_beatkey.line_offset > opus_manager.channels[cached_beatkey.channel].size - 1
            ) {
                continue
            }

            try {
                view.isFocused = false
                //TODO: check if attached instead of catching
            } catch (e: Exception) {
                continue
            }
        }
    }

    fun validate_leaf(beatkey: BeatKey, position: List<Int>, valid: Boolean) {
        val view = this.cache.getTreeView(beatkey, position) ?: return
        if (view is LeafButton) {
            view.setInvalid(!valid)
        }
    }

    private fun __tick_update_column_label_size(beat: Int) {
        val width = this.cache.get_column_width(beat)
        // Kludge: Need to remove/reattach label so it will shrink to a smaller
        // size if necessary
        val label_view = this.cache.getColumnLabel(beat)
        val label_row = label_view.parent as ViewGroup
        label_row.removeView(label_view)
        label_view.layoutParams.width = (width * 100) - 5
        label_row.addView(label_view, beat)
    }

    fun __tick_resize_beat_cell(channel: Int, line_offset: Int, beat: Int, new_width: Int) {
        val opus_manager = this.getMain().getOpusManager()
        val stack: MutableList<Pair<Float, List<Int>>> = mutableListOf(Pair(new_width.toFloat(), listOf()))
        val key = BeatKey(channel, line_offset, beat)
        while (stack.isNotEmpty()) {
            val (new_size, current_position) = stack.removeFirst()
            val current_tree = opus_manager.get_tree(key, current_position)

            val current_view = this.cache.getTreeView(key, current_position)
            val param = current_view!!.layoutParams as ViewGroup.MarginLayoutParams

            if (!current_tree.is_leaf()) {
                for (i in 0 until current_tree.size) {
                    val next_pos = current_position.toMutableList()
                    next_pos.add(i)
                    stack.add(Pair(new_size / current_tree.size.toFloat(), next_pos))
                }

                param.width = (new_size * 120.toFloat()).toInt()
            } else {
                param.marginStart = 5
                param.marginEnd = 5
                param.width = (new_size * 120.toFloat()).toInt() - param.marginStart - param.marginEnd
            }

            current_view.layoutParams = param
        }
    }

    fun tick_resize_beats(updated_beats: List<Int>) {
        val opus_manager = this.getMain().getOpusManager()
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

            for (channel in 0 until opus_manager.channels.size) {
                for (line_offset in 0 until opus_manager.channels[channel].size) {
                    this.__tick_resize_beat_cell(channel, line_offset, b, max_width)
                }
            }

            this.cache.set_column_width(b, max_width)
            this.__tick_update_column_label_size(b)
        }
    }

    private fun om_split(splits: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.split_tree_at_cursor(splits)

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun om_insert(count: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()

        val beatkey = opus_manager.get_cursor().get_beatkey()
        val position = opus_manager.get_cursor().get_position()

        if (position.isEmpty()) {
            opus_manager.split_tree_at_cursor(count + 1)
        } else {
            opus_manager.insert_after(beatkey, position, count)
        }

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun om_remove(count: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()

        val cursor = opus_manager.get_cursor()
        if (cursor.get_position().isNotEmpty()) {
            val beatkey = cursor.get_beatkey()
            val position = cursor.get_position()
            opus_manager.remove(beatkey, position, count)
        }
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun om_insert_line(count: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        var beatkey = opus_manager.get_cursor().get_beatkey()
        opus_manager.new_line(beatkey.channel, beatkey.line_offset + 1, count)
        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        this.tick()
    }

    private fun om_remove_line(count: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()

        if (opus_manager.line_count() > 1) {
            var beatkey = opus_manager.get_cursor().get_beatkey()
            opus_manager.remove_line(beatkey.channel, beatkey.line_offset, count)
        }

        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        this.tick()
    }

    private fun om_remove_beat(count: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()

        var cursor = opus_manager.get_cursor()
        opus_manager.remove_beat(cursor.get_beatkey().beat, count)
        this.tick()
    }

    private fun om_insert_beat(count: Int) {
        var main = this.getMain()
        val opus_manager = main.getOpusManager()
        var cursor = opus_manager.get_cursor()
        opus_manager.insert_beat(cursor.get_beatkey().beat, count)
        this.tick()
    }

    fun tick() {
        /*
        TODO: break this function up. Only a monolith since changing to handle all flags in
            the exact order they were created
         */
        if (!this.ticking) {
            this.ticking = true
            var main = this.getMain()
            val opus_manager = main.getOpusManager()
            this.tick_unapply_focus()

            val updated_beats: MutableSet<Int> = mutableSetOf()
            var min_changed_beat = opus_manager.opus_beat_count
            var validate_count = 0
            while (true) {
                when (opus_manager.fetch_next_flag()) {
                    UpdateFlag.AbsVal -> {
                        //Validation has to happen after all beat updates
                        validate_count += 1
                    }
                    UpdateFlag.Beat -> {
                        val (index, operation) = opus_manager.fetch_flag_beat() ?: break
                        min_changed_beat = Integer.min(min_changed_beat, index)

                        when (operation) {
                            FlagOperation.New -> {
                                this.beat_new(index)
                                updated_beats.add(index)
                            }
                            FlagOperation.Pop -> {
                                this.beat_remove(index)
                            }
                        }
                    }
                    UpdateFlag.BeatMod -> {
                        val beatkey = opus_manager.fetch_flag_change() ?: break
                        this.beat_update(beatkey)

                        for (linked_beatkey in opus_manager.get_all_linked(beatkey)) {
                            updated_beats.add(linked_beatkey.beat)
                        }

                    }
                    UpdateFlag.Line -> {
                        var line_flag = opus_manager.fetch_flag_line() ?: break
                        when (line_flag.operation) {
                            FlagOperation.Pop -> {
                                this.line_remove(line_flag.channel, line_flag.line)
                            }
                            FlagOperation.New -> {
                                this.line_new(line_flag.channel, line_flag.line, line_flag.beat_count)
                            }
                        }
                    }
                    null -> {
                        break
                    }
                }

            }

            this.line_update_labels(opus_manager)

            this.tick_resize_beats(updated_beats.toList())
            for (index in min_changed_beat until opus_manager.opus_beat_count) {
                this.update_column_label_size(index)
            }

            for (b in updated_beats) {
                this.update_column_label_size(b)
            }

            for (i in 0 until validate_count) {
                val (beatkey, position) = opus_manager.fetch_flag_absolute_value() ?: break
                var abs_value = opus_manager.get_absolute_value(beatkey, position) ?: continue
                this.validate_leaf(beatkey, position, abs_value in 0..95)

            }

            this.tick_apply_focus()
            this.ticking = false
        }
    }

    private fun tick_apply_focus() {
        val opus_manager = this.getMain().getOpusManager()
        val cursor = opus_manager.get_cursor()
        val focused: MutableSet<Pair<BeatKey, List<Int>>> = mutableSetOf()
        if (this.focus_column) {
            for (y in 0 until opus_manager.line_count()) {
                val (channel, index) = opus_manager.get_channel_index(y)
                focused.add(
                    Pair(
                        BeatKey(channel, index, cursor.get_beatkey().beat),
                        listOf()
                    )
                )
            }
        } else if (this.focus_row) {
            for (x in 0 until opus_manager.opus_beat_count) {
                val beatkey = cursor.get_beatkey()
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

        this.apply_focus(focused, opus_manager)
    }

    fun newProject() {
        var main = this.getMain()
        main.newProject()

        this.takedownCurrent()
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }
}