package com.qfs.radixulous

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.channel_ctrl.view.*
import kotlinx.android.synthetic.main.contextmenu_cell.*
import kotlinx.android.synthetic.main.contextmenu_cell.view.*
import kotlinx.android.synthetic.main.contextmenu_column.view.*
import kotlinx.android.synthetic.main.contextmenu_row.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.item_opustree.view.*
import kotlinx.android.synthetic.main.numberline_item.view.*
import kotlinx.android.synthetic.main.table_cell_label.view.*
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import com.qfs.radixulous.apres.*

import android.content.Intent
import kotlinx.android.synthetic.main.contextmenu_linking.view.*
import java.lang.Math.abs

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
                this.view_cache[y].second.add(x, Pair(view, HashMap<List<Int>, View>()))
            } else {
                this.view_cache[y].second.add(Pair(view, HashMap<List<Int>, View>()))
            }
        } else {
            this.view_cache[y].second[x].second[position] = view
        }
    }
    fun getTree(y: Int, x: Int, position: List<Int>): View? {
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
    fun getColumnLabel(x: Int): View? {
        return this.column_label_cache[x]
    }
    fun detachColumnLabel() {
        var label = this.column_label_cache.removeLast()
        (label.parent as ViewGroup).removeView(label)
    }
    fun addLineLabel(y: Int, view: Button) {
        this.line_label_cache.add(y, view)
    }
    fun getLineLabel(y: Int): Button? {
        return this.line_label_cache[y]
    }
    fun detachLine(y: Int) {
        var view = this.view_cache.removeAt(y).first
        this.line_label_cache.removeAt(y)
        (view.parent as ViewGroup).removeView(view)
    }

    fun removeBeatView(y: Int, x: Int) {
        var beat_view = this.view_cache[y].second.removeAt(x).first
        (beat_view?.parent as ViewGroup).removeView(beat_view)
    }

    fun getTreeViewYXPosition(view: View): Triple<Int, Int, List<Int>>? {
        for (y in 0 until this.view_cache.size) {
            var line_cache = this.view_cache[y].second
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
}

class MainActivity : AppCompatActivity() {
    private var opus_manager = OpusManager()
    private var cache = ViewCache()
    private var active_context_menu_index: Int = 0
    private var linking_beat: BeatKey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // calling this activity's function to
        // use ActionBar utility methods
        val actionBar = supportActionBar
        actionBar!!.title = "Radixulous"
        actionBar.subtitle = "   Doop"
        //actionBar.setIcon(R.drawable.app_logo)
        // methods to display the icon in the ActionBar
        actionBar.setDisplayUseLogoEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        this.opus_manager.load("/data/data/com.qfs.radixulous/projects/test")

        this.populateTable()
        this.update_cursor_position()
        this.setContextMenu(3)
    }

    // method to inflate the options menu when
    // the user opens the menu for the first time
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // methods to control the operations that will
    // happen when user clicks on the action buttons
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itmNewProject -> this.newProject()
            R.id.itmLoadProject -> Toast.makeText(this, "Load Clicked", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun newProject() {
        // Check to save old
        // destroy current layout
        this.takedownCurrent()
        this.opus_manager.new()
        this.populateTable()
        this.update_cursor_position()
        this.setContextMenu(3)
    }

    private fun takedownCurrent() {
        this.setContextMenu(0)
        this.tlOpusLines.removeAllViews()
        this.cache = ViewCache()
    }

    private fun buildHeader() {
        var row = TableRow(this.tlOpusLines.context)
        this.tlOpusLines.addView(row)
        this.cache.setHeaderRow(row)

        var action_button = Button(row.context)
        action_button.setOnClickListener {
            //this.openFileBrowser()
            this.showPopup(action_button)
        }
        action_button.text = "Channels"
        row.addView(action_button)

        for (i in 0 until this.opus_manager.opus_beat_count) {
            this.newColumnLabel()
        }
    }

    fun populateTable() {
        this.buildHeader()
        var y = 0
        for (channel in 0 until this.opus_manager.channel_lines.size) {
            for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                this.buildLineView(y)
                y += 1
            }
        }
    }

    fun showPopup(view: View?) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.channel_ctrl, null)

        val popupWindow = PopupWindow(
            popupView,
            MATCH_PARENT,
            MATCH_PARENT,
            true
        )
        for (i in 0 until this.opus_manager.channel_lines.size) {
            var chipView = Chip(popupView.clA.clB.cgEnabledChannels.context)
            chipView.isCheckable = true
            if (i == 9) {
                chipView.text = "Drums"
            } else {
                chipView.text = "${i}"
            }
            chipView.isChecked = this.opus_manager.channel_lines[i].isNotEmpty()

            // TODO: I suspect there is a better listener for this
            chipView.setOnClickListener {
                if (chipView.isChecked) {
                    if (this.opus_manager.channel_lines[i].isEmpty()) {
                        this.opus_manager.add_channel(i)
                        var y = this.opus_manager.get_y(i, 0)
                        this.buildLineView(y)
                    }
                } else {
                    var y = this.opus_manager.get_y(i, 0)
                    var line_count = this.opus_manager.channel_lines[i].size
                    if (this.opus_manager.line_count() > line_count) {
                        for (l in 0 until line_count) {
                            this.cache.detachLine(y)
                        }
                        this.opus_manager.remove_channel(i)
                        this.update_cursor_position()
                    } else {
                        chipView.isChecked = true
                    }
                }
            }
            popupView.clA.clB.cgEnabledChannels.addView(chipView)
        }

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

        popupView.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    // private fun showPopup(view: View) {

   //     val popup = PopupWindow(this)
   //     popup.inflate(R.menu.popup_menu)

   //     popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

   //         when (item!!.itemId) {
   //             R.id.a -> {
   //                 Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
   //             }
   //             R.id.b -> {
   //                 Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
   //             }
   //             R.id.c -> {
   //                 Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
   //             }
   //         }

   //         true
   //     })

   //     popup.show()
   // }

    fun newColumnLabel() {
        var parent = this.cache.getHeaderRow() ?: throw Exception("Header Not initialized")
        var headerCellView = LayoutInflater.from(parent.context).inflate(
            R.layout.table_cell_label,
            parent,
            false
        ) as TextView
        var x = parent.childCount - 1
        headerCellView.text = "${x}"
        headerCellView.setOnClickListener {
            this.opus_manager.jump_to_beat(x)
            var cursor = this.opus_manager.cursor
            this.update_cursor_position()
            this.setContextMenu(2)
        }
        this.cache.addColumnLabel(headerCellView)
        parent.addView(headerCellView)
    }

    fun buildLineView(y: Int) {
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
                rowLabel.textView.text = "${channel}:${line_offset}"
            } else {
                rowLabel.textView.text = "  :${line_offset}"
            }
        } else {
            var instrument = that.opus_manager.get_percussion_instrument(line_offset)
            rowLabel.textView.text = "P:${get_number_string(instrument, that.opus_manager.RADIX, 2)}"
        }

        rowLabel.textView.setOnClickListener {
            var y: Int? = that.cache.getLineIndex(rowView)
            var cursor = that.opus_manager.cursor

            this.setContextMenu(1)
            if (y != null) {
                this.opus_manager.set_cursor_position(y, cursor.x, listOf())
                this.update_cursor_position()
            }
        }
        this.cache.addLineLabel(y, rowLabel.textView)

        rowView.addView(rowLabel)

        for (x in 0 until this.opus_manager.channel_lines[channel][line_offset].size) {
            this.buildTreeView(rowView, y, x, listOf())
        }
    }

    private fun buildTreeView(parent: LinearLayout, y: Int, x: Int, position: List<Int>) {
        var channel_index = this.opus_manager.get_channel_index(y)
        var tree = this.opus_manager.get_tree(BeatKey(channel_index.first, channel_index.second, x), position)

        if (tree.is_leaf()) {
            var leafView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_opusbutton,
                parent,
                false
            )
            val changeColour = if (this.opus_manager.is_reflection(channel_index.first, channel_index.second, x)) {
                ContextCompat.getColor(leafView.button.context, R.color.leaf_linked)
            } else {
                ContextCompat.getColor(leafView.button.context, R.color.leaf)
            }
            leafView.button.setBackgroundColor(changeColour)

            if (tree.is_event()) {
                var event = tree.get_event()!!
                leafView.button.text = if (event.relative) {
                    if (event.note == 0 || event.note % event.radix != 0) {
                        var prefix = if (event.note < 0) {
                            "-"
                        } else {
                            "+"
                        }
                        "$prefix${get_number_string(abs(event.note), event.radix, 1)}"
                    } else {
                        var prefix = if (event.note < 0) {
                            "v"
                        } else {
                            "^"
                        }
                        "$prefix${get_number_string(abs(event.note) / event.radix, event.radix, 1)}"
                    }
                } else if (event.channel != 9) {
                    get_number_string(event.note, event.radix, 2)
                } else {
                    "!!"
                }
            } else {
                leafView.button.text = ".."
            }

            var that = this
            leafView.button.setOnClickListener {
                var key = that.cache.getTreeViewYXPosition(leafView)
                if (key != null) {
                    if (this.linking_beat != null) {
                        var pair = this.opus_manager.get_channel_index(key.first)
                        var working_position = BeatKey(
                            pair.first,
                            pair.second,
                            key.second
                        )
                        this.opus_manager.link_beats(this.linking_beat!!, working_position)
                        this.rebuildBeatView(
                            this.opus_manager.get_y(
                                this.linking_beat!!.channel,
                                this.linking_beat!!.line_offset
                            ),
                            this.linking_beat!!.beat
                        )
                        this.linking_beat = null
                    }
                    this.cellClickListener(key.first, key.second, key.third)
                }
            }

            leafView.button.setOnLongClickListener {
                var key = that.cache.getTreeViewYXPosition(leafView)
                if (key != null) {
                    var pair = that.opus_manager.get_channel_index(key.first)
                    this.linking_beat = BeatKey(pair.first, pair.second, key.second)
                    this.cellClickListener(key.first, key.second, listOf())
                }
                true
            }

            if (position.isEmpty()) {
                parent.addView(leafView, x + 1) // (+1 considers row label)
            } else {
                parent.addView(leafView)
            }

            this.cache.cacheTree(leafView, y, x, position)

        } else {
            var tableLayout = LayoutInflater.from(parent.context).inflate(
                R.layout.item_opustree,
                parent,
                false
            )
            this.cache.cacheTree(tableLayout, y, x, position)
            var treeLayout = tableLayout.tl.tr

            if (tree.size > 1) {
                var open_brace = TextView(parent.context)
                open_brace.text = "["
                treeLayout.addView(open_brace)
            }

            for (i in 0 until tree.size) {
                var new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(treeLayout, y, x, new_position)
            }

            if (tree.size > 1) {
                var close_brace = TextView(parent.context)
                close_brace.text = "]"
                treeLayout.addView(close_brace)
            }

            if (position.isEmpty()) {
                parent.addView(tableLayout, x + 1) // (+1 considers row label)
            } else {
                parent.addView(tableLayout)
            }
        }
    }


    fun setContextMenu(menu_index: Int) {
        this.active_context_menu_index = menu_index
        var view = this.cache.getActiveContextMenu()
        (view?.parent as? ViewGroup)?.removeView(view)

        var opus_manager = opus_manager
        var current_tree = opus_manager.get_tree_at_cursor()
        var that = this

        when (menu_index) {
            1 -> {
                var view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    this.llContextMenu,
                    false
                )

                view.apply {
                    this.btnRemoveLine.setOnClickListener {
                        if (that.opus_manager.line_count() > 1) {
                            var cursor = that.opus_manager.cursor

                            that.opus_manager.remove_line_at_cursor()
                            that.cache.detachLine(cursor.y)
                            that.update_cursor_position()

                            var beatkey = cursor.get_beatkey()
                            for (y in beatkey.line_offset until that.opus_manager.channel_lines[beatkey.channel].size) {
                                var line_offset = cursor.y - beatkey.line_offset  + y
                                var label = that.cache.getLineLabel(line_offset)?: continue
                                if (beatkey.channel != 9) {
                                    label.text = "${beatkey.channel}:${y}"
                                } else {
                                    var instrument = that.opus_manager.get_percussion_instrument(line_offset)
                                    label.text = "P:${get_number_string(instrument, that.opus_manager.RADIX, 2)}"
                                }
                            }
                        }
                    }

                    this.btnInsertLine.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var beat_key = cursor.get_beatkey()
                        that.opus_manager.new_line_at_cursor()
                        var new_line_channel = beat_key.channel
                        var new_line_offset = that.opus_manager.channel_lines[new_line_channel].size - 1
                        var line_y = that.opus_manager.get_y(new_line_channel, new_line_offset)
                        that.buildLineView(line_y)
                    }
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            2 -> {
                var view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_column,
                    this.llContextMenu,
                    false
                )

                view.apply {
                    this.btnInsertBeat.setOnClickListener {
                        that.opus_manager.insert_beat_at_cursor()
                        var cursor = that.opus_manager.cursor
                        that.newColumnLabel()
                        for (y in 0 until that.opus_manager.line_count()) {
                            var rowView = that.cache.getLine(y)?: continue
                            var tree = that.opus_manager.get_tree_at_cursor()

                            that.buildTreeView(rowView, y, cursor.x + 1, listOf())
                        }
                    }

                    this.btnRemoveBeat.setOnClickListener {
                        if (that.opus_manager.opus_beat_count > 1) {
                            var cursor = that.opus_manager.cursor

                            for (y in 0 until that.opus_manager.line_count()) {
                                that.cache.removeBeatView(y, cursor.x)
                            }

                            var label = that.cache.detachColumnLabel()
                            that.opus_manager.remove_beat_at_cursor()

                            that.update_cursor_position()
                        }
                    }
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            3 -> {
                var view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_cell,
                    this.llContextMenu,
                    false
                )

                view.apply {
                    if (current_tree.is_event()) {
                        var event = current_tree.get_event()!!
                        this.sbOffset.progress = event.note % event.radix
                        this.sbOctave.progress = event.note / event.radix
                    }

                    this.sbOffset?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seek: SeekBar,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                if (!fromUser) {
                                    return
                                }
                                var cursor = that.opus_manager.cursor
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
                                for (linked_key in that.opus_manager.get_all_linked(beatkey)) {
                                    var y = that.opus_manager.get_y(linked_key.channel, linked_key.line_offset)

                                    that.cache.getTree(y, linked_key.beat, position)?.button?.text =
                                        get_number_string(event.note, event.radix, 2)
                                }
                            }

                            override fun onStartTrackingTouch(seek: SeekBar) {

                            }

                            override fun onStopTrackingTouch(seek: SeekBar) {

                            }
                        }
                    )


                    this.sbOctave?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seek: SeekBar,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                if (!fromUser) {
                                    return
                                }
                                var cursor = opus_manager.cursor
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
                                for (linked_key in that.opus_manager.get_all_linked(beatkey)) {
                                    var y = that.opus_manager.get_y(linked_key.channel, linked_key.line_offset)

                                    that.cache.getTree(y, linked_key.beat, position)?.button?.text =
                                        get_number_string(event.note, event.radix, 2)
                                }
                            }

                            override fun onStartTrackingTouch(seek: SeekBar) {

                            }

                            override fun onStopTrackingTouch(seek: SeekBar) {

                            }
                        }
                    )

                    this.clButtons.btnSplit?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        that.opus_manager.split_tree_at_cursor()

                        that.rebuildBeatView(cursor.y, cursor.x)
                        that.cellClickListener(cursor.y, cursor.x, cursor.get_position())
                    }

                    this.clButtons.btnUnset?.setOnClickListener {
                        that.opus_manager.unset_at_cursor()

                        var cursor = that.opus_manager.cursor
                        var position = cursor.get_position()
                        that.sbOffset.progress = 0
                        that.sbOctave.progress = 0

                        that.cache.getTree(cursor.y, cursor.x, position)?.button?.text = ".."
                        that.cellClickListener(cursor.y, cursor.x, position)
                    }

                    this.clButtons.btnRemove?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var position = cursor.get_position()


                        if (position.isNotEmpty()) {
                            that.opus_manager.remove_tree_at_cursor()
                            that.rebuildBeatView(cursor.y, cursor.x)
                            that.cellClickListener(cursor.y, cursor.x, cursor.get_position())
                        }
                    }

                    this.clButtons.btnInsert?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var position = cursor.get_position()
                        if (position.isEmpty()) {
                            that.opus_manager.split_tree_at_cursor()
                        } else {
                            that.opus_manager.insert_after_cursor()
                        }

                        that.rebuildBeatView(cursor.y, cursor.x)
                        that.cellClickListener(cursor.y, cursor.x, cursor.get_position())
                    }

                    var numberLine = this.clNumberLine.row
                    for (i in 0 until that.opus_manager.RADIX) {
                        var leafView = LayoutInflater.from(numberLine.context).inflate(
                            R.layout.numberline_item,
                            numberLine,
                            false
                        )

                        leafView.tv.text = get_number_string(i, that.opus_manager.RADIX, 2)
                        numberLine.addView(leafView)
                    }
                    //this.clButtons.btnSplit?.text = "${that.opus_manager.cursor.position}"
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            4 -> {
                var view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_linking,
                    this.llContextMenu,
                    false
                )
                view.apply {
                    var cursor_key = that.opus_manager.cursor.get_beatkey()
                    if (that.opus_manager.is_reflection(cursor_key.channel, cursor_key.line_offset, cursor_key.beat)) {
                        this.btnUnLink.setOnClickListener {
                            var cursor = that.opus_manager.cursor
                            that.opus_manager.unlink_beat(cursor.get_beatkey())
                            cursor.settle()
                            that.linking_beat = null
                            that.cellClickListener(cursor.y, cursor.x, cursor.position)
                        }
                    } else {
                        this.btnUnLink.visibility = View.GONE
                    }

                    this.btnCancelLink.setOnClickListener {
                        that.opus_manager.cursor.settle()
                        that.linking_beat = null
                        var cursor = that.opus_manager.cursor
                        that.cellClickListener(cursor.y, cursor.x, cursor.position)
                    }
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

    private fun update_cursor_position() {
        var c = this.cache.getCursor()
        if (c != null) {
            var pair = this.opus_manager.get_channel_index(c.first)
            var color = if (this.opus_manager.is_reflection(pair.first, pair.second, c.second)) {
                R.color.leaf_linked
            } else {
                R.color.leaf
            }
            for (view in this.cache.get_all_leafs(c.first, c.second, c.third)) {
                val changeColour = ContextCompat.getColor(view.button.context, color)
                view.button.setBackgroundColor(changeColour)
            }
        }

        //if (this.selected_beat_key == null) {
        //} else if (key != null) {
        //    var pair = this.opus_manager.get_channel_index(key.first)
        //    var beat_key = BeatKey(pair.first, pair.second, key.second)
        //    this.opus_manager.link_beats(
        //        beat_key,
        //        this.selected_beat_key!!
        //    )
        //    var y = this.opus_manager.get_y(this.selected_beat_key!!.channel, this.selected_beat_key!!.line_offset)
        //    this.rebuildBeatView(y, key.second)

        //    this.selected_beat_key = null
        //}

        var cursor = this.opus_manager.cursor
        var position = cursor.get_position()

        for (view in this.cache.get_all_leafs(cursor.y, cursor.x, position)) {
            var pair = this.opus_manager.get_channel_index(cursor.y)
            var color = if (this.opus_manager.is_reflection(pair.first, pair.second, cursor.x)) {
                R.color.leaf_linked_selected
            } else {
                R.color.leaf_selected
            }
            val changeColour = ContextCompat.getColor(view.button.context, color)
            view.button.setBackgroundColor(changeColour)
        }

        this.cache.setCursor(cursor.y, cursor.x, position)
    }

    fun cellClickListener(y: Int, x: Int, position: List<Int>) {
        this.opus_manager.set_cursor_position(y, x, position)
        this.update_cursor_position()
        if (this.linking_beat == null) {
            this.setContextMenu(3)
        } else {
            this.setContextMenu(4)
        }
    }

    fun openFileBrowser() {
        val fileintent = Intent(Intent.ACTION_GET_CONTENT)
        fileintent.type = "gagt/sdf"
        startActivity(fileintent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 777) {
            val filePath = data?.data?.path
            this.opus_manager.load(filePath!!)
        }
    }
}
