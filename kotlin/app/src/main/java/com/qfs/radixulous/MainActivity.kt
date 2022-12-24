package com.qfs.radixulous

import android.content.Intent
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
import kotlinx.android.synthetic.main.contextmenu_linking.view.*
import kotlinx.android.synthetic.main.contextmenu_row.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.load_project.view.*
import kotlinx.android.synthetic.main.numberline_item.view.*
import kotlinx.android.synthetic.main.table_cell_label.view.*
import java.io.File
import java.lang.Integer.max
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager

enum class ContextMenu {
    Leaf,
    Line,
    Beat,
    Linking,
    None
}

class ViewCache {
    var view_cache: MutableList<Pair<LinearLayout, MutableList<Pair<View?, HashMap<List<Int>, View>>>>> = mutableListOf()
    var line_label_cache: MutableList<View> = mutableListOf()
    var column_label_cache: MutableList<View> = mutableListOf()
    var _cursor: Triple<Int, Int, List<Int>>? = null
    private var active_context_menu_view: View? = null

    fun get_all_leafs(y: Int, x: Int, position: List<Int>): List<View> {
        val output: MutableList<View> = mutableListOf()
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
        val beat_view = this.view_cache[y].second.removeAt(x).first
        (beat_view?.parent as ViewGroup).removeView(beat_view)
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
        // hsvColumnLabels.viewTreeObserver.addOnScrollChangedListener(OnScrollChangedListener {
       //     hsvTable.scrollX = hsvColumnLabels.scrollX
       // })
       // svLineLabels.viewTreeObserver.addOnScrollChangedListener(OnScrollChangedListener {
       //     svTable.scrollY = svLineLabels.scrollY
       // })

        val btnChannelCtrl: TextView = findViewById(R.id.btnChannelCtrl)
        btnChannelCtrl.setOnClickListener{
            this.showChannelPopup(it)
        }

        this.load("/data/data/com.qfs.radixulous/projects/test")
    }

    fun load(path: String) {
        this.opus_manager.load(path)
        this.tick()
        this.setContextMenu(ContextMenu.Leaf)

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
    }

    private fun takedownCurrent() {
        this.setContextMenu(ContextMenu.None)
        this.tlOpusLines.removeAllViews()
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

            val row = TextView(popupView.svProjectList.llProjectList.context)
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
        val parent = this.hsvColumnLabels.llColumnLabels
        val headerCellView = LayoutInflater.from(parent.context).inflate(
            R.layout.table_cell_label,
            parent,
            false
        ) as TextView
        val param = headerCellView.layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(0,0,0,0)
        headerCellView.layoutParams = param
        headerCellView.setBackgroundColor(
            ContextCompat.getColor(headerCellView.context, R.color.leaf_linked)
        )
        val x = parent.childCount
        headerCellView.text = "$x"
        headerCellView.setOnClickListener {
            val cursor = this.opus_manager.get_cursor()
            this.opus_manager.set_cursor_position(cursor.y, x, listOf())
            this.setContextMenu(ContextMenu.Beat)
            this.tick()
        }
        this.cache.addColumnLabel(headerCellView)
        parent.addView(headerCellView)
    }

    fun buildLineView(y: Int): TableRow {
        val clo = this.opus_manager.get_channel_index(y)
        val channel = clo.first
        val line_offset = clo.second

        val rowView = TableRow(this.tlOpusLines.context)
        rowView.setPadding(0,0,0,0)

        this.tlOpusLines.addView(rowView, y)
        this.cache.cacheLine(rowView, y)

        val rowLabel = LayoutInflater.from(this.svLineLabels.llLineLabels.context).inflate(
            R.layout.table_cell_label,
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
            var abs_y: Int = 0
            val label_column = rowLabel.parent!! as ViewGroup
            for (i in 0 until label_column.childCount) {
                if (label_column.getChildAt(i) == rowLabel) {
                    abs_y = i
                    break
                }
            }

            val cursor = that.opus_manager.get_cursor()
            this.opus_manager.set_cursor_position(abs_y, cursor.x, listOf())
            this.tick()
            this.setContextMenu(ContextMenu.Line)
        }

        val rl_params = rowLabel.layoutParams as ViewGroup.MarginLayoutParams
        rl_params.height = 130
        rl_params.setMargins(0,0,0,0)
        rowLabel.layoutParams = rl_params

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
            leafView.setBackgroundColor(
                if (this.opus_manager.is_reflection(channel_index.first, channel_index.second, x)) {
                    ContextCompat.getColor(leafView.context, R.color.leaf_linked)
                } else if (y % 2 == 0) {
                    ContextCompat.getColor(leafView.context, R.color.leaf_even)
                } else {
                    ContextCompat.getColor(leafView.context, R.color.leaf_odd)
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
                val key = this.cache.getTreeViewYXPosition(leafView)
                if (key != null) {
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
                    this.setContextMenu(ContextMenu.Leaf)

                    this.tick()
                }
            }

            leafView.setOnLongClickListener {
                val key = this.cache.getTreeViewYXPosition(leafView)
                if (key != null) {
                    val pair = this.opus_manager.get_channel_index(key.first)
                    this.linking_beat = BeatKey(pair.first, pair.second, key.second)
                    this.opus_manager.set_cursor_position(key.first, key.second, listOf())
                    this.setContextMenu(ContextMenu.Linking)
                    this.tick()
                }
                true
            }

            if (position.isEmpty()) {
                parent.addView(leafView, x)
            } else {
                parent.addView(leafView)
            }

            this.cache.cacheTree(leafView, y, x, position)
            return leafView
        } else {
            val cellLayout = LinearLayout(parent.context)
            this.cache.cacheTree(cellLayout, y, x, position)

           // if (tree.size > 1) {
           //     val open_brace = TextView(parent.context)
           //     open_brace.text = "["
           //     cellLayout.addView(open_brace)
           // }
            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(cellLayout as ViewGroup, y, x, new_position)
            }

           // if (tree.size > 1) {
           //     val close_brace = TextView(parent.context)
           //     close_brace.text = "]"
           //     cellLayout.addView(close_brace)
           // }

            if (position.isEmpty()) {
                parent.addView(cellLayout, x)
            } else {
                parent.addView(cellLayout)
            }

            return cellLayout
        }

    }

    fun setContextMenu(menu_index: ContextMenu) {
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
                    if (this.opus_manager.line_count() > 1) {
                        this.opus_manager.remove_line_at_cursor()
                    }
                    this.tick()
                }

                view.btnInsertLine.setOnClickListener {
                    this.opus_manager.new_line_at_cursor()
                    this.tick()
                }

                val beatkey = this.opus_manager.get_cursor().get_beatkey()
                if (beatkey.channel == 9) {
                    val instrument = this.opus_manager.get_percussion_instrument(beatkey.line_offset)
                    view.btnChooseInstrument.text = resources.getStringArray(R.array.midi_drums)[instrument - 35]
                } else {
                    val instrument = this.opus_manager.get_channel_instrument(beatkey.channel)
                    view.btnChooseInstrument.text = resources.getStringArray(R.array.midi_instruments)[instrument]
                }

                view.btnChooseInstrument.setOnClickListener {
                    val popupMenu = PopupMenu(window.decorView.rootView.context, it)
                    val cursor = this.opus_manager.get_cursor()
                    if (cursor.get_beatkey().channel == 9) {
                        //popupMenu.menuInflater.inflate(R.menu.percussion_instruments, popupMenu.getMenu())
                        val drums = resources.getStringArray(R.array.midi_drums)
                        drums.forEachIndexed { i, string ->
                            popupMenu.menu.add(0, i + 35, i, string)
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
            ContextMenu.Leaf -> {
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
                val cursor = this.opus_manager.get_cursor()
                if (this.opus_manager.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())) {
                    view.sRelative.isChecked = this.relative_mode
                    view.sRelative.setOnCheckedChangeListener { _, isChecked ->
                        this.relative_mode = isChecked
                        if (isChecked) {
                            this.opus_manager.convert_event_at_cursor_to_relative()
                        } else {
                            this.opus_manager.convert_event_at_cursor_to_absolute()
                        }
                        this.setContextMenu(ContextMenu.Leaf)
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
                        this.setContextMenu(ContextMenu.Leaf)
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

                    val that = this
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
                            val progress = seek.progress

                            val cursor = that.opus_manager.get_cursor()
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
                            val progress = seek.progress

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
                            val new_progress = if (event.note > 0) {
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

                    val that = this
                    view.llRelativePalette.sbRelativeValue?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seek: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) { }

                        override fun onStartTrackingTouch(seek: SeekBar) {}

                        override fun onStopTrackingTouch(seek: SeekBar) {
                            val progress = seek.progress
                            val radio_button: Int = that.llRelativePalette.rgRelOptions.checkedRadioButtonId

                            val cursor = opus_manager.get_cursor()
                            val beatkey = cursor.get_beatkey()

                            val new_value = when (radio_button) {
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


                            val event = OpusEvent(
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
                        val progress = view.llRelativePalette.sbRelativeValue.progress

                        val cursor = this.opus_manager.get_cursor()
                        val beatkey = cursor.get_beatkey()

                        val new_value = when (checkedId) {
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

                        val event = OpusEvent(
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
                    this.setContextMenu(ContextMenu.Leaf)
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
            ContextMenu.Linking -> {
                val view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_linking,
                    this.llContextMenu,
                    false
                )

                val cursor_key = this.opus_manager.get_cursor().get_beatkey()
                if (this.opus_manager.is_reflection(cursor_key.channel, cursor_key.line_offset, cursor_key.beat)) {
                    view.btnUnLink.setOnClickListener {
                        val cursor = this.opus_manager.get_cursor()
                        this.opus_manager.unlink_beat(cursor.get_beatkey())
                        cursor.settle()
                        this.linking_beat = null
                        this.setContextMenu(ContextMenu.Leaf)
                        this.tick()
                    }
                } else {
                    view.btnUnLink.visibility = View.GONE
                }

                view.btnCancelLink.setOnClickListener {
                    this.opus_manager.get_cursor().settle()
                    this.linking_beat = null
                    this.setContextMenu(ContextMenu.Leaf)
                    this.tick()
                }

                this.llContextMenu.addView(view)
                this.cache.setActiveContextMenu(view)
            }
            else -> { }
        }
    }

    fun rebuildBeatView(y: Int, x: Int) {
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

    @Deprecated("Deprecated in Java")
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
                    }
                }
                2 -> {
                    val y = this.opus_manager.get_y(channel, index)
                    this.buildLineView(y)
                }
            }
        }

        // Redraw labels
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

                val line_color = if (y % 2 == 0) {
                    R.color.leaf_even
                } else {
                    R.color.leaf_odd
                }

                for (x in 0 until this.opus_manager.opus_beat_count) {
                    for (leaf in this.cache.get_all_leafs(y, x, listOf())) {
                        if ((leaf as ViewGroup).childCount > 0) {
                            continue
                        }
                        val changeColour = ContextCompat.getColor(leaf.context, line_color)
                        leaf.setBackgroundColor(changeColour)
                    }
                }
                y += 1
            }
        }
    }

    fun tick_manage_beats() {
        val updated_beats: MutableSet<Int> = mutableSetOf()
        while (true) {
            val (index, operation) = this.opus_manager.fetch_flag_beat() ?: break
            when (operation) {
                1 -> {
                    this.newColumnLabel()
                    for (y in 0 until this.opus_manager.line_count()) {
                        val rowView = this.cache.getLine(y)?: continue
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
        val updated_beats: MutableSet<Int> = mutableSetOf()
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
        for (b in updated_beats) {
            var max_width = 0
            for (channel in 0 until this.opus_manager.channel_lines.size) {
                for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                    val tree = this.opus_manager.get_beat_tree(BeatKey(channel, line_offset, b))
                    val size = tree.size * tree.get_max_child_weight()
                    max_width = max(max_width, size)
                }
            }

            val label_view = this.cache.getColumnLabel(b)
            val param = label_view.layoutParams as ViewGroup.MarginLayoutParams
            param.width = (max_width * 100) - 10
            param.setMargins(5,0,5,0)
            label_view.layoutParams = param

            var y = 0
            for (channel in 0 until this.opus_manager.channel_lines.size) {
                for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                    val stack: MutableList<Pair<Float, List<Int>>> = mutableListOf(Pair(max_width.toFloat(), listOf()))
                    val key = BeatKey(channel, line_offset, b)

                    while (stack.isNotEmpty()) {
                        val (new_size, current_position) = stack.removeFirst()
                        val current_tree = this.opus_manager.get_tree(key, current_position)

                        val current_view = this.cache.getTreeView(y, b, current_position)
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
                            param.width = (new_size * 100.toFloat()).toInt() - 10
                            param.height = 120
                            param.setMargins(5,5,5,5)
                        }

                        current_view!!.layoutParams = param
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
                } else if (c.first % 2 == 0) {
                    R.color.leaf_even
                } else {
                    R.color.leaf_odd
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
