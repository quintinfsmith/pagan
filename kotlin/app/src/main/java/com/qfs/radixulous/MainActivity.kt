package com.qfs.radixulous

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.qfs.radixulous.apres.*
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.structure.OpusTree
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.channel_ctrl.view.*
import kotlinx.android.synthetic.main.contextmenu_cell.*
import kotlinx.android.synthetic.main.contextmenu_cell.view.*
import kotlinx.android.synthetic.main.contextmenu_column.view.*
import kotlinx.android.synthetic.main.contextmenu_linking.view.*
import kotlinx.android.synthetic.main.contextmenu_row.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.item_opustree.view.*
import kotlinx.android.synthetic.main.load_project.view.*
import kotlinx.android.synthetic.main.numberline_item.view.*
import kotlinx.android.synthetic.main.table_cell_label.view.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.max
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager


class ViewCache {
    var view_cache: MutableList<Pair<LinearLayout, MutableList<Pair<View?, HashMap<List<Int>, View>>>>> = mutableListOf()
    var line_label_cache: MutableList<Button> = mutableListOf()
    var column_label_cache: MutableList<View> = mutableListOf()
    var header_row: TableRow? = null
    var _cursor: Triple<Int, Int, List<Int>>? = null
    private var active_context_menu_view: View? = null

    fun get_all_leafs(y: Int, x: Int, position: List<Int>): List<View> {
        var output: MutableList<View> = mutableListOf()
        if (y >= this.view_cache.size || x >= this.view_cache[y].second.size) {
            return output
        }
        for ((key_pos, view) in this.view_cache[y].second[x].second) {
            if (position.size <= key_pos.size && key_pos.subList(0, position.size) == position) {
                output.add(view)
            }
        }
        if (output.isEmpty() && position.isEmpty()) {
            output.add(this.view_cache[y].second[x].first!!)
        }

        return output
    }

    fun setActiveContextMenu(view: View) {
        this.active_context_menu_view = view
    }

    fun getActiveContextMenu(): View? {
        return this.active_context_menu_view
    }

    fun getHeaderRow(): TableRow? {
        return this.header_row
    }

    fun setHeaderRow(row: TableRow) {
        this.header_row = row
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

    fun getLine(y: Int): LinearLayout? {
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
    fun addLineLabel(y: Int, view: Button) {
        this.line_label_cache.add(y, view)
    }

    fun getLineLabel(y: Int): Button? {
        return if (this.line_label_cache.size <= y) {
            null
        } else {
            this.line_label_cache[y]
        }
    }

    fun detachLine(y: Int) {
        val view = this.view_cache.removeAt(y).first
        this.line_label_cache.removeAt(y)
        (view.parent as ViewGroup).removeView(view)
    }

    fun removeBeatView(y: Int, x: Int) {
        val beat_view = this.view_cache[y].second.removeAt(x).first
        (beat_view?.parent as ViewGroup).removeView(beat_view)
    }

    fun getBeatView(y: Int, x: Int): View? {
        return this.view_cache[y].second[x].first
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

    fun getLineIndex(view: LinearLayout): Int? {
        for (i in 0 until this.view_cache.size) {
            if (this.view_cache[i].first == view) {
                return i
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
    private var active_context_menu_index: Int = 0
    private var linking_beat: BeatKey? = null
    private var relative_mode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // calling this activity's function to
        // use ActionBar utility methods
        val actionBar = supportActionBar!!
        //actionBar.setIcon(R.drawable.app_logo)
        // methods to display the icon in the ActionBar
        actionBar.setDisplayUseLogoEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        this.load("/data/data/com.qfs.radixulous/projects/test")
    }

    fun load(path: String) {
        this.opus_manager.load(path)
        this.buildHeader()
        this.tick()
        this.setContextMenu(3)

        var name = this.opus_manager.get_working_dir()
        if (name != null) {
            name = name.substring(name.lastIndexOf("/") + 1)
        }
        val actionBar = supportActionBar
        actionBar!!.title = name
    }

    fun save() {
        this.opus_manager.save()
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
            this.setContextMenu(3)
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
        var directory = File(projects_dir)
        var i = 0
        while (File("$projects_dir/opus$i").isFile) {
            i += 1
        }
        this.opus_manager.path = "$projects_dir/opus$i"
        val actionBar = supportActionBar
        actionBar!!.title = "opus$i"

        this.setContextMenu(3)
    }

    private fun takedownCurrent() {
        this.setContextMenu(0)
        this.tlOpusLines.removeAllViews()
        this.cache = ViewCache()
        //this.buildHeader()
    }

    private fun buildHeader() {
        val row = TableRow(this.tlOpusLines.context)
        this.tlOpusLines.addView(row)
        this.cache.setHeaderRow(row)

        val action_button = Button(row.context)
        action_button.setOnClickListener {
            //this.openFileBrowser()
            this.showChannelPopup(action_button)
            //this.file_test()
        }
        action_button.text = getString(R.string.label_channels_button)
        row.addView(action_button)
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

        var directory = File(projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }
        for (file_name in directory.list()!!) {
            var file = File("$projects_dir/$file_name")
            if (!file.isDirectory) {
                continue
            }
            // TODO: Check if directory is project directory

            var row = TextView(popupView.svProjectList.llProjectList.context)
            row.text = file_name
            row.setOnClickListener {
                this.takedownCurrent()
                this.load("$projects_dir/$file_name")
                popupWindow.dismiss()
            }
            popupView.svProjectList.llProjectList.addView(row)
        }

        popupWindow.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)

        popupView.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    fun newColumnLabel() {
        val parent = this.cache.getHeaderRow() ?: throw Exception("Header Not initialized")
        val headerCellView = LayoutInflater.from(parent.context).inflate(
            R.layout.table_cell_label,
            parent,
            false
        ) as TextView
        val x = parent.childCount - 1
        headerCellView.text = "$x"
        headerCellView.setOnClickListener {
            val cursor = this.opus_manager.get_cursor()
            this.opus_manager.set_cursor_position(cursor.y, x, listOf())
            this.setContextMenu(2)
            this.tick()
        }
        this.cache.addColumnLabel(headerCellView)
        parent.addView(headerCellView)
    }

    fun buildLineView(y: Int): TableRow {
        var clo = this.opus_manager.get_channel_index(y)
        var channel = clo.first
        var line_offset = clo.second

        var rowView = TableRow(this.tlOpusLines.context)
        rowView.setPadding(0,0,0,0)

        this.tlOpusLines.addView(rowView, y + 1)
        this.cache.cacheLine(rowView, y)

        var rowLabel = LayoutInflater.from(rowView.context).inflate(
            R.layout.table_cell_label,
            rowView,
            false
        )

        var that = this
        if (channel != 9) {
            if (line_offset == 0) {
                rowLabel.textView.text = "$channel:$line_offset"
            } else {
                rowLabel.textView.text = "  :$line_offset"
            }
        } else {
            var instrument = that.opus_manager.get_percussion_instrument(line_offset)
            rowLabel.textView.text = "P:$instrument"
        }

        rowLabel.textView.setOnClickListener {
            val y: Int? = that.cache.getLineIndex(rowView)
            val cursor = that.opus_manager.get_cursor()
            if (y != null) {
                this.opus_manager.set_cursor_position(y, cursor.x, listOf())
            }
            this.tick()
            this.setContextMenu(1)
        }

        this.cache.addLineLabel(y, rowLabel.textView)
        rowView.addView(rowLabel)

        return rowView
    }

    private fun buildTreeView(parent: ViewGroup, y: Int, x: Int, position: List<Int>): View {
        var channel_index = this.opus_manager.get_channel_index(y)
        var tree = this.opus_manager.get_tree(BeatKey(channel_index.first, channel_index.second, x), position)

        if (tree.is_leaf()) {
            var leafView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_opusbutton,
                parent,
                false
            )
            leafView.setBackgroundColor(
                if (this.opus_manager.is_reflection(channel_index.first, channel_index.second, x)) {
                    ContextCompat.getColor(leafView.context, R.color.leaf_linked)
                } else {
                    ContextCompat.getColor(leafView.context, R.color.leaf)
                }
            )

            if (tree.is_event()) {
                val event = tree.get_event()!!
                leafView.llLeaf.tvLeaf.text = if (event.relative) {
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
                leafView.llLeaf.tvLeaf.text = getString(R.string.empty_note)
            }

            leafView.setOnClickListener {
                var key = this.cache.getTreeViewYXPosition(leafView)
                if (key != null) {
                    if (this.linking_beat != null) {
                        var pair = this.opus_manager.get_channel_index(key.first)
                        var working_position = BeatKey(
                            pair.first,
                            pair.second,
                            key.second
                        )
                        this.opus_manager.link_beats(this.linking_beat!!, working_position)
                        this.linking_beat = null
                    }

                    this.opus_manager.set_cursor_position(key.first, key.second, key.third)
                    this.setContextMenu(3)

                    this.tick()
                }
            }

            leafView.setOnLongClickListener {
                val key = this.cache.getTreeViewYXPosition(leafView)
                if (key != null) {
                    var pair = this.opus_manager.get_channel_index(key.first)
                    this.linking_beat = BeatKey(pair.first, pair.second, key.second)
                    this.opus_manager.set_cursor_position(key.first, key.second, listOf())
                    this.setContextMenu(4)
                    this.tick()
                }
                true
            }

            if (position.isEmpty()) {
                parent.addView(leafView, x + 1) // (+1 considers row label)
            } else {
                parent.addView(leafView)
            }

            this.cache.cacheTree(leafView, y, x, position)

            return leafView
        } else {
            var cellLayout = LinearLayout(parent.context)
            this.cache.cacheTree(cellLayout, y, x, position)

           // if (tree.size > 1) {
           //     val open_brace = TextView(parent.context)
           //     open_brace.text = "["
           //     cellLayout.addView(open_brace)
           // }
            var max_weight = tree.get_max_child_weight()
            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                var tree_view = this.buildTreeView(cellLayout as ViewGroup, y, x, new_position)
                //if (max_weight > 1) {
                //    tree_view.layoutParams.width = max_weight * 100
                //}
            }

           // if (tree.size > 1) {
           //     val close_brace = TextView(parent.context)
           //     close_brace.text = "]"
           //     cellLayout.addView(close_brace)
           // }

            if (position.isEmpty()) {
                parent.addView(cellLayout, x + 1) // (+1 considers row label)
            } else {
                parent.addView(cellLayout)
            }

            return cellLayout
        }

    }

    fun setContextMenu(menu_index: Int) {
        this.active_context_menu_index = menu_index
        val view_to_remove = this.cache.getActiveContextMenu()
        (view_to_remove?.parent as? ViewGroup)?.removeView(view_to_remove)

        when (menu_index) {
            1 -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    this.llContextMenu,
                    false
                )

                view.btnRemoveLine.setOnClickListener {
                    if (this.opus_manager.line_count() > 1) {
                        this.opus_manager.remove_line_at_cursor()
                    }
                    this.tick()
                }

                view.btnInsertLine.setOnClickListener {
                    this.opus_manager.new_line_at_cursor()
                    this.tick()
                }

                var beatkey = this.opus_manager.get_cursor().get_beatkey()
                if (beatkey.channel == 9) {
                    var instrument = this.opus_manager.get_percussion_instrument(beatkey.line_offset)
                    view.btnChooseInstrument.text = resources.getStringArray(R.array.midi_drums)[instrument - 35]
                } else {
                    var instrument = this.opus_manager.get_channel_instrument(beatkey.channel)
                    view.btnChooseInstrument.text = resources.getStringArray(R.array.midi_instruments)[instrument]
                }

                view.btnChooseInstrument.setOnClickListener {
                    var popupMenu = PopupMenu(window.decorView.rootView.context, it)
                    var cursor = this.opus_manager.get_cursor()
                    if (cursor.get_beatkey().channel == 9) {
                        //popupMenu.menuInflater.inflate(R.menu.percussion_instruments, popupMenu.getMenu())
                        var drums = resources.getStringArray(R.array.midi_drums)
                        drums.forEachIndexed { i, string ->
                            popupMenu.menu.add(0, i + 35, i, string)
                        }

                        popupMenu.setOnMenuItemClickListener {
                            this.opus_manager.set_percussion_instrument(
                                cursor.get_beatkey().line_offset,
                                it.itemId
                            )
                            this.tick()
                            var y = this.opus_manager.get_cursor().get_y()
                            this.cache.getLineLabel(y)!!.text = "P:${it.itemId}"
                            this.setContextMenu(1)
                            true
                        }
                    } else {
                        var instruments = resources.getStringArray(R.array.midi_instruments)
                        instruments.forEachIndexed { i, string ->
                            popupMenu.menu.add(0, i, i, string)
                        }

                        popupMenu.setOnMenuItemClickListener {
                            this.opus_manager.set_channel_instrument(
                                cursor.get_beatkey().channel,
                                it.itemId
                            )

                            this.tick()
                            var y = this.opus_manager.get_cursor().get_y()
                            this.setContextMenu(1)
                            true
                        }
                    }
                    popupMenu.show()
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            2 -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_column,
                    this.llContextMenu,
                    false
                )

                view.btnInsertBeat.setOnClickListener {
                    this.opus_manager.insert_beat_at_cursor()
                    this.tick()
                }

                view.btnRemoveBeat.setOnClickListener {
                    if (this.opus_manager.opus_beat_count > 1) {
                        this.opus_manager.remove_beat_at_cursor()
                    }
                    this.tick()
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            3 -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_cell,
                    this.llContextMenu,
                    false
                )

                val current_tree = this.opus_manager.get_tree_at_cursor()
                if (current_tree.is_event()) {
                    this.relative_mode = current_tree.get_event()!!.relative
                }

                // TODO: Specify exception
                var cursor = this.opus_manager.get_cursor()
                if (this.opus_manager.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())) {
                    view.sRelative.isChecked = this.relative_mode
                    view.sRelative.setOnCheckedChangeListener { _, isChecked ->
                        this.relative_mode = isChecked
                        if (isChecked) {
                            this.opus_manager.convert_event_at_cursor_to_relative()
                        } else {
                            this.opus_manager.convert_event_at_cursor_to_absolute()
                        }
                        this.setContextMenu(3)
                        this.tick()
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
                        if (this.opus_manager.get_tree_at_cursor().is_event()) {
                            this.opus_manager.unset_at_cursor()
                        } else {
                            this.opus_manager.set_percussion_event_at_cursor()
                        }
                        this.tick()
                        this.setContextMenu(3)
                    }

                } else if (!this.relative_mode) {
                    view.llRelativePalette.visibility = View.GONE

                    if (current_tree.is_event()) {
                        val event = current_tree.get_event()!!
                        if (!event.relative) {
                            view.sbOffset.progress = event.note % event.radix
                            view.sbOctave.progress = event.note / event.radix
                        }
                    }

                    var that = this
                    view.llAbsolutePalette.sbOffset?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seek: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                        }

                        override fun onStartTrackingTouch(seek: SeekBar) {}
                        override fun onStopTrackingTouch(seek: SeekBar) {
                            var progress = seek.progress

                            var cursor = that.opus_manager.get_cursor()
                            var position = cursor.position
                            var beatkey = cursor.get_beatkey()

                            var event = if (current_tree.is_event()) {
                                var event = current_tree.get_event()!!
                                var old_octave = event.note / event.radix
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

                            that.opus_manager.set_event(beatkey, position, event)

                            that.tick()
                        }
                    }
                    )


                    view.llAbsolutePalette.sbOctave?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seek: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                        }

                        override fun onStartTrackingTouch(seek: SeekBar) {}

                        override fun onStopTrackingTouch(seek: SeekBar) {
                            var progress = seek.progress

                            var cursor = opus_manager.get_cursor()
                            var position = cursor.position
                            var beatkey = cursor.get_beatkey()

                            var event = if (current_tree.is_event()) {
                                var event = current_tree.get_event()!!
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

                            that.opus_manager.set_event(beatkey, position, event)

                            that.tick()
                        }
                    }
                    )

                    val numberLine = view.llAbsolutePalette.clNumberLine.row
                    for (i in 0 until this.opus_manager.RADIX) {
                        val leafView = LayoutInflater.from(numberLine.context).inflate(
                            R.layout.numberline_item,
                            numberLine,
                            false
                        )

                        leafView.tv.text = get_number_string(i, this.opus_manager.RADIX, 2)
                        numberLine.addView(leafView)
                    }
                } else {
                    view.llAbsolutePalette.visibility = View.GONE

                    view.llRelativePalette.rgRelOptions.check(R.id.rbAdd)
                    if (current_tree.is_event()) {
                        val event = current_tree.get_event()!!
                        if (event.relative) {
                            var selected_button = R.id.rbAdd
                            var new_progress = if (event.note > 0) {
                                if (event.note >= event.radix) {
                                    selected_button = R.id.rbPow
                                    event.note / event.radix
                                } else {
                                    event.note
                                }
                            } else if (event.note < 0) {
                                if (event.note <= 0 - event.radix) {
                                    selected_button = R.id.rbLog
                                    event.note / (0 - event.radix)
                                } else {
                                    selected_button = R.id.rbSubtract
                                    0 - event.note
                                }
                            } else {
                                0
                            }
                            view.llRelativePalette.sbRelativeValue.progress = new_progress
                            view.llRelativePalette.rgRelOptions.check(selected_button)
                        }
                    }


                    val numberLine = view.llRelativePalette.tlNumberLineRel.rowRel
                    for (i in 0 until this.opus_manager.RADIX) {
                        val leafView = LayoutInflater.from(numberLine.context).inflate(
                            R.layout.numberline_item,
                            numberLine,
                            false
                        )

                        leafView.tv.text = get_number_string(i, this.opus_manager.RADIX, 2)
                        numberLine.addView(leafView)
                    }

                    var that = this
                    view.llRelativePalette.sbRelativeValue?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seek: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) { }

                        override fun onStartTrackingTouch(seek: SeekBar) {}

                        override fun onStopTrackingTouch(seek: SeekBar) {
                            var progress = seek.progress
                            var radio_button: Int = that.llRelativePalette.rgRelOptions.checkedRadioButtonId

                            var cursor = opus_manager.get_cursor()
                            var beatkey = cursor.get_beatkey()

                            var new_value = when (radio_button) {
                                R.id.rbAdd -> {
                                    progress
                                }
                                R.id.rbSubtract -> {
                                    0 - progress
                                }
                                R.id.rbPow -> {
                                    that.opus_manager.RADIX * progress
                                }
                                R.id.rbLog -> {
                                    0 - (that.opus_manager.RADIX * progress)
                                }
                                else -> {
                                    0
                                    // Should be Unreachable
                                }
                            }


                            var event = OpusEvent(
                                new_value,
                                that.opus_manager.RADIX,
                                beatkey.channel,
                                true
                            )

                            that.opus_manager.set_event(beatkey, cursor.position, event)
                            that.tick()
                        }
                    }
                    )
                    view.rgRelOptions.setOnCheckedChangeListener { _, checkedId ->
                        var progress = view.llRelativePalette.sbRelativeValue.progress

                        var cursor = this.opus_manager.get_cursor()
                        var beatkey = cursor.get_beatkey()

                        var new_value = when (checkedId) {
                            R.id.rbAdd -> {
                                progress
                            }
                            R.id.rbSubtract -> {
                                0 - progress
                            }
                            R.id.rbPow -> {
                                that.opus_manager.RADIX * progress
                            }
                            R.id.rbLog -> {
                                0 - (that.opus_manager.RADIX * progress)
                            }
                            else -> {
                                0
                                // Should be Unreachable
                            }
                        }

                        var event = OpusEvent(
                            new_value,
                            that.opus_manager.RADIX,
                            beatkey.channel,
                            true
                        )

                        that.opus_manager.set_event(beatkey, cursor.position, event)
                        that.tick()
                    }
                }

                view.clButtons.btnSplit?.setOnClickListener {
                    this.opus_manager.split_tree_at_cursor()
                    this.tick()
                }

                view.clButtons.btnUnset?.setOnClickListener {
                    if (this.opus_manager.get_cursor().get_beatkey().channel != 9 || this.opus_manager.get_tree_at_cursor().is_event()) {
                        this.opus_manager.unset_at_cursor()
                    } else {
                        this.opus_manager.set_percussion_event_at_cursor()
                    }

                    this.tick()
                    this.setContextMenu(3)
                }

                view.clButtons.btnRemove?.setOnClickListener {
                    val cursor = this.opus_manager.get_cursor()
                    if (cursor.get_position().isNotEmpty()) {
                        this.opus_manager.remove_tree_at_cursor()
                    }
                    this.tick()
                }

                view.clButtons.btnInsert?.setOnClickListener {
                    val position = this.opus_manager.get_cursor().get_position()
                    if (position.isEmpty()) {
                        this.opus_manager.split_tree_at_cursor()
                    } else {
                        this.opus_manager.insert_after_cursor()
                    }
                    this.tick()
                }



                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)

            }
            4 -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_linking,
                    this.llContextMenu,
                    false
                )

                val cursor_key = this.opus_manager.get_cursor().get_beatkey()
                if (this.opus_manager.is_reflection(cursor_key.channel, cursor_key.line_offset, cursor_key.beat)) {
                    view.btnUnLink.setOnClickListener {
                        var cursor = this.opus_manager.get_cursor()
                        this.opus_manager.unlink_beat(cursor.get_beatkey())
                        cursor.settle()
                        this.linking_beat = null
                        this.setContextMenu(3)
                        this.tick()
                    }
                } else {
                    view.btnUnLink.visibility = View.GONE
                }

                view.btnCancelLink.setOnClickListener {
                    this.opus_manager.get_cursor().settle()
                    this.linking_beat = null
                    this.setContextMenu(3)
                    this.tick()
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
        }
    }

    fun rebuildBeatView(y: Int, x: Int) {
        var pair = this.opus_manager.get_channel_index(y)
        var main_beatkey = BeatKey(pair.first, pair.second, x)
        for (beatkey in this.opus_manager.get_all_linked(main_beatkey)) {
            var new_y = this.opus_manager.get_y(beatkey.channel, beatkey.line_offset)
            var new_x = beatkey.beat
            this.cache.removeBeatView(new_y, new_x)
            var rowView = this.cache.getLine(new_y)!!
            this.buildTreeView(rowView, new_y, new_x, listOf())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 777) {
            val filePath = data?.data?.path
            this.opus_manager.load(filePath!!)
        }
    }

    private fun tick() {
        this.tick_unapply_cursor()
        this.tick_manage_lines()
        this.tick_manage_beats() // new/pop
        this.tick_update_beats() // changes
        this.tick_apply_cursor()
    }

    fun tick_manage_lines() {
        while (true) {
            val (channel, index, operation) = this.opus_manager.fetch_flag_line() ?: break
            when (operation) {
                0 -> {
                    var counts = this.opus_manager.get_channel_line_counts()

                    var y = 0
                    for (i in 0 until channel) {
                        y += counts[i]
                    }

                    var cursor = this.cache.getCursor()
                    if (cursor != null && y + index < cursor.first) {
                        this.cache.setCursor(
                            cursor.first - 1,
                            cursor.second,
                            cursor.third
                        )
                    }


                    this.cache.detachLine(y + index)

                    // Redraw labels
                    // TODO: Redraw labels at end of function after all lines have been popped and added
                    if (this.opus_manager.channel_lines[channel].isNotEmpty()) {
                        val initial_y = this.opus_manager.get_y(channel, 0)
                        for (j in 0 until this.opus_manager.channel_lines[channel].size) {
                            val line_offset = initial_y + j
                            val label = this.cache.getLineLabel(line_offset)?: continue
                            if (channel != 9) {
                                label.text = "$channel:$j"
                            } else {
                                var instrument = this.opus_manager.get_percussion_instrument(j)
                                label.text = "P:$instrument"
                            }
                        }
                    }
                }
                1 -> {
                    val y = this.opus_manager.get_y(channel, index)

                    var cursor = this.cache.getCursor()
                    if (cursor != null && y + index < cursor.first) {
                        this.cache.setCursor(
                            cursor.first + 1,
                            cursor.second,
                            cursor.third
                        )
                    }

                    var rowView = this.buildLineView(y)
                    for (x in 0 until this.opus_manager.opus_beat_count) {
                        this.buildTreeView(rowView, y, x, listOf())
                    }
                }
                2 -> {
                    val y = this.opus_manager.get_y(channel, index)
                    this.buildLineView(y)
                }
            }
        }
    }


    fun tick_manage_beats() {
        var updated_beats: MutableSet<Int> = mutableSetOf()
        while (true) {
            val (index, operation) = this.opus_manager.fetch_flag_beat() ?: break
            when (operation) {
                1 -> {
                    this.newColumnLabel()
                    for (y in 0 until this.opus_manager.line_count()) {
                        var rowView = this.cache.getLine(y)?: continue
                        this.buildTreeView(rowView, y, index, listOf())
                    }
                    updated_beats.add(index)
                }
                0 -> {
                    this.cache.detachColumnLabel()
                    for (y in 0 until this.opus_manager.line_count()) {
                        this.cache.removeBeatView(y, index)
                    }
                }
            }
        }
        this.tick_resize_beats(updated_beats.toList())
    }

    private fun tick_update_beats() {
        var updated_beats: MutableSet<Int> = mutableSetOf()
        while (true) {
            val beatkey = this.opus_manager.fetch_flag_change() ?: break
            this.rebuildBeatView(
                this.opus_manager.get_y(
                    beatkey.channel,
                    beatkey.line_offset
                ),
                beatkey.beat
            )

            updated_beats.add(beatkey.beat)

        }
        this.tick_resize_beats(updated_beats.toList())
    }
    fun tick_resize_beats(updated_beats: List<Int>) {
        // resize Columns
        var line_count = this.opus_manager.line_count()
        for (b in updated_beats) {
            var max_width = 0
            for (channel in 0 until this.opus_manager.channel_lines.size) {
                for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                    var tree = this.opus_manager.get_beat_tree(BeatKey(channel, line_offset, b))
                    var size = tree.size * tree.get_max_child_weight()
                    max_width = max(max_width, size)
                }
            }

            var y = 0
            for (channel in 0 until this.opus_manager.channel_lines.size) {
                for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                    var stack: MutableList<Pair<Float, List<Int>>> = mutableListOf(Pair(max_width.toFloat(), listOf()))
                    var key = BeatKey(channel, line_offset, b)

                    while (stack.isNotEmpty()) {
                        var (new_size, current_position) = stack.removeFirst()
                        var current_tree = this.opus_manager.get_tree(key, current_position)

                        if (!current_tree.is_leaf()) {
                            for (i in 0 until current_tree.size) {
                                var next_pos = current_position.toMutableList()
                                next_pos.add(i)
                                stack.add(Pair(new_size / current_tree.size.toFloat(), next_pos))
                            }
                        }
                        var current_view = this.cache.getTreeView(y, b, current_position)
                        current_view!!.layoutParams.width = (new_size * 100.toFloat()).toInt()
                    }

                    y += 1
                }
            }
        }
    }

    fun tick_apply_cursor() {
        val cursor = this.opus_manager.get_cursor()
        val position = cursor.get_position()

        for (view in this.cache.get_all_leafs(cursor.y, cursor.x, position)) {
            val pair = this.opus_manager.get_channel_index(cursor.y)
            val color = if (this.opus_manager.is_reflection(pair.first, pair.second, cursor.x)) {
                R.color.leaf_linked_selected
            } else {
                R.color.leaf_selected
            }
            val changeColour = ContextCompat.getColor(view.context, color)
            view.setBackgroundColor(changeColour)
        }


        this.cache.setCursor(cursor.y, cursor.x, position)

    }

    fun tick_unapply_cursor() {
        val c = this.cache.getCursor()
        if (c != null) {
            // TODO: specify Exception
            try {
                val pair = this.opus_manager.get_channel_index(c.first)
                val color = if (this.opus_manager.is_reflection(pair.first, pair.second, c.second)) {
                    R.color.leaf_linked
                } else {
                    R.color.leaf
                }
                for (view in this.cache.get_all_leafs(c.first, c.second, c.third)) {
                    val changeColour = ContextCompat.getColor(view.context, color)
                    view.setBackgroundColor(changeColour)
                }
            } catch (exception:Exception) {
                this.cache.unsetCursor()
            }
        }
    }

    fun setRelative(relative: Boolean) {
        this.relative_mode = relative
    }

    fun export_midi() {
        var filea = File("/data/data/com.qfs.radixulous/projects/miditest.mid")

        Log.e("AAA", "${filea.createNewFile()}")
        filea.writeBytes(this.opus_manager.get_midi().as_bytes())
        //val CREATE_FILE = 1

        //var name = this.opus_manager.get_working_dir()
        //if (name != null) {
        //    name = name.substring(name.lastIndexOf("/") + 1)
        //}
        //val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        //    addCategory(Intent.CATEGORY_OPENABLE)
        //    type = "application/midi"
        //    putExtra(Intent.EXTRA_TITLE, "$name.mid")
        //    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
        //}
        //startActivityForResult(intent, CREATE_FILE)
        //val contentResolver = applicationContext.contentResolver
        //if (intent.data != null) {
        //    var midi = this.opus_manager.get_midi()
        //    contentResolver.openFileDescriptor(intent.data!!, "w")?.use {
        //        FileOutputStream(it.fileDescriptor).use { it ->
        //            it.write(midi.as_bytes())
        //        }
        //    }
        //}


    }


}
