package com.qfs.radixulous

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.channel_ctrl.view.*
import kotlinx.android.synthetic.main.contextmenu_cell.view.*
import kotlinx.android.synthetic.main.contextmenu_column.view.*
import kotlinx.android.synthetic.main.contextmenu_linking.view.*
import kotlinx.android.synthetic.main.contextmenu_row.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.load_project.view.*
import kotlinx.android.synthetic.main.table_line_label.view.*
import java.io.File
import java.lang.Integer.max
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
//import com.qfs.radixulous.MIDIPlaybackDevice
import com.qfs.radixulous.apres.*

import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Integer.min
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

enum class ContextMenu {
    Leaf,
    Line,
    Beat,
    Linking,
    None
}

class ViewCache {
    private var view_cache: MutableList<Pair<LinearLayout, MutableList<Pair<View?, HashMap<List<Int>, View>>>>> = mutableListOf()
    private var line_label_cache: MutableList<View> = mutableListOf()
    private var column_label_cache: MutableList<View> = mutableListOf()
    private var _cursor: Triple<Int, Int, List<Int>>? = null
    private var active_context_menu_view: View? = null
    private var column_widths: MutableList<Int> = mutableListOf()
    fun set_column_width(x: Int, size: Int) {
        this.column_widths[x] = size
    }
    fun get_column_width(x: Int): Int {
        return this.column_widths[x]
    }
    fun add_column_width(x: Int) {
        this.column_widths.add(x, 0)
    }
    fun remove_column_width(x: Int): Int {
        return this.column_widths.removeAt(x)
    }

    fun get_all_leafs(y: Int, x: Int, position: List<Int>): List<Pair<View, List<Int>>> {
        val output: MutableList<Pair<View, List<Int>>> = mutableListOf()
        if (y >= this.view_cache.size || x >= this.view_cache[y].second.size) {
            return output
        }
        for ((key_pos, view) in this.view_cache[y].second[x].second) {
            try {
                view as ViewGroup
                continue
            } catch (e: Exception) {
                // pass. leaves can't be view groups so this is just a check
            }
            if (position.size <= key_pos.size && key_pos.subList(0, position.size) == position) {
                output.add(Pair(view, key_pos))
            }
        }
        if (output.isEmpty() && position.isEmpty()) {
            output.add(Pair(this.view_cache[y].second[x].first!!, position))
        }

        return output
    }

    fun setActiveContextMenu(view: View) {
        this.active_context_menu_view = view
    }

    fun getActiveContextMenu(): View? {
        return this.active_context_menu_view
    }

    fun cacheLine(view: LinearLayout, y: Int) {
        if (y < this.view_cache.size) {
            this.view_cache.add(y, Pair(view, mutableListOf()))
        } else {
            this.view_cache.add(Pair(view, mutableListOf()))
        }
    }

    fun cacheTree(view: View, y: Int, x: Int, position: List<Int>) {
        if (position.isEmpty()) {
            if (x < this.view_cache[y].second.size) {
                this.view_cache[y].second.add(x, Pair(view, HashMap()))
            } else {
                this.view_cache[y].second.add(Pair(view, HashMap()))
            }
        } else {
            this.view_cache[y].second[x].second[position] = view
        }
    }

    fun getTreeView(y: Int, x: Int, position: List<Int>): View? {
        return if (position.isEmpty()) {
            this.view_cache[y].second[x].first
        } else {
            this.view_cache[y].second[x].second[position]
        }
    }

    fun getLine(y: Int): LinearLayout {
        return this.view_cache[y].first
    }

    fun addColumnLabel(view: View) {
        this.column_label_cache.add(view)
    }

    fun getColumnLabel(x: Int): View {
        return this.column_label_cache[x]
    }

    fun detachColumnLabel() {
        val label = this.column_label_cache.removeLast()
        (label.parent as ViewGroup).removeView(label)
    }

    fun addLineLabel(view: View) {
        this.line_label_cache.add(view)
    }

    fun getLineLabel(y: Int): View? {
        if (y < this.line_label_cache.size) {
            return this.line_label_cache[y]
        } else {
            return null
        }
    }

    fun detachLine(y: Int) {
        val label = this.line_label_cache.removeAt(y)
        (label.parent as ViewGroup).removeView(label)

        val view = this.view_cache.removeAt(y).first
        (view.parent as ViewGroup).removeView(view)
    }

    fun removeBeatView(y: Int, x: Int) {
        val line_cache = this.view_cache[y].second
        for ((pos, view) in line_cache[x].second) {
            (view.parent as ViewGroup).removeView(view)
        }
        line_cache[x].second.clear()

        line_cache.removeAt(x)
        // Detach using line. we cache each leaf, but there is still a wrapper to deal with.
        var line = this.getLine(y)
        line.removeViewAt(x)
    }

    fun getTreeViewYXPosition(view: View): Triple<Int, Int, List<Int>>? {
        for (y in 0 until this.view_cache.size) {
            val line_cache = this.view_cache[y].second
            for (x in 0 until line_cache.size) {
                if (view == line_cache[x].first) {
                    return Triple(y, x, listOf())
                }

                for (key in line_cache[x].second.keys) {
                    if (line_cache[x].second[key] == view) {
                        return Triple(y, x, key)
                    }
                }
            }
        }
        return null
    }

    fun getCursor(): Triple<Int, Int, List<Int>>? {
        return this._cursor
    }

    fun setCursor(y: Int, x: Int, position: List<Int>) {
        this._cursor = Triple(y, x, position.toList())
    }
    fun unsetCursor() {
        this._cursor = null
    }
}

class MainActivity : AppCompatActivity() {
    private var opus_manager = OpusManager()
    private var cache = ViewCache()
    private var active_context_menu_index: ContextMenu = ContextMenu.None
    private var linking_beat: BeatKey? = null
    private var relative_mode: Boolean = false
    private var ticking: Boolean = false // Lock to prevent multiple attempts at updating from happening at once
    lateinit var midi_controller: MIDIController
    lateinit var midi_playback_device: MIDIPlaybackDevice
    private var midi_input_device = MIDIInputDevice()
    private var midi_player = MIDIPlayer()
    private var in_playback = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: clean up the file -> riff -> soundfont -> midi playback device process
        var soundfont = SoundFont(Riff(assets.open("freepats-general-midi.sf2")))
        this.midi_playback_device = MIDIPlaybackDevice(this, soundfont)


        this.midi_controller = RadMidiController(window.decorView.rootView.context)
        this.midi_controller.registerVirtualDevice(this.midi_playback_device)
        this.midi_controller.registerVirtualDevice(this.midi_input_device)
        this.midi_controller.registerVirtualDevice(this.midi_player)


        // calling this activity's function to
        // use ActionBar utility methods
        val actionBar = supportActionBar!!
        //actionBar.setIcon(R.drawable.app_logo)
        // methods to display the icon in the ActionBar
        actionBar.setDisplayUseLogoEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        val hsvTable: HorizontalScrollView = findViewById(R.id.hsvTable)
        val svTable: ScrollView = findViewById(R.id.svTable)
        val hsvColumnLabels: HorizontalScrollView = findViewById(R.id.hsvColumnLabels)
        val svLineLabels: ScrollView = findViewById(R.id.svLineLabels)

        hsvTable.viewTreeObserver.addOnScrollChangedListener {
            hsvColumnLabels.scrollX = hsvTable.scrollX
        }
        svTable.viewTreeObserver.addOnScrollChangedListener {
            svLineLabels.scrollY = svTable.scrollY
        }

        val btnChannelCtrl: TextView = findViewById(R.id.btnChannelCtrl)
        btnChannelCtrl.setOnClickListener{
            this.showChannelPopup(it)
        }

        this.newProject()
    }

    fun load(path: String) {
        this.opus_manager.load(path)

        var name = this.opus_manager.get_working_dir()
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
        }
        val actionBar = supportActionBar
        actionBar!!.title = name
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun save() {
        this.opus_manager.save()
        Toast.makeText(this, "Project Saved", Toast.LENGTH_SHORT).show()
    }

    // method to inflate the options menu when
    // the user opens the menu for the first time
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        //menuInflater.inflate(R.menu.channel_instruments, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // methods to control the operations that will
    // happen when user clicks on the action buttons
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itmNewProject -> this.newProject()
            R.id.itmLoadProject -> this.showLoadPopup()
            R.id.itmSaveProject -> this.save()
            R.id.itmExportMidi -> this.export_midi()
            R.id.itmUndo -> this.undo()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun undo() {
        if (this.opus_manager.has_history()) {
            this.opus_manager.apply_undo()
            this.tick()
            this.setContextMenu(ContextMenu.Leaf)
        } else {
            Toast.makeText(this, getString(R.string.msg_undo_none), Toast.LENGTH_SHORT).show()
        }
    }

    private fun newProject() {
        // Check to save old
        // destroy current layout
        this.takedownCurrent()
        this.opus_manager.new()

        var projects_dir = "/data/data/com.qfs.radixulous/projects"
        var i = 0
        while (File("$projects_dir/opus$i").isFile) {
            i += 1
        }
        this.opus_manager.path = "$projects_dir/opus$i"
        val actionBar = supportActionBar
        actionBar!!.title = "opus$i"

        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun takedownCurrent() {
        this.setContextMenu(ContextMenu.None)
        this.tlOpusLines.removeAllViews()
        this.llLineLabels.removeAllViews()
        this.llColumnLabels.removeAllViews()
        this.cache = ViewCache()
    }

    private fun showChannelPopup(view: View?) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.channel_ctrl, null)

        val popupWindow = PopupWindow(
            popupView,
            MATCH_PARENT,
            MATCH_PARENT,
            true
        )
        for (i in 0 until this.opus_manager.channel_lines.size) {
            val chipView = Chip(popupView.clA.llB.cgEnabledChannels.context)
            chipView.isCheckable = true
            if (i == 9) {
                chipView.text = "Drums"
            } else {
                chipView.text = "$i"
            }
            chipView.isChecked = this.opus_manager.channel_lines[i].isNotEmpty()

            // TODO: I suspect there is a better listener for this
            chipView.setOnClickListener {
                if (chipView.isChecked) {
                    if (this.opus_manager.channel_lines[i].isEmpty()) {
                        this.opus_manager.add_channel(i)
                        this.tick()
                    }
                } else {
                    val line_count = this.opus_manager.channel_lines[i].size
                    if (this.opus_manager.line_count() > line_count) {
                        this.opus_manager.remove_channel(i)
                        this.tick()
                    } else {
                        chipView.isChecked = true
                    }
                }
            }
            popupView.clA.llB.cgEnabledChannels.addView(chipView)
        }

        for (i in 0 until this.opus_manager.channel_lines.size) {
            if (this.opus_manager.channel_lines[i].isEmpty()) {
                continue
            }
        }

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

        popupView.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    private fun showLoadPopup() {
        // TODO: Find way to use relative path
        var projects_dir = "/data/data/com.qfs.radixulous/projects"
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.load_project, null)

        val popupWindow = PopupWindow(
            popupView,
            MATCH_PARENT,
            MATCH_PARENT,
            true
        )

        var close_btn: ImageButton = popupView.findViewById(R.id.btnCloseLoadProject)
        close_btn.setOnTouchListener { it: View, motionEvent: MotionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                popupWindow.dismiss()
            }
            true
        }

        val directory = File(projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }

        for (file_name in directory.list()!!) {
            val file = File("$projects_dir/$file_name")
            if (!file.isDirectory) {
                continue
            }

            // TODO: Check if directory is project directory
            val row = LayoutInflater.from(popupView.svProjectList.llProjectList.context).inflate(
                R.layout.loadmenu_item,
                popupView.svProjectList.llProjectList,
                false
            ) as ViewGroup
            (row.getChildAt(0) as TextView).text = file_name
            row.setOnClickListener {
                this.takedownCurrent()
                this.load("$projects_dir/$file_name")
                popupWindow.dismiss()
            }

            popupView.svProjectList.llProjectList.addView(row)
        }

        popupWindow.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)
    }

    private fun newColumnLabel() {
        val parent = this.hsvColumnLabels.llColumnLabels
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
            val cursor = this.opus_manager.get_cursor()
            this.opus_manager.set_cursor_position(cursor.y, x, listOf())
            this.play_beat(x)
            this.setContextMenu(ContextMenu.Beat)
            this.tick()
        }
        this.cache.addColumnLabel(headerCellView)
        parent.addView(headerCellView)
    }

    private fun buildLineView(y: Int): TableRow {
        val clo = this.opus_manager.get_channel_index(y)
        val channel = clo.first
        val line_offset = clo.second

        val rowView = TableRow(this.tlOpusLines.context)
        rowView.setPadding(0,0,0,0)

        this.tlOpusLines.addView(rowView, y)
        this.cache.cacheLine(rowView, y)

        val rowLabel = LayoutInflater.from(this.svLineLabels.llLineLabels.context).inflate(
            R.layout.table_line_label,
            this.svLineLabels.llLineLabels,
            false
        )

        val that = this
        if (channel != 9) {
            if (line_offset == 0) {
                rowLabel.textView.text = "$channel:$line_offset"
            } else {
                rowLabel.textView.text = "  :$line_offset"
            }
        } else {
            val instrument = that.opus_manager.get_percussion_instrument(line_offset)
            rowLabel.textView.text = "P:$instrument"
        }

        rowLabel.textView.setOnClickListener {
            this.interact_rowLabel(it)
        }

        rowLabel.layoutParams.height = 125

        this.cache.addLineLabel(rowLabel)
        this.svLineLabels.llLineLabels.addView(rowLabel)

        return rowView
    }

    private fun buildTreeView(parent: ViewGroup, y: Int, x: Int, position: List<Int>): View {
        val channel_index = this.opus_manager.get_channel_index(y)
        val tree = this.opus_manager.get_tree(BeatKey(channel_index.first, channel_index.second, x), position)


        if (tree.is_leaf()) {
            val leafView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_opusbutton,
                parent,
                false
            )

            leafView.tvLeaf.background = resources.getDrawable(
                if (this.opus_manager.is_reflection(channel_index.first, channel_index.second, x)) {
                    R.drawable.leaf_reflection
                } else if (this.opus_manager.is_reflected(channel_index.first, channel_index.second, x)) {
                    R.drawable.leaf_reflected
                } else if (!tree.is_event()) {
                    R.drawable.leaf
                } else {
                    R.drawable.leaf_active
                }
            )

            if (tree.is_event()) {
                val event = tree.get_event()!!
                leafView.tvLeaf.text = if (event.relative) {
                    if (event.note == 0 || event.note % event.radix != 0) {
                        val prefix = if (event.note < 0) {
                            getString(R.string.pfx_subtract)
                        } else {
                            getString(R.string.pfx_add)
                        }
                        "$prefix${get_number_string(kotlin.math.abs(event.note), event.radix, 1)}"
                    } else {
                        val prefix = if (event.note < 0) {
                            getString(R.string.pfx_log)
                        } else {
                            getString(R.string.pfx_pow)
                        }
                        "$prefix${get_number_string(kotlin.math.abs(event.note) / event.radix, event.radix, 1)}"
                    }
                } else if (event.channel != 9) {
                    get_number_string(event.note, event.radix, 2)
                } else {
                    "!!"
                }
            } else {
                leafView.tvLeaf.text = getString(R.string.empty_note)
            }

            leafView.tvLeaf.setOnClickListener {
                this.interact_leafView_click(it)
            }

            leafView.tvLeaf.setOnLongClickListener {
                this.interact_leafView_longclick(it)
                true
            }

            if (position.isEmpty()) {
                parent.addView(leafView, x)
            } else {
                parent.addView(leafView)
            }

            this.cache.cacheTree(leafView.tvLeaf, y, x, position)
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

    private fun setContextMenu(menu_index: ContextMenu) {
        this.active_context_menu_index = menu_index
        val view_to_remove = this.cache.getActiveContextMenu()
        (view_to_remove?.parent as? ViewGroup)?.removeView(view_to_remove)

        when (menu_index) {
            ContextMenu.Line -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    this.llContextMenu,
                    false
                )

                view.btnRemoveLine.setOnClickListener {
                    this.interact_btnRemoveLine(it)
                }

                view.btnInsertLine.setOnClickListener {
                    this.interact_btnInsertLine(it)
                }

                val beatkey = this.opus_manager.get_cursor().get_beatkey()
                if (beatkey.channel == 9) {
                    val instrument = this.opus_manager.get_percussion_instrument(beatkey.line_offset)
                    view.btnChooseInstrument.text = resources.getStringArray(R.array.midi_drums)[instrument]
                } else {
                    val instrument = this.opus_manager.get_channel_instrument(beatkey.channel)
                    view.btnChooseInstrument.text = resources.getStringArray(R.array.midi_instruments)[instrument]
                }

                view.btnChooseInstrument.setOnClickListener {
                    this.interact_btnChooseInstrument(it)
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            ContextMenu.Beat -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_column,
                    this.llContextMenu,
                    false
                )

                view.btnInsertBeat.setOnClickListener {
                    this.interact_btnInsertBeat(it)
                }

                view.btnRemoveBeat.setOnClickListener {
                    this.interact_btnRemoveBeat(it)
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            ContextMenu.Leaf -> {
                this.setContextMenu_leaf()
            }

            ContextMenu.Linking -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_linking,
                    this.llContextMenu,
                    false
                )

                val cursor_key = this.opus_manager.get_cursor().get_beatkey()
                if (this.opus_manager.is_reflection(cursor_key.channel, cursor_key.line_offset, cursor_key.beat)) {
                    view.btnUnLink.setOnClickListener {
                        this.interact_btnUnlink(it)
                    }
                } else {
                    view.btnUnLink.visibility = View.GONE
                }

                view.btnCancelLink.setOnClickListener {
                    this.interact_btnCancelLink(it)
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            else -> { }
        }
    }

    private fun setContextMenu_leaf() {
        val view = LayoutInflater.from(this.llContextMenu.context).inflate(
            R.layout.contextmenu_cell,
            this.llContextMenu,
            false
        )

        this.llContextMenu.addView(view)
        this.cache.setActiveContextMenu(view)

        val current_tree = this.opus_manager.get_tree_at_cursor()
        if (current_tree.is_event()) {
            this.relative_mode = current_tree.get_event()!!.relative
        }

        val cursor = this.opus_manager.get_cursor()

        if (this.opus_manager.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())) {

            view.sRelative.isChecked = this.relative_mode
            view.sRelative.setOnCheckedChangeListener { it, isChecked ->
                this.interact_sRelative_changed(it, isChecked)
            }
        } else {
            this.relative_mode = false
            view.sRelative.visibility = View.GONE
        }

        if (cursor.get_beatkey().channel == 9) {
            view.llAbsolutePalette.visibility = View.GONE
            view.llRelativePalette.visibility = View.GONE
            view.sRelative.visibility = View.GONE

            if (!this.opus_manager.get_tree_at_cursor().is_event()) {
                view.clButtons.btnUnset?.text = "Set"
            }

            view.clButtons.btnUnset?.setOnClickListener {
                this.interact_btnUnset(it)
            }

        } else if (!this.relative_mode) {
            view.llRelativePalette.visibility = View.GONE

            if (current_tree.is_event()) {
                val event = current_tree.get_event()!!
                if (!event.relative) {
                    view.nsOffset.setState(event.note % event.radix)
                    view.nsOctave.setState(event.note / event.radix)
                }
            }
            view.llAbsolutePalette.nsOffset?.setOnChange(this::interact_nsOffset)
            view.llAbsolutePalette.nsOctave?.setOnChange(this::interact_nsOctave)
        } else {
            view.llAbsolutePalette.visibility = View.GONE
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

            var relativeOptions: RelativeOptionSelector = findViewById(R.id.rosRelativeOption)
            relativeOptions.setState(selected_button)

            this.resize_relative_value_selector()
            if (new_progress != null) {
                try {
                    view.llRelativePalette.nsRelativeValue.setState(new_progress)
                } catch (e: Exception) {
                    view.llRelativePalette.nsRelativeValue.unset_active_button()
                }
            }

            view.llRelativePalette.rosRelativeOption.setOnChange(this::interact_rosRelativeOption)
            view.llRelativePalette.nsRelativeValue.setOnChange(this::interact_nsRelativeValue)
        }

        view.clButtons.btnSplit?.setOnClickListener {
            this.interact_btnSplit(it)
        }

        view.clButtons.btnUnset?.setOnClickListener {
            this.interact_btnUnset(it)
        }

        view.clButtons.btnRemove?.setOnClickListener {
            this.interact_btnRemove(it)
        }

        view.clButtons.btnInsert?.setOnClickListener {
            this.interact_btnInsert(it)
        }
    }

    private fun resize_relative_value_selector() {
        var maximum_note = 95

        var cursor = this.opus_manager.get_cursor()
        var preceding_leaf = this.opus_manager.get_preceding_leaf_position(cursor.get_beatkey(), cursor.get_position())!!
        while (!this.opus_manager.get_tree(preceding_leaf.first, preceding_leaf.second).is_event()) {
            preceding_leaf = this.opus_manager.get_preceding_leaf_position(preceding_leaf.first, preceding_leaf.second)!!
        }
        var preceding_value = this.opus_manager.get_absolute_value(preceding_leaf.first, preceding_leaf.second)!!

        // Hide/ Show Relative options if they don't/do need to be visible
        var selector: RelativeOptionSelector = findViewById(R.id.rosRelativeOption)
        if (preceding_value > maximum_note - this.opus_manager.RADIX) {
            selector.hideOption(2)
        }

        if (preceding_value < this.opus_manager.RADIX) {
            selector.hideOption(3)
        }

        if (preceding_value > maximum_note) {
            selector.hideOption(0)
        }

        if (preceding_value == 0) {
            selector.hideOption(1)
        }

        var relMin = 1
        var relMax = maximum_note / this.opus_manager.RADIX
        when (selector.getState()) {
            0 -> {
                relMin = 0
                relMax = min(maximum_note - preceding_value, this.opus_manager.RADIX - 1)
            }
            1 -> {
                relMax = min(this.opus_manager.RADIX - 1, preceding_value)
                relMin = min(relMin, relMax)
            }
            2 -> {
                relMax = min((maximum_note - preceding_value) / this.opus_manager.RADIX, relMax)
                relMin = min(relMin, relMax)
            }
            3 -> {
                relMax = min(preceding_value / this.opus_manager.RADIX, relMax)
                relMin = min(relMin, relMax)
            }
            else -> { }
        }

        var view: NumberSelector = findViewById(R.id.nsRelativeValue)
        view.setRange(relMin, relMax)
        view.unset_active_button()
    }

    private fun rebuildBeatView(y: Int, x: Int) {
        val pair = this.opus_manager.get_channel_index(y)
        val main_beatkey = BeatKey(pair.first, pair.second, x)
        for (beatkey in this.opus_manager.get_all_linked(main_beatkey)) {
            val new_y = this.opus_manager.get_y(beatkey.channel, beatkey.line_offset)
            val new_x = beatkey.beat
            this.cache.removeBeatView(new_y, new_x)
            val rowView = this.cache.getLine(new_y)
            this.buildTreeView(rowView, new_y, new_x, listOf())
        }
    }

    private fun tick() {
        if (! this.ticking) {
            this.ticking = true
            this.tick_unapply_cursor()
            this.tick_manage_lines()
            this.tick_manage_beats() // new/pop
            this.tick_update_beats() // changes
            this.tick_apply_cursor()
            this.ticking = false
        }
    }

    private fun tick_manage_lines() {
        var lines_changed = false
        while (true) {
            val (channel, index, operation) = this.opus_manager.fetch_flag_line() ?: break
            when (operation) {
                0 -> {
                    val counts = this.opus_manager.get_channel_line_counts()

                    var y = 0
                    for (i in 0 until channel) {
                        y += counts[i]
                    }

                    val cursor = this.cache.getCursor()
                    if (cursor != null && y + index < cursor.first) {
                        this.cache.setCursor(
                            cursor.first - 1,
                            cursor.second,
                            cursor.third
                        )
                    }

                    this.cache.detachLine(y + index)
                    lines_changed = true
                }
                1 -> {
                    val y = this.opus_manager.get_y(channel, index)

                    val cursor = this.cache.getCursor()
                    if (cursor != null && y + index < cursor.first) {
                        this.cache.setCursor(
                            cursor.first + 1,
                            cursor.second,
                            cursor.third
                        )
                    }

                    val rowView = this.buildLineView(y)
                    for (x in 0 until this.opus_manager.opus_beat_count) {
                        this.buildTreeView(rowView, y, x, listOf())
                        this.opus_manager.flag_beat_change(BeatKey(channel, index, x))
                    }
                    lines_changed = true
                }
                2 -> {
                    val y = this.opus_manager.get_y(channel, index)
                    this.buildLineView(y)
                    lines_changed = true
                }
            }
        }

        if (lines_changed) { // Redraw labels
            val line_counts = this.opus_manager.get_channel_line_counts()
            var y = 0
            for (channel in this.opus_manager.channel_order) {
                for (i in 0 until line_counts[channel]) {
                    val label = this.cache.getLineLabel(y)!!
                    if (channel != 9) {
                        label.textView.text = "$channel:$i"
                    } else {
                        val instrument = this.opus_manager.get_percussion_instrument(i)
                        label.textView.text = "P:$instrument"
                    }

                    for (x in 0 until this.opus_manager.opus_beat_count) {
                        for ((leaf, leaf_pos) in this.cache.get_all_leafs(y, x, listOf())) {
                            leaf.background = resources.getDrawable(
                                if (this.opus_manager.is_reflection(channel, i, x)) {
                                    R.drawable.leaf_reflection
                                } else if (this.opus_manager.is_reflected(channel, i, x)) {
                                    R.drawable.leaf_reflected
                                } else if (!this.opus_manager.get_tree(
                                        BeatKey(channel, i, x),
                                        leaf_pos
                                    ).is_event()
                                ) {
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
        val updated_beats: MutableSet<Int> = mutableSetOf()
        var beats_changed = false
        while (true) {
            val (index, operation) = this.opus_manager.fetch_flag_beat() ?: break
            when (operation) {
                1 -> {
                    this.newColumnLabel()
                    for (y in 0 until this.opus_manager.line_count()) {
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
                    for (y in 0 until this.opus_manager.line_count()) {
                        this.cache.removeBeatView(y, index)
                    }
                    beats_changed = true
                }
            }
        }
        if (beats_changed) {
            this.tick_resize_beats(updated_beats.toList())
        }
    }

    private fun tick_update_beats() {
        val updated_beats: MutableSet<Int> = mutableSetOf()
        var beats_changed = false
        while (true) {
            val beatkey = this.opus_manager.fetch_flag_change() ?: break
            var y = this.opus_manager.get_y(
                beatkey.channel,
                beatkey.line_offset
            )
            this.rebuildBeatView(y, beatkey.beat )

            for (linked_beatkey in this.opus_manager.get_all_linked(beatkey)) {
                updated_beats.add(linked_beatkey.beat)
            }
            beats_changed = true
        }
        if (beats_changed) {
            this.tick_resize_beats(updated_beats.toList())
        }
    }

    private fun tick_resize_beats(updated_beats: List<Int>) {
        // resize Columns
        for (b in updated_beats) {
            var max_width = 0
            for (channel in 0 until this.opus_manager.channel_lines.size) {
                for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                    val tree = this.opus_manager.get_beat_tree(BeatKey(channel, line_offset, b))
                    val size = max(1, tree.size) * tree.get_max_child_weight()
                    max_width = max(max_width, size)
                }
            }

            var y = 0
            for (channel in 0 until this.opus_manager.channel_lines.size) {
                for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
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
        val stack: MutableList<Pair<Float, List<Int>>> = mutableListOf(Pair(new_width.toFloat(), listOf()))
        val key = BeatKey(channel, line_offset, beat)
        var y = this.opus_manager.get_y(channel, line_offset)
        while (stack.isNotEmpty()) {
            val (new_size, current_position) = stack.removeFirst()
            val current_tree = this.opus_manager.get_tree(key, current_position)

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
                    if (this.opus_manager.is_reflection(channel, line_offset, beat)) {
                        R.drawable.leaf_reflection
                    } else if (this.opus_manager.is_reflected(channel, line_offset, beat)) {
                        R.drawable.leaf_reflected
                    } else if (!current_tree.is_event()) {
                         R.drawable.leaf
                    } else {
                        R.drawable.leaf_active
                    }
                )

            }

            current_view.layoutParams = param
        }
    }

    private fun tick_apply_cursor() {
        val cursor = this.opus_manager.get_cursor()
        val position = cursor.get_position()

        for ((view, leaf_pos) in this.cache.get_all_leafs(cursor.y, cursor.x, position)) {
            if (view is LinearLayout) {
                continue
            }
            val pair = this.opus_manager.get_channel_index(cursor.y)
            view.background = resources.getDrawable(
                if (this.opus_manager.is_reflection(pair.first, pair.second, cursor.x)) {
                    R.drawable.focus_leaf_reflection
                } else if (this.opus_manager.is_reflected(pair.first, pair.second, cursor.x)) {
                    R.drawable.focus_leaf_reflected
                } else if (this.opus_manager.get_tree(cursor.get_beatkey(), leaf_pos).is_event()) {
                    R.drawable.focus_leaf_active
                } else {
                    R.drawable.focus_leaf
                }
            )
        }

        this.cache.setCursor(cursor.y, cursor.x, position)
    }

    private fun tick_unapply_cursor() {
        val c = this.cache.getCursor()
        if (c != null) {
            // TODO: specify Exception
            try {
                val pair = this.opus_manager.get_channel_index(c.first)

                val drawable_id = if (this.opus_manager.is_reflection(pair.first, pair.second, c.second)) {
                    R.drawable.leaf_reflection
                } else if (this.opus_manager.is_reflected(pair.first, pair.second, c.second)) {
                    R.drawable.leaf_reflected
                } else {
                    R.drawable.leaf
                }


                for ((view, leaf_pos) in this.cache.get_all_leafs(c.first, c.second, c.third)) {
                    if (view is LinearLayout) {
                        continue
                    }

                    if (drawable_id == R.drawable.leaf && this.opus_manager.get_tree(BeatKey(pair.first, pair.second, c.second), leaf_pos).is_event()) {
                        view.background = resources.getDrawable(R.drawable.leaf_active)
                    } else {
                        view.background = resources.getDrawable(drawable_id)
                    }
                }
            } catch (exception:Exception) {
                this.cache.unsetCursor()
            }
        }
    }

    private fun export_midi() {
        val CREATE_FILE = 2

        var name = this.opus_manager.get_working_dir()
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/midi"
            putExtra(Intent.EXTRA_TITLE, "$name.mid")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
        }

        startActivityForResult(intent, CREATE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).write(this.opus_manager.get_midi().as_bytes())
                    Toast.makeText(this, "Exported to midi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun interact_rosRelativeOption(view: RelativeOptionSelector) {
        this.resize_relative_value_selector()
        var current_tree = this.opus_manager.get_tree_at_cursor()
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

        if (checkstate_and_value.first == view.getState()) {
            var valueSelector: NumberSelector = findViewById(R.id.nsRelativeValue)
            try {
                valueSelector.setState(checkstate_and_value.second)
            } catch (e: Exception) {
                valueSelector.unset_active_button()
            }
        }
    }

    private fun change_relative_value(progress: Int) {
        val cursor = opus_manager.get_cursor()
        val beat_key = cursor.get_beatkey()
        var relativeOptionSelector: RelativeOptionSelector = findViewById(R.id.rosRelativeOption)
        val new_value = when (relativeOptionSelector.getState()) {
            0 -> { progress }
            1 -> { 0 - progress }
            2 -> { this.opus_manager.RADIX * progress }
            3 -> { 0 - (this.opus_manager.RADIX * progress) }
            else -> { progress }
        }

        val event = OpusEvent(
            new_value,
            this.opus_manager.RADIX,
            beat_key.channel,
            true
        )

        this.play_event(beat_key.channel, event.note)

        this.opus_manager.set_event(beat_key, cursor.position, event)
        this.tick()
    }

    private fun interact_btnSplit(view: View) {
        this.opus_manager.split_tree_at_cursor()
        this.tick()
    }

    private fun interact_btnUnset(view: View) {
        this.setContextMenu(ContextMenu.Leaf)
        var cursor = this.opus_manager.get_cursor()
        if (cursor.get_beatkey().channel != 9 || this.opus_manager.get_tree_at_cursor().is_event()) {
            this.opus_manager.unset_at_cursor()
        } else {
            this.opus_manager.set_percussion_event_at_cursor()
        }

        this.tick()
        this.setContextMenu(ContextMenu.Leaf)
    }

    private fun interact_btnUnlink(view: View) {
        val cursor = this.opus_manager.get_cursor()
        this.opus_manager.unlink_beat(cursor.get_beatkey())
        cursor.settle()
        this.linking_beat = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()

    }

    private fun interact_btnCancelLink(view: View) {
        this.opus_manager.get_cursor().settle()
        this.linking_beat = null
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnInsertBeat(view: View) {
        this.opus_manager.insert_beat_at_cursor()
        this.tick()
    }

    private fun interact_btnRemoveBeat(view: View) {
        if (this.opus_manager.opus_beat_count > 1) {
            this.opus_manager.remove_beat_at_cursor()
        }
        this.tick()
    }

    private fun interact_btnRemoveLine(view: View) {
        if (this.opus_manager.line_count() > 1) {
            this.opus_manager.remove_line_at_cursor()
        }
        this.tick()
    }

    private fun interact_btnInsertLine(view: View) {
        this.opus_manager.new_line_at_cursor()
        this.tick()
    }

    private fun interact_btnChooseInstrument(view: View) {
        val popupMenu = PopupMenu(window.decorView.rootView.context, view)
        val cursor = this.opus_manager.get_cursor()
        if (cursor.get_beatkey().channel == 9) {
            val drums = resources.getStringArray(R.array.midi_drums)
            drums.forEachIndexed { i, string ->
                popupMenu.menu.add(0, i, i, string)
            }

            popupMenu.setOnMenuItemClickListener {
                this.opus_manager.set_percussion_instrument(
                    cursor.get_beatkey().line_offset,
                    it.itemId
                )
                this.tick()
                val y = this.opus_manager.get_cursor().get_y()
                this.cache.getLineLabel(y)!!.textView.text = "P:${it.itemId}"
                this.setContextMenu(ContextMenu.Line) // TODO: overkill?
                true
            }
        } else {
            val instruments = resources.getStringArray(R.array.midi_instruments)
            instruments.forEachIndexed { i, string ->
                popupMenu.menu.add(0, i, i, string)
            }

            popupMenu.setOnMenuItemClickListener {
                this.opus_manager.set_channel_instrument(
                    cursor.get_beatkey().channel,
                    it.itemId
                )

                this.tick()
                this.setContextMenu(ContextMenu.Line) //TODO: Overkill?
                true
            }
        }
        popupMenu.show()
    }

    private fun interact_leafView_click(view: View) {
        val key = this.cache.getTreeViewYXPosition(view) ?: return
        if (this.linking_beat != null) {
            val pair = this.opus_manager.get_channel_index(key.first)
            val working_position = BeatKey(
                pair.first,
                pair.second,
                key.second
            )

            this.opus_manager.link_beats(this.linking_beat!!, working_position)
            this.linking_beat = null
       }

        this.opus_manager.set_cursor_position(key.first, key.second, key.third)

        var cursor_tree = this.opus_manager.get_tree_at_cursor()
        if (cursor_tree.is_event()) {
            this.play_event(cursor_tree.get_event()!!.channel, cursor_tree.get_event()!!.note)
        }
        this.tick()
        this.setContextMenu(ContextMenu.Leaf)
    }

    fun interact_leafView_longclick(view: View) {
        val key = this.cache.getTreeViewYXPosition(view) ?: return
        val pair = this.opus_manager.get_channel_index(key.first)
        this.linking_beat = BeatKey(pair.first, pair.second, key.second)
        this.opus_manager.set_cursor_position(key.first, key.second, listOf())
        this.setContextMenu(ContextMenu.Linking)
        this.tick()
    }

    public fun interact_nsOffset(view: NumberSelector) {
        val progress = view.getState()!!
        var current_tree = this.opus_manager.get_tree_at_cursor()

        val cursor = this.opus_manager.get_cursor()
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

        this.opus_manager.set_event(beatkey, position, event)

        this.play_event(beatkey.channel, event.note)

        var nsOctave: NumberSelector = findViewById(R.id.nsOctave)
        if (nsOctave.getState() == null) {
            nsOctave.setState(event.note / event.radix)
        }

        this.tick()
    }

    public fun interact_nsOctave(view: NumberSelector) {
        val progress = view.getState() ?: return

        var current_tree = this.opus_manager.get_tree_at_cursor()
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

        this.opus_manager.set_event(beatkey, position, event)
        var nsOffset: NumberSelector = findViewById(R.id.nsOffset)
        if (nsOffset.getState() == null) {
            nsOffset.setState(event.note % event.radix)
        }

        this.tick()
    }

    private fun interact_rowLabel(view: View) {
        var abs_y: Int = 0
        val label_column = view.parent!! as ViewGroup
        for (i in 0 until label_column.childCount) {
            if (label_column.getChildAt(i) == view) {
                abs_y = i
                break
            }
        }

        val cursor = this.opus_manager.get_cursor()
        this.opus_manager.set_cursor_position(abs_y, cursor.x, listOf())
        this.tick()
        this.setContextMenu(ContextMenu.Line)
    }

    private fun interact_sRelative_changed(view: View, isChecked: Boolean) {
        if (isChecked) {
            this.opus_manager.convert_event_at_cursor_to_relative()
        } else {
            try {
                this.opus_manager.convert_event_at_cursor_to_absolute()
            } catch (e: Exception) {
                Toast.makeText(this, "Can't convert event", Toast.LENGTH_SHORT).show()
                (view as ToggleButton).isChecked = true
                return
            }
        }
        this.relative_mode = isChecked
        this.setContextMenu(ContextMenu.Leaf)
        this.tick()
    }

    private fun interact_btnRemove(view: View) {
        val cursor = this.opus_manager.get_cursor()
        if (cursor.get_position().isNotEmpty()) {
            this.opus_manager.remove_tree_at_cursor()
        }
        this.tick()
    }

    private fun interact_btnInsert(view: View) {
        val position = this.opus_manager.get_cursor().get_position()
        if (position.isEmpty()) {
            this.opus_manager.split_tree_at_cursor()
        } else {
            this.opus_manager.insert_after_cursor()
        }
        this.tick()
    }

    public fun interact_nsRelativeValue(view: NumberSelector) {
        this.change_relative_value(view.getState()!!)
    }

    fun play_beat(beat: Int) {
        if (! this.in_playback) {
            this.in_playback = true
            var midi = this.opus_manager.get_midi(beat, beat + 1)
            this.midi_player.play_midi(midi)
            this.in_playback = false
        }
    }

    fun play_event(channel: Int, event_value: Int) {
        if (! this.in_playback) {
            this.in_playback = true
            this.midi_input_device.sendEvent(NoteOn(channel, event_value + 21, 64))
            thread {
                Thread.sleep(200)
                this.midi_input_device.sendEvent(NoteOff(channel, event_value + 21, 64))
                this.in_playback = false
            }
        }
    }
}

class RadMidiController(context: Context): MIDIController(context) { }

class MIDIInputDevice: VirtualMIDIDevice() {}
