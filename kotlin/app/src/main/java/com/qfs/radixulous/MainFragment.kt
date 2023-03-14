package com.qfs.radixulous

import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.core.view.DragStartHelper
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.databinding.FragmentMainBinding
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.FlagOperation
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.UpdateFlag
import java.io.FileInputStream
import kotlin.concurrent.thread
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

        val rvBeatTable = view.findViewById<RecyclerView>(R.id.rvBeatTable)
        val svTable: ScrollView = view.findViewById(R.id.svTable)
        val rvColumnLabels = view.findViewById<RecyclerView>(R.id.rvColumnLabels)
        val svLineLabels: ScrollView = view.findViewById(R.id.svLineLabels)

        var column_label_adapter = ColumnLabelAdapter(this, rvColumnLabels)
        // init OpusManagerAdapter
        OpusManagerAdapter(this, rvBeatTable, column_label_adapter)

        rvBeatTable.viewTreeObserver.addOnScrollChangedListener {
            println("${rvBeatTable.scrollX} -----")
            (rvColumnLabels.adapter as ColumnLabelAdapter).scrollToX(rvBeatTable.computeHorizontalScrollOffset())
        }
        //svTable.viewTreeObserver.addOnScrollChangedListener {
        //    svLineLabels.scrollY = svTable.scrollY
        //}
        //rvColumnLabels.viewTreeObserver.addOnScrollChangedListener {
        //    rvColumnLabels.scrollX = rvBeatTable.scrollX
        //}
        //svLineLabels.viewTreeObserver.addOnScrollChangedListener {
        //    svLineLabels.scrollY = svTable.scrollY
        //}

        setFragmentResultListener("LOAD") { _, bundle: Bundle? ->
            this.block_default_return = true
            val main = this.getMain()

            bundle!!.getString("TITLE")?.let { title: String ->
                main.set_current_project_title( title )
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
            this.block_default_return = true
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

        setFragmentResultListener("RETURNED") { _, bundle: Bundle? ->
            if (this.block_default_return) {
                this.block_default_return = false
                return@setFragmentResultListener
            }
            val main = this.getMain()
            main.getOpusManager().reset_cache()
            main.update_title_text()

            this.setContextMenu(ContextMenu.Leaf)
            this.tick()
            main.update_menu_options()
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

        val sRelative: ToggleButton = view.findViewById(R.id.sRelative)
        val llAbsolutePalette: LinearLayout = view.findViewById(R.id.llAbsolutePalette)
        val llRelativePalette: LinearLayout = view.findViewById(R.id.llRelativePalette)

        val btnUnset = view.findViewById<ImageView>(R.id.btnUnset)
        val btnSplit = view.findViewById<View>(R.id.btnSplit)
        val btnRemove = view.findViewById<View>(R.id.btnRemove)
        val btnInsert = view.findViewById<View>(R.id.btnInsert)
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

            //if (!opus_manager.get_tree_at_cursor().is_event()) {
            //    btnUnset.text = "Set"
            //}

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

    private fun interact_nsOffset(view: NumberSelector) {
        val main = this.getMain()
        main.stop_playback()
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
        main.stop_playback()
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

    private fun interact_sRelative_changed(view: View, isChecked: Boolean) {
        val main = this.getMain()
        main.stop_playback()
        val opus_manager = main.getOpusManager()
        if (isChecked) {
            try {
                opus_manager.convert_event_at_cursor_to_relative()
            } catch (e: Exception) {

            }
        } else {
            try {
                opus_manager.convert_event_at_cursor_to_absolute()
            } catch (e: Exception) {
                if (opus_manager.get_tree_at_cursor().is_event()) {
                    main.feedback_msg("Can't convert event")
                    (view as ToggleButton).isChecked = true
                    return
                }
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
        main.stop_playback()
        val opus_manager = main.getOpusManager()

        val popupMenu = PopupMenu(this.binding.root.context, view)
        val cursor = opus_manager.get_cursor()
        val drums = resources.getStringArray(R.array.midi_drums)
        var preset = main.soundfont.get_preset(0, 128)
        var available_drum_keys = mutableSetOf<Int>()

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

        var line_map = opus_manager.channels[cursor.get_beatkey().channel].line_map
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

    fun scroll_to_beat(beat: Int) {
        val main = this.getMain()

        val rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        rvBeatTable.smoothScrollToPosition(beat)
        //val rvColumnLabels = main.findViewById<LinearLayout>(R.id.rvColumnLabels)
    }


    fun select_column(x: Int) {
        val main = this.getMain()
        val opus_manager = main.getOpusManager()

        var rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        (rvBeatTable.adapter as OpusManagerAdapter).set_focus_type(FocusType.Column)

        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(cursor.y, x, listOf())

        this.setContextMenu(ContextMenu.Beat)
        this.tick()
    }

    fun update_leaf_labels(opus_manager: OpusManager) {
        val beat_table = this.getMain().findViewById<RecyclerView>(R.id.rvBeatTable)
        var beat_table_adapter = beat_table.adapter as OpusManagerAdapter
        beat_table_adapter.update_leaf_labels()
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
        /* TODO: break this function up. Only a monolith since changing to handle all flags in
            the exact order they were created
         */
        if (!this.ticking) {
            this.ticking = true

            val beat_table = this.getMain().findViewById<RecyclerView>(R.id.rvBeatTable)
            var beat_table_adapter = beat_table.adapter as OpusManagerAdapter
            var rvColumnLabels_adapter = this.getMain().findViewById<RecyclerView>(R.id.rvColumnLabels).adapter as ColumnLabelAdapter

            var main = this.getMain()
            val opus_manager = main.getOpusManager()

            var updated_beatkeys = mutableSetOf<BeatKey>()
            val updated_beats: MutableSet<Int> = mutableSetOf()
            var min_changed_beat = opus_manager.opus_beat_count
            var validate_count = 0

            beat_table_adapter.tick_unapply_cursor_focus()

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
                                rvColumnLabels_adapter.addColumnLabel()
                                beat_table_adapter.notifyItemInserted(index)
                            }
                            FlagOperation.Pop -> {
                                beat_table_adapter.notifyItemRemoved(index)
                                rvColumnLabels_adapter.removeColumnLabel(index)
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
                        var line_flag = opus_manager.fetch_flag_line() ?: break
                        for (i in 0 until opus_manager.opus_beat_count ) {
                            updated_beats.add(i)
                        }
                        when (line_flag.operation) {
                            FlagOperation.Pop -> {
                                this.remove_line_label(line_flag.line)
                            }
                            FlagOperation.New -> {
                                this.insert_line_label(line_flag.line)
                            }
                        }
                    }

                    null -> {
                        break
                    }
                }
            }

            //this.line_update_labels(opus_manager)

            //this.tick_resize_beats(updated_beats.toList())
            for (b in updated_beats) {
                println("$b Beat Changed")
                beat_table_adapter.notifyItemChanged(b)
            }
            //beat_table_adapter.notifyItemRangeChanged(
            //    min_changed_beat,
            //    opus_manager.opus_beat_count - min_changed_beat
            //)

            for (i in 0 until validate_count) {
                val (beatkey, position) = opus_manager.fetch_flag_absolute_value() ?: break
                var abs_value = opus_manager.get_absolute_value(beatkey, position) ?: continue
                beat_table_adapter.validate_leaf(beatkey, position, abs_value in 0..95)
            }

            beat_table_adapter.tick_apply_cursor_focus()

            this.ticking = false
        }
    }

    fun newProject() {
        var main = this.getMain()
        main.newProject()

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    fun scrollTo(beatkey: BeatKey, position: List<Int>) {
        var leafView: View? = this.cache.getTreeView(beatkey, position) ?: return
        if (leafView !is LeafButton) {
            return
        }

        var hsvTable = this.getMain().findViewById<RecyclerView>(R.id.rvBeatTable)
        var svTable = this.getMain().findViewById<ScrollView>(R.id.svTable)

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

        var offset_left = offsetViewBounds.left
        var offset_top = offsetViewBounds.top

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

        var rvBeatTable = main.findViewById<RecyclerView>(R.id.rvBeatTable)
        (rvBeatTable.adapter as OpusManagerAdapter).set_focus_type(FocusType.Row)


        var opus_manager = main.getOpusManager()
        val cursor = opus_manager.get_cursor()
        opus_manager.set_cursor_position(y, cursor.x, listOf())

        this.tick()
        this.setContextMenu(ContextMenu.Line)
    }

}