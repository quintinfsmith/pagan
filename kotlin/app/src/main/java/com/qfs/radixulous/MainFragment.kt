package com.qfs.radixulous

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.databinding.FragmentMainBinding
import com.qfs.radixulous.opusmanager.*
import com.qfs.radixulous.BeatColumnAdapter.FocusType
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : TempNameFragment() {
    // Boiler Plate //
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    //////////////////

    private var active_context_menu_index: ContextMenu? = null

    private var table_offset_pause: Int = 0

    enum class ContextMenu {
        Leaf,
        Line,
        Column,
        Linking
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        this.get_main().apply {
            unlockDrawer()
            update_menu_options()
        }

        return binding.root
    }

    override fun onPause() {
        val rvBeatTable = this.binding.root.findViewById<RecyclerView>(R.id.rvBeatTable)
        this.table_offset_pause = rvBeatTable.computeHorizontalScrollOffset()
        super.onPause()
    }


    override fun onResume() {
        val rvBeatTable = this.binding.root.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = rvBeatTable.adapter as BeatColumnAdapter
        val rvLineLabels = this.binding.root.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelAdapter

        val opus_manager = this.get_main().get_opus_manager()
        if (rvLineLabels_adapter.itemCount == 0) {
            opus_manager.channels.forEach { channel: OpusChannel ->
                channel.lines.forEach { _: OpusChannel.OpusLine ->
                    rvLineLabels_adapter.addLineLabel()
                }
            }
        }
        if (rvBeatTable_adapter.itemCount == 0) {
            // Kludge AF. Using these 2 threads  is the only way i could get the first item
            // Rendered when hitting back from load
            thread {
                this.get_main().runOnUiThread {
                    for (i in 0 until opus_manager.opus_beat_count) {
                        rvBeatTable_adapter.addBeatColumn(i)
                    }
                }
            }
        }


        this.get_main().update_title_text()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvBeatTable = view.findViewById<RecyclerView>(R.id.rvBeatTable)
        val svTable: ScrollView = view.findViewById(R.id.svTable)
        val rvColumnLabels = view.findViewById<RecyclerView>(R.id.rvColumnLabels)
        val rvLineLabels = view.findViewById<RecyclerView>(R.id.rvLineLabels)

        LineLabelAdapter(this, rvLineLabels)
        BeatColumnAdapter(this, rvBeatTable, ColumnLabelAdapter(this, rvColumnLabels))

        svTable.viewTreeObserver.addOnScrollChangedListener {
            (rvLineLabels.adapter as LineLabelAdapter).scrollToLine(svTable.scrollY)
        }

        setFragmentResultListener("LOAD") { _, bundle: Bundle? ->
            if (bundle == null) {
                return@setFragmentResultListener
            }

            bundle.getString("PATH")?.let { path: String ->
                val main = this.get_main()
                main.get_opus_manager().load(path)
            }
        }

        setFragmentResultListener("IMPORT") { _, bundle: Bundle? ->
            bundle!!.getString("URI")?.let { path ->
                val main = this.get_main()
                main.import_midi(path)
            }
        }

        setFragmentResultListener("NEW") { _, _: Bundle? ->
            val main = this.get_main()
            main.get_opus_manager().new()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    fun reset_context_menu() {
        when (this.active_context_menu_index) {
            ContextMenu.Leaf -> {
                this.setContextMenu_leaf()
            }
            ContextMenu.Line -> {
                this.setContextMenu_line()
            }
            ContextMenu.Column -> {
                this.setContextMenu_column()
            }
            ContextMenu.Linking -> {
                this.setContextMenu_linking()
            }
            null -> { }
        }
    }

    fun clearContextMenu() {
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)
        llContextMenu.removeAllViews()
        this.active_context_menu_index = null
    }

    internal fun setContextMenu_linking() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

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
        this.active_context_menu_index = ContextMenu.Linking
    }

    private fun setContextMenu_column() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

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
            val main = this.get_main()
            main.popup_number_dialog("Insert Beats", 1, 99, this::om_insert_beat)
            true
        }

        btnRemoveBeat.setOnClickListener {
            this.om_remove_beat(1)
        }

        btnRemoveBeat.setOnLongClickListener {
            val main = this.get_main()
            main.popup_number_dialog("Remove Beats", 1, opus_manager.opus_beat_count, this::om_remove_beat)
            true
        }

        llContextMenu.addView(view)
        this.active_context_menu_index = ContextMenu.Column
    }

    private fun setContextMenu_line() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_row,
            llContextMenu,
            false
        )
        val sbLineVolume = view.findViewById<SeekBar>(R.id.sbLineVolume)
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
                val main = this.get_main()
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
            val main = this.get_main()
            main.popup_number_dialog(
                "Insert Lines",
                1,
                9,
                this::om_insert_line
            )
            true
        }

        sbLineVolume.progress = opus_manager.get_line_volume(beatkey.channel, beatkey.line_offset)
        sbLineVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) { }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                opus_manager.set_line_volume(beatkey.channel, beatkey.line_offset, seekbar.progress)
            }
        })

        llContextMenu.addView(view)
        this.active_context_menu_index = ContextMenu.Line
    }

    internal fun setContextMenu_leaf() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu: LinearLayout = this.activity!!.findViewById(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_cell,
            llContextMenu,
            false
        )
        llContextMenu.addView(view)

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
            val event = current_tree.get_event()!!
            opus_manager.set_relative_mode(event)
        }

        val cursor = opus_manager.get_cursor()

        rosRelativeOption.setState(opus_manager.relative_mode, true)

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
                val value = if (event.note < 0) {
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
            val main = this.get_main()
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
                opus_manager.clear_parent_at_cursor()
                true
            }
        }

        btnInsert.setOnClickListener {
            this.om_insert(1)
        }

        btnInsert.setOnLongClickListener {
            val main = this.get_main()
            main.popup_number_dialog("Insert", 1, 29, this::om_insert)
            true
        }

        this.active_context_menu_index = ContextMenu.Leaf
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val current_tree = opus_manager.get_tree_at_cursor()

        opus_manager.relative_mode = view.getState()!!

        var event = current_tree.get_event() ?: return

        val nsOctave: NumberSelector = main.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = main.findViewById(R.id.nsOffset)

        when (opus_manager.relative_mode) {
            0 -> {
                if (event.relative) {
                    try {
                        opus_manager.convert_event_at_cursor_to_absolute()
                        event = current_tree.get_event()!!
                    } catch (e: Exception) {
                        event.note = 0
                        event.relative = false
                        opus_manager.set_event_at_cursor(event.copy())
                    }
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
                    nsOctave.setState(
                        (0 - event.note) / event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                    nsOffset.setState(
                        (0 - event.note) % event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                }
            }
        }
    }

    private fun interact_btnUnset(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.get_cursor()
        val channel = cursor.get_beatkey().channel
        if (!opus_manager.is_percussion(channel) || opus_manager.get_tree_at_cursor().is_event()) {
            opus_manager.unset_at_cursor()
        } else {
            opus_manager.set_percussion_event_at_cursor()
        }
        this.reset_context_menu()
    }

    private fun interact_btnUnlink(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.get_cursor()

        this.unset_cursor_position()
        opus_manager.unlink_beat(cursor.get_beatkey())
        this.set_cursor_position(cursor.y, cursor.x, cursor.get_position(), FocusType.Cell)
    }

    private fun interact_btnUnlinkAll(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.get_cursor()
        opus_manager.clear_link_pool(cursor.get_beatkey())
        this.set_cursor_position(cursor.y, cursor.x, cursor.get_position(), FocusType.Cell)

        this.setContextMenu_leaf()
    }

    private fun interact_btnCancelLink(view: View) {
        val opus_manager = this.get_main().get_opus_manager()
        val cursor = opus_manager.get_cursor()
        // Cancelling the linking is handled in set_cursor_position
        this.set_cursor_position(cursor.y, cursor.x, cursor.get_position(), FocusType.Cell)
        this.setContextMenu_leaf()
    }

    private fun interact_nsOffset(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree_at_cursor()

        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            var prev_note = if (opus_manager.relative_mode != 0) {
                val nsOctave = this.get_main().findViewById<NumberSelector>(R.id.nsOctave)
                if (nsOctave.getState() == null) {
                    nsOctave.setState(0, true, true)
                    0
                } else {
                    event.note
                }
            } else {
                event.note
            }

            when (opus_manager.relative_mode) {
                2 -> {
                    if (prev_note > 0) {
                        prev_note *= -1
                    }
                    ((prev_note / event.radix) * event.radix) - progress
                }
                else -> {
                    ((prev_note / event.radix) * event.radix) + progress
                }
            }
        } else {
            when (opus_manager.relative_mode) {
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
            opus_manager.relative_mode != 0
        )

        opus_manager.set_event(beatkey, position, event)
        this.reset_context_menu()
    }

    private fun interact_nsOctave(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState() ?: return

        val current_tree = opus_manager.get_tree_at_cursor()
        val cursor = opus_manager.get_cursor()
        val position = cursor.position
        val beatkey = cursor.get_beatkey()

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            var prev_note = if (opus_manager.relative_mode != 0) {
                val nsOffset  = this.get_main().findViewById<NumberSelector>(R.id.nsOffset)
                if (nsOffset.getState() == null) {
                    nsOffset.setState(0, true, true)
                    0
                } else {
                    event.note
                }
            } else {
                event.note
            }

            when (opus_manager.relative_mode) {
                2 -> {
                    0 - (((0 - prev_note) % event.radix) + (progress * event.radix))
                }
                else -> {
                    ((prev_note % event.radix) + (progress * event.radix))
                }
            }
        } else {
            when (opus_manager.relative_mode) {
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
            opus_manager.relative_mode != 0
        )

        opus_manager.set_event(beatkey, position, event)
        this.reset_context_menu()
    }

    private fun interact_btnChoosePercussion(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

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
                if (sample.key_range != null) {
                    for (j in sample.key_range!!.first .. sample.key_range!!.second) {
                        available_drum_keys.add(j)
                    }
                }
            }
        }

        val line_map = opus_manager.channels[cursor.get_beatkey().channel].line_map
        if (line_map != null) {
            for ((_, instrument) in line_map) {
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
            true
        }

        popupMenu.show()
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        val main = this.get_main()
        main.runOnUiThread {
            val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
            (rvBeatTable.adapter as BeatColumnAdapter).scrollToPosition(beat)
        }

        if (select) {
            val opus_manager = main.get_opus_manager()
            val cursor = opus_manager.get_cursor()
            this.set_cursor_position(cursor.y, beat, listOf(), FocusType.Column)
        }
    }

    fun select_column(x: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.get_cursor()
        this.set_cursor_position(cursor.y, x, listOf(), FocusType.Column)

        this.setContextMenu_column()
    }

    private fun om_split(splits: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.split_tree_at_cursor(splits)
        this.reset_context_menu()
    }

    private fun om_insert(count: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val beatkey = opus_manager.get_cursor().get_beatkey()
        val position = opus_manager.get_cursor().get_position()

        if (position.isEmpty()) {
            opus_manager.split_tree_at_cursor(count + 1)
        } else {
            opus_manager.insert_after(beatkey, position, count)
        }
        this.reset_context_menu()
    }

    private fun om_remove(count: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.get_cursor()
        if (cursor.get_position().isNotEmpty()) {
            val beatkey = cursor.get_beatkey()
            val position = cursor.get_position()
            opus_manager.remove(beatkey, position, count)
        }
        this.reset_context_menu()
    }

    private fun om_insert_line(count: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val beatkey = opus_manager.get_cursor().get_beatkey()
        opus_manager.new_line(beatkey.channel, beatkey.line_offset + 1, count)

        this.reset_context_menu()
    }

    private fun om_remove_line(count: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        if (opus_manager.line_count() > 1) {
            val beatkey = opus_manager.get_cursor().get_beatkey()
            opus_manager.remove_line(beatkey.channel, beatkey.line_offset, count)
        }
        this.reset_context_menu()
    }

    private fun om_remove_beat(count: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val cursor = opus_manager.get_cursor()
        opus_manager.remove_beat(cursor.get_beatkey().beat, count)
    }

    private fun om_insert_beat(count: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.get_cursor()
        opus_manager.insert_beat(cursor.get_beatkey().beat + 1, count)
        opus_manager.cursor_right()
    }

    // TODO: Consider Y
    fun is_leaf_visible(beatkey: BeatKey, position: List<Int>): Boolean {
        val main = this.get_main()
        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = rvBeatTable.adapter as BeatColumnAdapter
        val tree_view = rvBeatTable_adapter.get_leaf_view(beatkey, position)
        return (tree_view != null)
    }

    // If the position isn't on screen, scroll to it
    fun scrollTo(beatkey: BeatKey, position: List<Int>) {
        if (this.is_leaf_visible(beatkey, position)) {
            return
        }
        val main = this.get_main()
        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = rvBeatTable.adapter as BeatColumnAdapter
        rvBeatTable_adapter.scrollToPosition(beatkey, position)
    }

    fun set_active_line(y: Int) {
        val main = this.get_main()

        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.get_cursor()
        this.set_cursor_position(y, cursor.x, listOf(), FocusType.Row)

        this.setContextMenu_line()
    }

    fun get_label_text(y: Int): String {
        val opus_manager = this.get_main().get_opus_manager()
        val (channel, line_offset) = opus_manager.get_channel_index(y)
        return if (!opus_manager.is_percussion(channel)) {
            "$channel::$line_offset"
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            "!$instrument"
        }
    }


    fun set_cursor_position(y: Int, x: Int, position: List<Int>, type: FocusType = FocusType.Cell) {
        val main = this.get_main()
        main.runOnUiThread {
            val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
            val adapter = rvBeatTable.adapter as BeatColumnAdapter
            val opus_manager = main.get_opus_manager()

            adapter.unset_cursor_position()

            opus_manager.set_cursor_position(y, x, position)

            adapter.apply_focus_type(type)
        }
    }

    private fun unset_cursor_position() {
        val rvBeatTable = this.get_main().findViewById<RecyclerView>(R.id.rvBeatTable)
        val adapter = rvBeatTable.adapter as BeatColumnAdapter
        adapter.unset_cursor_position()
    }

    fun set_channel_instrument(channel: Int, instrument: Int) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.set_channel_instrument(channel, instrument)
        main.update_channel_instruments(opus_manager)
    }
}