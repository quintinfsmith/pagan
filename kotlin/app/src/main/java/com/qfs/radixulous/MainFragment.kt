package com.qfs.radixulous

import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.databinding.FragmentMainBinding
import com.qfs.radixulous.opusmanager.*
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {
    private var active_context_menu_index: ContextMenu = ContextMenu.None
    private var relative_mode: Int = 0

    private var _binding: FragmentMainBinding? = null
    internal var cache = ViewCache()

    private var ticking: Boolean = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        this.getMain().unlockDrawer()
        this.getMain().update_menu_options()
        return binding.root
    }

    //override fun onSaveInstanceState(savedInstanceState: Bundle) {
    //    savedInstanceState.putString("BUTTS?", "Butts!")
    //    println("SAVEING!!------------------")
    //    super.onSaveInstanceState(savedInstanceState)
    //}

    //override fun onPause() {
    //    println("PAUSED!!")
    //    super.onPause()
    //}
    override fun onResume() {
        val rvBeatTable = this.binding.root.findViewById<RecyclerView>(R.id.rvBeatTable)
        var rvBeatTable_adapter = rvBeatTable.adapter as OpusManagerAdapter
        val rvRowLabels = this.binding.root.findViewById<RecyclerView>(R.id.rvRowLabels)
        var rvRowLabels_adapter = rvRowLabels.adapter as RowLabelAdapter

        var opus_manager = this.getMain().getOpusManager()
        if (rvRowLabels_adapter.itemCount == 0) {
            opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
                channel.lines.forEachIndexed { j: Int, line: List<OpusTree<OpusEvent>> ->
                    rvRowLabels_adapter.addRowLabel()
                }
            }

            for (i in 0 until opus_manager.opus_beat_count) {
                rvBeatTable_adapter.addBeatColumn(i)
            }
            // TODO: Fix this kludge. A partially-visible first item will not render, so i'm just
            // Scrolling back to the beginning for now
            rvBeatTable_adapter.scrollToPosition(0)
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvBeatTable = view.findViewById<RecyclerView>(R.id.rvBeatTable)
        val svTable: ScrollView = view.findViewById(R.id.svTable)
        val rvColumnLabels = view.findViewById<RecyclerView>(R.id.rvColumnLabels)
        val rvRowLabels = view.findViewById<RecyclerView>(R.id.rvRowLabels)

        RowLabelAdapter(this, rvRowLabels)
        OpusManagerAdapter(this, rvBeatTable, ColumnLabelAdapter(this, rvColumnLabels))

        svTable.viewTreeObserver.addOnScrollChangedListener {
            (rvRowLabels.adapter as RowLabelAdapter).scrollToY(svTable.scrollY)
        }

        setFragmentResultListener("LOAD") { _, bundle: Bundle? ->
            val main = this.getMain()

            bundle!!.getString("TITLE")?.let { title: String ->
                main.set_current_project_title(title)
            }

            bundle!!.getString("PATH")?.let { path: String ->

                main.getOpusManager().load(path)

                this.setContextMenu(ContextMenu.Leaf)
                this.tick()
            }
            main.update_menu_options()
            main.setup_config_drawer()
            main.cancel_reticle()
        }
        setFragmentResultListener("IMPORT") { _, bundle: Bundle? ->
            val main = this.getMain()
            main.loading_reticle()
            bundle!!.getString("URI")?.let { path ->
                main.import_midi(path)
                this.setContextMenu(ContextMenu.Leaf)
                this.tick()
            }
        }

        setFragmentResultListener("NEW") { _, bundle: Bundle? ->
            this.newProject()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    internal fun getMain(): MainActivity {
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

                if (opus_manager.channels[channel].size == 1) {
                    btnRemoveLine.visibility = View.GONE
                } else {
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

        val rosRelativeOption = view.findViewById<RelativeOptionSelector>(R.id.rosRelativeOption)

        val btnUnset = view.findViewById<ImageView>(R.id.btnUnset)
        val btnSplit = view.findViewById<View>(R.id.btnSplit)
        val btnRemove = view.findViewById<View>(R.id.btnRemove)
        val btnInsert = view.findViewById<View>(R.id.btnInsert)

        val nsOctave: NumberSelector = view.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = view.findViewById(R.id.nsOffset)

        val current_tree = opus_manager.get_tree_at_cursor()
        // If event exists, change relative mode, other wise use active relative mode
        if (current_tree.is_event()) {
            var event = current_tree.get_event()!!
            this.relative_mode = if (event.relative) {
                if (event.note >= 0) {
                    1
                } else {
                    2
                }
            } else {
                0
            }
        }

        val cursor = opus_manager.get_cursor()
        if (opus_manager.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())) {
            rosRelativeOption.visibility = View.VISIBLE
        } else {
            this.relative_mode = 0
            rosRelativeOption.visibility = View.GONE
        }

        this.validate_rosRelativeOption()

        rosRelativeOption.setState(this.relative_mode, true)

        if (opus_manager.is_percussion(cursor.get_beatkey().channel)) {
            nsOctave.visibility = View.GONE
            nsOffset.visibility = View.GONE
            rosRelativeOption.visibility = View.GONE

            if (!opus_manager.get_tree_at_cursor().is_event()) {
                btnUnset.setImageResource(R.drawable.set_percussion)
            }

            btnUnset.setOnClickListener {
                this.interact_btnUnset(it)
            }

        } else {
            if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                var value = if (event.note < 0) {
                    0 - event.note
                } else {
                    event.note
                }
                nsOffset.setState(value % event.radix, true, true)
                nsOctave.setState(value / event.radix, true, true)
            }

            nsOffset.setOnChange(this::interact_nsOffset)
            nsOctave.setOnChange(this::interact_nsOctave)
            rosRelativeOption.setOnChange(this::interact_rosRelativeOption)
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

    private fun validate_rosRelativeOption() {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val rosRelativeOption = main.findViewById<RelativeOptionSelector>(R.id.rosRelativeOption) ?: return
        val cursor = opus_manager.get_cursor()
        val abs_value = opus_manager.get_absolute_value(cursor.get_beatkey(), cursor.get_position())
        if (abs_value != null && (abs_value > 127 || abs_value < 0)) {
            rosRelativeOption.hideOption(0)
        } else {
            rosRelativeOption.unhideOption(0)
        }

    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val current_tree = opus_manager.get_tree_at_cursor()

        this.relative_mode = view.getState()!!

        var event = current_tree.get_event() ?: return
        var cursor = opus_manager.get_cursor()

        val nsOctave: NumberSelector = main.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = main.findViewById(R.id.nsOffset)

        when (this.relative_mode) {
            0 -> {
                if (event.relative) {
                    opus_manager.convert_event_at_cursor_to_absolute()
                    event = current_tree.get_event()!!
                }
                nsOctave.setState(event.note / event.radix, true, true)
                nsOffset.setState(event.note % event.radix, true, true)
            }
            1 -> {
                if (!event.relative) {
                    opus_manager.convert_event_at_cursor_to_relative()
                    event = current_tree.get_event()!!
                }
                if (event.note < 0) {
                    nsOctave.unset_active_button()
                    nsOffset.unset_active_button()
                } else {
                    nsOctave.setState(event.note / event.radix, true, true)
                    nsOffset.setState(event.note % event.radix, true, true)
                }

            }
            2 -> {
                if (!event.relative) {
                    opus_manager.convert_event_at_cursor_to_relative()
                    event = current_tree.get_event()!!
                }
                if (event.note > 0) {
                    nsOctave.unset_active_button()
                    nsOffset.unset_active_button()
                } else {
                    nsOctave.setState((0 - event.note) / event.radix, true, true)
                    nsOffset.setState((0 - event.note) % event.radix, true, true)
                }
            }
        }

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

        this.unset_cursor_position()
        opus_manager.unlink_beat(cursor.get_beatkey())
        this.set_cursor_position(cursor.y, cursor.x, cursor.get_position(), FocusType.Cell)
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_btnUnlinkAll(view: View) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        val cursor = opus_manager.get_cursor()
        opus_manager.clear_link_pool(cursor.get_beatkey())
        this.set_cursor_position(cursor.y, cursor.x, cursor.get_position(), FocusType.Cell)

        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_btnCancelLink(view: View) {
        var opus_manager = this.getMain().getOpusManager()
        var cursor = opus_manager.get_cursor()
        // Cancelling the linking is handled in set_cursor_position
        this.set_cursor_position(cursor.y, cursor.x, cursor.get_position(), FocusType.Cell)
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_nsOffset(view: NumberSelector) {
        val main = this.getMain()
        main.stop_playback()
        val opus_manager = main.getOpusManager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree_at_cursor()

        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()

        var value = if (current_tree.is_event()) {
            var event = current_tree.get_event()!!
            when (this.relative_mode) {
                2 -> {
                    0 - ((((0 - event.note) / event.radix) * event.radix) + progress)
                }
                else -> {
                    ((event.note / event.radix) * event.radix) + progress
                }
            }
        } else {
            when (this.relative_mode) {
                2 -> {
                    0 - progress
                }
                else -> {
                    progress
                }
            }
        }

        val event = OpusEvent(
            value,
            opus_manager.RADIX,
            beatkey.channel,
            this.relative_mode != 0
        )

        this.set_event(beatkey, position, event)
        this.validate_rosRelativeOption()

        //this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_nsOctave(view: NumberSelector) {
        val main = this.getMain()
        main.stop_playback()
        val opus_manager = main.getOpusManager()
        val progress = view.getState() ?: return

        val current_tree = opus_manager.get_tree_at_cursor()
        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()

        var value = if (current_tree.is_event()) {
            var event = current_tree.get_event()!!
            when (this.relative_mode) {
                2 -> {
                    0 - (((0 - event.note) % event.radix) + (progress * event.radix))
                }
                else -> {
                    ((event.note % event.radix) + (progress * event.radix))
                }
            }
        } else {
            when (this.relative_mode) {
                2 -> {
                    (0 - progress) * opus_manager.RADIX
                }
                else -> {
                    (progress * opus_manager.RADIX)
                }
            }
        }

        val event = OpusEvent(
            value,
            opus_manager.RADIX,
            beatkey.channel,
            this.relative_mode != 0
        )

        this.set_event(beatkey, position, event)
        this.validate_rosRelativeOption()

        //this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnChoosePercussion(view: View) {
        val main = this.getMain()
        main.stop_playback()
        val opus_manager = main.getOpusManager()

        val popupMenu = PopupMenu(this.binding.root.context, view)
        val cursor = opus_manager.get_cursor()
        val drums = resources.getStringArray(R.array.midi_drums)
        val preset = main.soundfont.get_preset(0, 128)
        val available_drum_keys = mutableSetOf<Int>()

        for (preset_instrument in preset.instruments) {
            if (preset_instrument.instrument == null) {
                continue
            }

            for (sample in preset_instrument.instrument!!.samples) {
                if (sample.key_range == null) {

                } else {
                    for (j in sample.key_range!!.first .. sample.key_range!!.second) {
                        available_drum_keys.add(j)
                    }
                }
            }
        }

        val line_map = opus_manager.channels[cursor.get_beatkey().channel].line_map
        if (line_map != null) {
            for ((offset, instrument) in line_map) {
                if (instrument + 27 in available_drum_keys) {
                    available_drum_keys.remove(instrument + 27)
                }
            }
        }
        drums.forEachIndexed { i, string ->
            if ((i + 27) in available_drum_keys) {
                popupMenu.menu.add(0, i, i, "$i: $string")
            }
        }

        popupMenu.setOnMenuItemClickListener {
            opus_manager.set_percussion_instrument(
                cursor.get_beatkey().line_offset,
                it.itemId
            )

            this.tick()
            this.setContextMenu(ContextMenu.Line) // TODO: overkill?

            this.update_line_labels()
            true
        }

        popupMenu.show()
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        val main = this.getMain()
        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvColumnLabels = main.findViewById<RecyclerView>(R.id.rvColumnLabels)

        (rvBeatTable.adapter as OpusManagerAdapter).scrollToPosition(beat)

        if (select) {
            val opus_manager = main.getOpusManager()
            val cursor = opus_manager.get_cursor()
            this.set_cursor_position(cursor.y, beat, listOf(), FocusType.Column)
        }
    }


    fun select_column(x: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        val cursor = opus_manager.get_cursor()
        this.set_cursor_position(cursor.y, x, listOf(), FocusType.Column)

        this.setContextMenu(ContextMenu.Beat)
    }

    fun refresh_leaf_labels(beats: Set<Int>? = null) {
        val beat_table = this.getMain().findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = beat_table.adapter as OpusManagerAdapter
        rvBeatTable_adapter.refresh_leaf_labels(beats)
    }

    fun update_line_labels() {
        val rvRowLabels = this.getMain().findViewById<RecyclerView>(R.id.rvRowLabels)
        val rvRowLabels_adapter = rvRowLabels.adapter as RowLabelAdapter
        var start = (rvRowLabels.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        var end = (rvRowLabels.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        for (i in start .. end) {
            rvRowLabels_adapter.notifyItemChanged(i)
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



    private fun om_split(splits: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        opus_manager.split_tree_at_cursor(splits)

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun om_insert(count: Int) {
        val main = this.getMain()
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
        val main = this.getMain()
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
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val beatkey = opus_manager.get_cursor().get_beatkey()
        opus_manager.new_line(beatkey.channel, beatkey.line_offset + 1, count)
        this.setContextMenu(ContextMenu.Line) // TODO: overkill?
        this.tick()
    }

    private fun om_remove_line(count: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        if (opus_manager.line_count() > 1) {
            val beatkey = opus_manager.get_cursor().get_beatkey()
            opus_manager.remove_line(beatkey.channel, beatkey.line_offset, count)
        }

        this.tick()
    }

    private fun om_remove_beat(count: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        val cursor = opus_manager.get_cursor()
        opus_manager.remove_beat(cursor.get_beatkey().beat, count)
        this.tick()
    }

    private fun om_insert_beat(count: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.insert_beat(cursor.get_beatkey().beat + 1, count)
        this.tick()
    }

    fun tick() {
        if (!this.ticking) {
            this.ticking = true
            val main = this.getMain()

            val beat_table = main.findViewById<RecyclerView>(R.id.rvBeatTable)
            val rvBeatTable_adapter = beat_table.adapter as OpusManagerAdapter
            val rvRowLabels_adapter = main.findViewById<RecyclerView>(R.id.rvRowLabels).adapter as RowLabelAdapter

            val opus_manager = main.getOpusManager()

            val updated_beats: MutableSet<Int> = mutableSetOf()
            var min_changed_beat = opus_manager.opus_beat_count
            var validate_count = 0
            var lines_need_update = false


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
                                rvBeatTable_adapter.addBeatColumn(index)
                            }
                            FlagOperation.Pop -> {
                                rvBeatTable_adapter.removeBeatColumn(index)
                            }
                        }
                    }

                    UpdateFlag.BeatMod -> {
                        val beatkey = opus_manager.fetch_flag_change() ?: break
                        for (linked_beatkey in opus_manager.get_all_linked(beatkey)) {
                            updated_beats.add(linked_beatkey.beat)
                        }
                    }

                    UpdateFlag.Line -> {
                        val line_flag = opus_manager.fetch_flag_line() ?: break
                        lines_need_update = true
                        for (i in 0 until line_flag.beat_count ) {
                            updated_beats.add(i)
                        }

                        when (line_flag.operation) {
                            FlagOperation.Pop -> {
                                rvRowLabels_adapter.removeRowLabel(line_flag.line)
                            }
                            FlagOperation.New -> {
                                rvRowLabels_adapter.addRowLabel()
                            }
                        }
                    }

                    UpdateFlag.Clear -> {
                        var clear_flag = opus_manager.fetch_flag_clear() ?: break

                        var channel_offset = 0
                        clear_flag.first.forEachIndexed { _: Int, j: Int ->
                            channel_offset += j
                        }
                        for (i in channel_offset - 1 downTo 0) {
                            rvRowLabels_adapter.removeRowLabel(i)
                        }

                        for (i in 0 until clear_flag.second) {
                            rvBeatTable_adapter.removeBeatColumn((clear_flag.second - 1) - i)
                        }

                        updated_beats.clear()
                    }

                    null -> {
                        break
                    }
                }
            }

            if (lines_need_update) {
                this.line_update_labels(opus_manager)
            }

            this.refresh_leaf_labels(updated_beats)

            this.ticking = false
        }
    }

    fun newProject() {
        val main = this.getMain()
        main.newProject()

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    fun scrollTo(beatkey: BeatKey, position: List<Int>) {
        val leafView: View? = this.cache.getTreeView(beatkey, position) ?: return
        if (leafView !is LeafButton) {
            return
        }

        val hsvTable = this.getMain().findViewById<RecyclerView>(R.id.rvBeatTable)
        val svTable = this.getMain().findViewById<ScrollView>(R.id.svTable)

        val offsetViewBounds = Rect()
        // TODO: FIX this KLUDGE. I don't know of a way to use a callback here offhand
        while (true) {
            leafView!!.getGlobalVisibleRect(offsetViewBounds);
            if (offsetViewBounds.width() != 0) {
                break
            } else {
                Thread.sleep(10)
            }
        }

        val offset_left = offsetViewBounds.left
        val offset_top = offsetViewBounds.top

        if (offset_left < hsvTable.scrollX || offset_left > hsvTable.scrollX + svTable.width) {
            hsvTable.scrollTo(offset_left, 0)
        }
        if (offset_top < svTable.scrollY || offset_top > svTable.scrollY + svTable.width) {
            svTable.smoothScrollTo(0, offset_top)
        }
    }

    fun set_active_row(y: Int) {
        val main = this.getMain()
        main.stop_playback()

        val opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        this.set_cursor_position(y, cursor.x, listOf(), FocusType.Row)

        this.setContextMenu(ContextMenu.Line)
    }

    fun get_label_text(y: Int): String {
        val opus_manager = this.getMain().getOpusManager()
        val (channel, line_offset) = opus_manager.get_channel_index(y)
        return if (!opus_manager.is_percussion(channel)) {
            if (line_offset == 0) {
                "$channel:$line_offset"
            } else {
                "  :$line_offset"
            }
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            "P:$instrument"
        }
    }

    fun move_line(y_from: Int, y_to: Int) {
        var opus_manager = this.getMain().getOpusManager()
        var (channel_from, line_from) = opus_manager.get_channel_index(y_from)
        var (channel_to, line_to) = opus_manager.get_channel_index(y_to)

        if (channel_to != channel_from) {
            line_to += 1
        }


        opus_manager.move_line(channel_from, line_from, channel_to, line_to)

        this.tick()
    }

    fun set_cursor_position(y: Int, x: Int, position: List<Int>, type: FocusType = FocusType.Cell) {
        val main = this.getMain()
        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        var adapter = rvBeatTable.adapter as OpusManagerAdapter
        val opus_manager = main.getOpusManager()

        adapter.unset_cursor_position()

        opus_manager.set_cursor_position(y, x, position)

        adapter.set_cursor_position(y, x, position, type)
    }
    fun unset_cursor_position() {
        val rvBeatTable = this.getMain().findViewById<RecyclerView>(R.id.rvBeatTable)
        var adapter = rvBeatTable.adapter as OpusManagerAdapter
        adapter.unset_cursor_position()
    }

    fun set_channel_instrument(channel: Int, instrument: Int) {
        var main = this.getMain()
        var opus_manager = main.getOpusManager()
        opus_manager.set_channel_instrument(channel, instrument)
        main.update_channel_instruments(opus_manager)
        //this.update_line_labels()
        //this.refresh_leaf_labels()
    }

}