package com.qfs.pagan

class UIChangeBill {
    enum class BillableItem {
        AddRow,
        RemoveRow,
        ChangeRow,
        AddColumn,
        RemoveColumn,
        ChangeColumn,
        ChangeCell,
        ChangeInstrument,
        ChangeProjectName
    }

    private val bill = mutableListOf<BillableItem>()
    private var queued_cell_changes: MutableList<EditorTable.Coordinate> = mutableListOf()
    private var queued_column_changes: MutableList<Int> = mutableListOf()

    private var queued_column_adds = mutableListOf<Int>()
    private var queued_row_adds = mutableListOf<Int>()
    private var queued_column_removals = mutableListOf<Int>()
    private var queued_row_removals = mutableListOf<Pair<Int, Int>>()

    private var queued_channel_instrument_change: MutableList<Pair<Int, Pair<Int, Int>> = mutableListOf()
    private var queued_project_name_change = mutableListOf<String?>()

    fun queue_project_name_change(new_name: String?) {
        this.queued_project_name_change.add(new_name)
        this.bill.add(BillableItem.ChangeProjectName)
    }

    fun queue_cell_changes(cells: List<EditorTable.Coordinate>) {
        this.queued_cell_changes.addAll(cells)
        for (i in 0 until cells.size) {
            this.bill.add(BillableItem.ChangeCell)
        }
    }
    fun queue_cell_change(cell: EditorTable.Coordinate) {
        this.queued_cell_changes.add(cell)
        this.bill.add(BillableItem.ChangeCell)
    }
    fun queue_column_changes(columns: List<Int>) {
        this.queued_column_changes.addAll(columns)
        for (i in 0 until columns.size) {
            this.bill.add(BillableItem.ChangeColumn)
        }
    }
    fun queue_column_change(column: Int) {
        this.queued_column_changes.add(column)
        this.bill.add(BillableItem.ChangeColumn)
    }

    fun queue_new_row(y: Int) {
        this.queued_row_adds.add(y)
        this.bill.add(BillableItem.AddRow)
    }

    fun queue_instrument_change(channel: Int, instrument: Pair<Int, Int>) {
        this.queued_channel_instrument_change.add(Pair(channel, instrument))
        this.bill.add(BillableItem.ChangeInstrument)
    }

    fun queue_refresh_context_menu() {
        TODO()
    }
    fun queue_set_context_menu_line() {
        TODO()
    }
    fun queue_set_context_menu_leaf() {
        TODO()
    }
    fun queue_set_context_menu_linking() {
        TODO()
    }
    fun queue_set_context_menu_column() {
        TODO()
    }
    fun queue_set_context_menu_control_line() {
        TODO()
    }
    fun queue_set_context_menu_control_leaf() {
        TODO()
    }
    fun queue_clear_context_menu() {
        TODO()
    }

    fun queue_line_label_refresh(y: Int) {
        TODO()
    }

    fun queue_row_change(y: Int) {
        TODO()
    }

    fun queue_row_removal(y: Int, count: Int) {
        this.queued_row_removals.add(Pair(y, count))
    }




    /* Must be run on UI Thread */
    fun apply_changes(editor_table: EditorTable) {
        for (entry in this@UIChangeBill.bill) {
            when (entry) {
                BillableItem.AddRow -> {
                    // TODO
                }
                BillableItem.RemoveRow -> {
                    editor_table.remove_rows(
                        this@UiChangeBill.queued_row_removals.removeFirst(),
                        entry.count
                    )
                }
                BillableItem.ChangeRow -> {}
                BillableItem.AddColumn -> {}
                BillableItem.RemoveColumn -> {}
                BillableItem.ChangeColumn -> {}
                BillableItem.ChangeCell -> {}
                BillableItem.ChangeInstrument -> {
                    val (channel, instruments) = this.queued_channel_instrument_change.removeFirst()!!
                    main.update_channel_instruments(channel)
                    main.populate_active_percussion_names()
                    val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
                    if (channel_recycler.adapter != null) {
                        (channel_recycler.adapter as ChannelOptionAdapter).notifyItemChanged(channel)
                    }
                }
                BillableItem.ChangeProjectName -> {
                    main.update_title_text()
                }
            }
        }
    }
}
