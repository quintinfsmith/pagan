package com.qfs.pagan

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.*
import java.lang.Integer.max
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class EditorFragment : PaganFragment() {
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

        LineLabelAdapter(this.get_main().get_opus_manager(), rvLineLabels, this.get_main())
        BeatColumnAdapter(this, rvBeatTable, ColumnLabelAdapter(this.get_main().get_opus_manager(), rvColumnLabels, this.get_main()))

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

        setFragmentResultListener("IMPORTPROJECT") { _, bundle: Bundle? ->
            try {
                bundle!!.getString("URI")?.let { path ->
                    val main = this.get_main()
                    var opus_manager = main.import_project(path)
                }
            } catch (e: Exception) {
                var opus_manager = this.get_main().get_opus_manager()
                // if Not Loaded, just create new and throw a message up
                if (!opus_manager.first_load_done) {
                    opus_manager.new()
                }
                this.get_main().feedback_msg("Corrupt Project File")
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
        val btnUnLink = view.findViewById<ImageView>(R.id.btnUnLink)
        val btnUnLinkAll = view.findViewById<ImageView>(R.id.btnUnLinkAll)
        val btnCancelLink: TextView = view.findViewById(R.id.btnCancelLink)

        val (is_networked, many_links) = if (opus_manager.cursor.mode == Cursor.CursorMode.Range) {
            var output = false
            for (beat_key in opus_manager.get_beatkeys_in_range(opus_manager.cursor.range!!.first, opus_manager.cursor.range!!.second)) {
                if (opus_manager.is_networked(beat_key)) {
                    output = true
                    break
                }
            }

            Pair(
                output,
                true
            )
        } else if (opus_manager.cursor.mode == Cursor.CursorMode.Single) {
            val cursor_key = opus_manager.cursor.get_beatkey()
            Pair(
                opus_manager.is_networked(cursor_key),
                opus_manager.get_all_linked(cursor_key).size == 2
            )
        } else {
            return
        }
        if (is_networked) {
            btnUnLink.setOnClickListener {
                this.interact_btnUnlink(it)
            }
            if (many_links) {
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

    fun setContextMenu_column() {
        this.clearContextMenu()
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_column,
            llContextMenu,
            false
        )
        val btnInsertBeat = view.findViewById<ImageView>(R.id.btnInsertBeat)
        val btnRemoveBeat = view.findViewById<ImageView>(R.id.btnRemoveBeat)

        btnInsertBeat.setOnClickListener {
            val beat = opus_manager.cursor.beat
            opus_manager.insert_beat_at_cursor(1)
            opus_manager.cursor_select_column(beat + 1)
        }
        btnInsertBeat.setOnLongClickListener {
            main.popup_number_dialog( "Insert Beats", 1, 99) { count: Int ->
                val beat = opus_manager.cursor.beat
                opus_manager.insert_beat_at_cursor(count)
                opus_manager.cursor_select_column(beat + count)
            }
            true
        }

        btnRemoveBeat.setOnClickListener {
            val beat = opus_manager.cursor.beat
            opus_manager.remove_beat_at_cursor(1)
            if (beat >= opus_manager.opus_beat_count) {
                opus_manager.cursor_select_column(opus_manager.opus_beat_count - 1)
            }
        }

        btnRemoveBeat.setOnLongClickListener {
            main.popup_number_dialog("Remove Beats", 1, opus_manager.opus_beat_count) { count: Int ->
                val beat = opus_manager.cursor.beat
                opus_manager.remove_beat_at_cursor(count)
                if (beat >= opus_manager.opus_beat_count) {
                    opus_manager.cursor_select_column(opus_manager.opus_beat_count - 1)
                }
            }
            true
        }

        llContextMenu.addView(view)
        this.active_context_menu_index = ContextMenu.Column
    }

    fun setContextMenu_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != Cursor.CursorMode.Row) {
            throw Cursor.InvalidModeException(opus_manager.cursor.mode, Cursor.CursorMode.Row)
        }
        this.clearContextMenu()
        val llContextMenu = this.activity!!.findViewById<LinearLayout>(R.id.llContextMenu)

        val view = LayoutInflater.from(llContextMenu.context).inflate(
            R.layout.contextmenu_row,
            llContextMenu,
            false
        )
        val sbLineVolume = view.findViewById<SeekBar>(R.id.sbLineVolume)
        val btnRemoveLine = view.findViewById<ImageView>(R.id.btnRemoveLine)
        val btnInsertLine = view.findViewById<ImageView>(R.id.btnInsertLine)
        val btnChoosePercussion: TextView = view.findViewById(R.id.btnChoosePercussion)

        if (opus_manager.get_total_line_count() == 1) {
            btnRemoveLine.visibility = View.GONE
        }

        val channel = opus_manager.cursor.channel
        val line_offset = opus_manager.cursor.line_offset

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
                opus_manager.remove_line(1)
                opus_manager.cursor_select_row(channel, max(0, line_offset - 1))
            }

            btnRemoveLine.setOnLongClickListener {
                main.popup_number_dialog(
                    "Remove Lines",
                    1,
                    kotlin.math.max(1, opus_manager.get_total_line_count() - 1)
                ) { count: Int ->
                    opus_manager.remove_line(count)
                    opus_manager.cursor_select_row(channel, max(0, line_offset - 1))
                }
                true
            }
        }

        btnInsertLine.setOnClickListener {
            opus_manager.insert_line(1)
            opus_manager.cursor_select_row(channel, line_offset + 1)
        }

        btnInsertLine.setOnLongClickListener {
            main.popup_number_dialog(
                "Insert Lines",
                1,
                9,
            ) { count: Int ->
                opus_manager.insert_line(count)
                opus_manager.cursor_select_row(channel, line_offset + count)
            }
            true
        }

        sbLineVolume.progress = opus_manager.get_line_volume(channel, line_offset)
        sbLineVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) { }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                opus_manager.set_line_volume(channel, line_offset, seekbar.progress)
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

        val current_tree = opus_manager.get_tree()

        // If event exists, change relative mode, other wise use active relative mode
        if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            opus_manager.set_relative_mode(event)
        }


        rosRelativeOption.setState(opus_manager.relative_mode, true)

        if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
            nsOctave.visibility = View.GONE
            nsOffset.visibility = View.GONE
            rosRelativeOption.visibility = View.GONE

            if (!opus_manager.get_tree().is_event()) {
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
            val beatkey = opus_manager.cursor.get_beatkey()
            val position = opus_manager.cursor.get_position().toMutableList()

            opus_manager.split_tree(2)

            position.add(0)
            opus_manager.cursor_select(beatkey, position)
        }

        btnSplit.setOnLongClickListener {
            main.popup_number_dialog("Split", 2, 29) { splits: Int ->
                val beatkey = opus_manager.cursor.get_beatkey()
                val position = opus_manager.cursor.get_position().toMutableList()

                opus_manager.split_tree(splits)

                position.add(0)
                opus_manager.cursor_select(beatkey, position)
            }
            true
        }

        btnUnset.setOnClickListener {
            this.interact_btnUnset(it)
        }

        val channel = opus_manager.cursor.channel
        if (!opus_manager.is_percussion(channel) && current_tree.is_leaf() && !current_tree.is_event()) {
            btnUnset.visibility = View.GONE
        }

        if (opus_manager.cursor.get_position().isEmpty()) {
            btnRemove.visibility = View.GONE
        } else {
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener {
                val beatkey = opus_manager.cursor.get_beatkey()
                val position = opus_manager.cursor.get_position().toMutableList()
                val original_size = current_tree.parent!!.size

                opus_manager.remove(1)
                if (original_size >= 2) {
                    position.removeLast()
                } else if (position.last() >= original_size) {
                    position[position.size - 1] = max(original_size - 1, 0)
                }
                opus_manager.cursor_select(beatkey, position)
            }
            btnRemove.setOnLongClickListener {
                val position = opus_manager.cursor.get_position().toMutableList()
                if (position.isNotEmpty()) {
                    position.removeLast()

                }
                opus_manager.cursor_select(
                    opus_manager.cursor.get_beatkey(),
                    position
                )

                opus_manager.unset()
                true
            }
        }

        btnInsert.setOnClickListener {
            val beat_key = opus_manager.cursor.get_beatkey()
            val position = opus_manager.cursor.get_position().toMutableList()
            if (position.isEmpty()) {
                opus_manager.split_tree(2)
                position.add(0)
            } else {
                opus_manager.insert_after(1)
                position[position.size - 1] += 1
            }
            opus_manager.cursor_select(beat_key, position)
        }

        btnInsert.setOnLongClickListener {
            main.popup_number_dialog("Insert", 1, 29) { count: Int ->
                val beat_key = opus_manager.cursor.get_beatkey()
                val position = opus_manager.cursor.get_position().toMutableList()
                if (position.isEmpty()) {
                    position.add(0)
                    opus_manager.split_tree(count)
                } else {
                    opus_manager.insert_after(count)
                    position[position.size - 1] += count
                }
                opus_manager.cursor_select(beat_key, position)
            }
            true
        }

        this.active_context_menu_index = ContextMenu.Leaf
    }

    private fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val current_tree = opus_manager.get_tree()

        opus_manager.relative_mode = view.getState()!!

        var event = current_tree.get_event() ?: return

        val nsOctave: NumberSelector = main.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = main.findViewById(R.id.nsOffset)

        when (opus_manager.relative_mode) {
            0 -> {
                if (event.relative) {
                    try {
                        opus_manager.convert_event_to_absolute()
                        event = current_tree.get_event()!!
                    } catch (e: Exception) {
                        event.note = 0
                        event.relative = false
                        opus_manager.set_event(event.copy())
                    }
                }
                nsOctave.setState(event.note / event.radix, true, true)
                nsOffset.setState(event.note % event.radix, true, true)
            }
            1 -> {
                if (!event.relative) {
                    opus_manager.convert_event_to_relative()
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
                    opus_manager.convert_event_to_relative()
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

        val channel = opus_manager.cursor.channel
        if (!opus_manager.is_percussion(channel) || opus_manager.get_tree().is_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event()
        }
        this.reset_context_menu()
    }

    private fun interact_btnUnlink(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.unlink_beat()
        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        (rvBeatTable.adapter as BeatColumnAdapter).cancel_linking()
    }

    private fun interact_btnUnlinkAll(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.clear_link_pool()

        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        (rvBeatTable.adapter as BeatColumnAdapter).cancel_linking()
    }

    private fun interact_btnCancelLink(view: View) {
        var main = this.get_main()

        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        (rvBeatTable.adapter as BeatColumnAdapter).cancel_linking()

    }

    private fun interact_nsOffset(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState()!!
        val current_tree = opus_manager.get_tree()

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
            opus_manager.cursor.channel,
            opus_manager.relative_mode != 0
        )

        opus_manager.set_event(event)
        this.reset_context_menu()
    }

    private fun interact_nsOctave(view: NumberSelector) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val progress = view.getState() ?: return

        val current_tree = opus_manager.get_tree()

        val value = if (current_tree.is_event()) {
            val event = current_tree.get_event()!!
            val prev_note = if (opus_manager.relative_mode != 0) {
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
            opus_manager.cursor.channel,
            opus_manager.relative_mode != 0
        )

        opus_manager.set_event(event)
        this.reset_context_menu()
    }

    private fun interact_btnChoosePercussion(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val popupMenu = PopupMenu(this.binding.root.context, view)
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

        val line_map = opus_manager.channels[opus_manager.cursor.channel].line_map
        // Allowing repeat instruments. Commenting Out.
        //if (line_map != null) {
        //    for ((_, instrument) in line_map) {
        //        if (instrument + 27 in available_drum_keys) {
        //            available_drum_keys.remove(instrument + 27)
        //        }
        //    }
        //}
        drums.forEachIndexed { i, string ->
            if ((i + 27) in available_drum_keys) {
                popupMenu.menu.add(0, i, i, "$i: $string")
            }
        }

        popupMenu.setOnMenuItemClickListener {
            opus_manager.set_percussion_instrument( it.itemId )
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
            opus_manager.cursor_select_column(beat)
        }
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
        if (beatkey.beat == -1) {
            return
        }
        val adj_beatkey = BeatKey(
            max(0, beatkey.channel),
            max(0, beatkey.line_offset),
            beatkey.beat
        )
        // Move to leaf
        val new_position = position.toMutableList()
        var tree = this.get_main().get_opus_manager().get_tree(adj_beatkey, position)
        while (! tree.is_leaf()) {
            tree = tree[0]
            new_position.add(0)
        }

        if (this.is_leaf_visible(adj_beatkey, new_position)) {
            return
        }
        val main = this.get_main()
        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        val rvBeatTable_adapter = rvBeatTable.adapter as BeatColumnAdapter
        rvBeatTable_adapter.scrollToPosition(adj_beatkey, new_position)
    }
}