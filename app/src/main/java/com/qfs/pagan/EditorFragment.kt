package com.qfs.pagan

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.RecyclerView
import com.qfs.apres.InvalidMIDIFile
import com.qfs.pagan.databinding.FragmentMainBinding
import com.qfs.pagan.opusmanager.*
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.concurrent.thread

/**
 *
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
        val rvTable = this.binding.root.findViewById<RecyclerView>(R.id.rvTable)
        this.table_offset_pause = rvTable.computeHorizontalScrollOffset()
        super.onPause()
    }


    override fun onResume() {
        val rvTable = this.binding.root.findViewById<RecyclerView>(R.id.rvTable)
        val rvTable_adapter = rvTable.adapter as ColumnRecyclerAdapter

        val rvLineLabels = this.binding.root.findViewById<RecyclerView>(R.id.rvLineLabels)
        val rvLineLabels_adapter = rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter

        val opus_manager = this.get_main().get_opus_manager()
        if (rvLineLabels_adapter.itemCount == 0) {
            opus_manager.channels.forEach { channel: OpusChannel ->
                channel.lines.forEach { _: OpusChannel.OpusLine ->
                    rvLineLabels_adapter.addLineLabel()
                }
            }
        }

        //rvTable_adapter.notifyItemRangeInserted(0, opus_manager.opus_beat_count)
        rvTable_adapter.notifyItemRangeChanged(0, opus_manager.opus_beat_count)
        Log.d("AAA", "resume? ${opus_manager.opus_beat_count}")

        this.get_main().update_title_text()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvTable = view.findViewById<RecyclerView>(R.id.rvTable)
        val rvLineLabels = view.findViewById<RecyclerView>(R.id.rvLineLabels)

        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val new_adapter = ColumnRecyclerAdapter(main)
        rvTable.adapter = new_adapter
        rvTable.adapter?.notifyItemRangeInserted(0, opus_manager.opus_beat_count)


         LineLabelRecyclerView.LineLabelAdapter(
             opus_manager,
             rvLineLabels,
             main
         )

        //BeatColumnAdapter(
        //    this,
        //    rvTable,
        //    ColumnLabelAdapter(
        //        opus_manager,
        //        rvColumnLabels,
        //        main
        //    ),
        //)


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
                try {
                    main.import_midi(path)
                } catch (e: InvalidMIDIFile) {
                    main.get_opus_manager().new()
                    main.feedback_msg("Invalid MIDI")
                }
            }
        }

        setFragmentResultListener("IMPORTPROJECT") { _, bundle: Bundle? ->
            try {
                bundle!!.getString("URI")?.let { path ->
                    val main = this.get_main()
                    main.import_project(path)
                }
            } catch (e: Exception) {
                val opus_manager = this.get_main().get_opus_manager()
                // if Not Loaded, just create new and throw a message up
                if (!opus_manager.first_load_done) {
                    opus_manager.new()
                }
                this.get_main().feedback_msg("Corrupt Project File")
            }
        }

        setFragmentResultListener("NEW") { _, _: Bundle? ->
            val main = this.get_main()
            main.loading_reticle()
            main.get_opus_manager().new()
            main.cancel_reticle()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun reset_context_menu() {
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
        //val rvTable = this.get_main().findViewById<RecyclerView>(R.id.rvTable)
        //(rvTable.adapter as BeatColumnAdapter).refresh_visible()
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
                opus_manager.get_all_linked(cursor_key).size > 2
            )
        } else {
            return
        }
        if (is_networked) {
            btnUnLink.setOnClickListener {
                this.interact_btnUnlink()
            }
            if (!many_links) {
                btnUnLinkAll.visibility = View.GONE
            } else {
                btnUnLinkAll.setOnClickListener {
                    this.interact_btnUnlinkAll()
                }
            }
        } else {
            btnUnLink.visibility = View.GONE
            btnUnLinkAll.visibility = View.GONE
        }

        btnCancelLink.setOnClickListener {
            this.interact_btnCancelLink()
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
            try {
                opus_manager.remove_beat_at_cursor(1)
                if (beat >= opus_manager.opus_beat_count) {
                    opus_manager.cursor_select_column(opus_manager.opus_beat_count - 1)
                }
            } catch (e: OpusManagerBase.RemovingLastBeatException) {
                this.get_main().feedback_msg("Can't remove only beat")
            }

            if (opus_manager.opus_beat_count == 1) {
                btnRemoveBeat.visibility = View.GONE
            }
        }

        btnRemoveBeat.setOnLongClickListener {
            main.popup_number_dialog("Remove Beats", 1, opus_manager.opus_beat_count - 1) { count: Int ->
                val beat = opus_manager.cursor.beat
                opus_manager.remove_beat_at_cursor(count)
                if (beat >= opus_manager.opus_beat_count) {
                    opus_manager.cursor_select_column(opus_manager.opus_beat_count - 1)
                }

                if (opus_manager.opus_beat_count == 1) {
                    btnRemoveBeat.visibility = View.GONE
                }
            }
            true
        }

        if (opus_manager.opus_beat_count == 1) {
            btnRemoveBeat.visibility = View.GONE
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

            val instrument = opus_manager.get_percussion_instrument(line_offset)
            btnChoosePercussion.text = main.getString(
                R.string.label_choose_percussion,
                instrument,
                main.get_drum_name(instrument) ?: "Drum Not Found"
            )
        }

        if (opus_manager.channels[channel].size == 1) {
            btnRemoveLine.visibility = View.GONE
        } else {
            btnRemoveLine.setOnClickListener {
                opus_manager.remove_line(1)
                opus_manager.cursor_select_row(channel, max(0, line_offset - 1))
            }

            btnRemoveLine.setOnLongClickListener {
                var lines = opus_manager.channels[opus_manager.cursor.channel].size
                var max_lines = min(lines - 1, lines - opus_manager.cursor.line_offset)
                main.popup_number_dialog(
                    "Remove Lines",
                    1,
                    max_lines
                ) { count: Int ->
                    opus_manager.remove_line(count)
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
                this.interact_btnUnset()
            }

        } else {
            if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                val value = if (event.note < 0) {
                    0 - event.note
                } else {
                    event.note
                }
                nsOffset.setState(value % event.radix, manual = true, surpress_callback = true)
                nsOctave.setState(value / event.radix, manual = true, surpress_callback = true)
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
            this.interact_btnUnset()
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

                opus_manager.remove(1)
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
                    opus_manager.split_tree(count + 1)
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
                nsOctave.setState(event.note / event.radix, manual = true, surpress_callback = true)
                nsOffset.setState(event.note % event.radix, manual = true, surpress_callback = true)
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
                    nsOctave.setState(event.note / event.radix,
                        manual = true,
                        surpress_callback = true
                    )
                    nsOffset.setState(event.note % event.radix,
                        manual = true,
                        surpress_callback = true
                    )
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

    fun play_event(beat_key: BeatKey, position: List<Int>) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val current_tree = opus_manager.get_tree()
        var event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        if (event_note < 0) {
            return
        }
        var volume = opus_manager.channels[beat_key.channel].get_line_volume(beat_key.line_offset)
        main.play_event(
            beat_key.channel,
            event_note,
            volume
        )

    }

    private fun interact_btnUnset() {
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

    private fun interact_btnUnlink() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.unlink_beat()
        val rvTable = main.findViewById<RecyclerView>(R.id.rvTable)
        (rvTable.adapter as BeatColumnAdapter).cancel_linking()
    }

    private fun interact_btnUnlinkAll() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.clear_link_pool()

        val rvTable = main.findViewById<RecyclerView>(R.id.rvTable)
        (rvTable.adapter as BeatColumnAdapter).cancel_linking()
    }

    private fun interact_btnCancelLink() {
        val main = this.get_main()

        val rvTable = main.findViewById<RecyclerView>(R.id.rvTable)
        (rvTable.adapter as BeatColumnAdapter).cancel_linking()

        val rvLineLabels = main.findViewById<RecyclerView>(R.id.rvLineLabels)
        (rvLineLabels.adapter as LineLabelRecyclerView.LineLabelAdapter).refresh()
        val rvColumnLabels = main.findViewById<RecyclerView>(R.id.rvColumnLabels)
        (rvColumnLabels.adapter as ColumnLabelAdapter).refresh()

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
                    nsOctave.setState(0, manual = true, surpress_callback = true)
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
                1 -> {
                    progress
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        ((preceding_event.note / opus_manager.RADIX) * opus_manager.RADIX) + progress
                    } else {
                        progress
                    }
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
        this.play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
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
                    nsOffset.setState(0, manual = true, surpress_callback = true)
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
                1 -> {
                    progress * opus_manager.RADIX
                }
                else -> {
                    val beat_key = opus_manager.cursor.get_beatkey()
                    val position = opus_manager.cursor.get_position()
                    val preceding_event = opus_manager.get_preceding_event(beat_key, position)
                    if (preceding_event != null && !preceding_event.relative) {
                        (progress * opus_manager.RADIX) + (preceding_event.note % opus_manager.RADIX)
                    } else {
                        (progress * opus_manager.RADIX)
                    }
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
        this.play_event(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
        this.reset_context_menu()
    }


    private fun interact_btnChoosePercussion(view: View) {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()

        val popupMenu = PopupMenu(this.binding.root.context, view)

        var i = 0
        val sorted_keys = main.active_percussion_names.keys.toMutableList()
        sorted_keys.sort()
        for (note in sorted_keys) {
            val name = main.active_percussion_names[note]
            popupMenu.menu.add(0, note - 27, i, "${note - 27}: $name")
            i += 1
        }

        popupMenu.setOnMenuItemClickListener {
            opus_manager.set_percussion_instrument( it.itemId)
            true
        }

        popupMenu.show()
    }

    fun scroll_to_beat(beat: Int, select: Boolean = false) {
        val main = this.get_main()
        main.runOnUiThread {
            val rvTable = main.findViewById<RecyclerView>(R.id.rvTable)
            (rvTable.adapter as BeatColumnAdapter).scrollToPosition(beat)
        }

        if (select) {
            val opus_manager = main.get_opus_manager()
            opus_manager.cursor_select_column(beat)
        }
    }


    // TODO: Consider Y
    private fun is_leaf_visible(beatkey: BeatKey, position: List<Int>): Boolean {
        val main = this.get_main()
        val rvTable = main.findViewById<RecyclerView>(R.id.rvTable)
        val rvTable_adapter = rvTable.adapter as BeatColumnAdapter
        val tree_view = rvTable_adapter.get_leaf_view(beatkey, position)
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
        val rvTable = main.findViewById<RecyclerView>(R.id.rvTable)
        val rvTable_adapter = rvTable.adapter as BeatColumnAdapter
        rvTable_adapter.scrollToPosition(adj_beatkey, new_position)
    }
}
