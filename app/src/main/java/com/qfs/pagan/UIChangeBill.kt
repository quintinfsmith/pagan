package com.qfs.pagan

class UIChangeBill {
    enum class BillableItem {
        RowAdd,
        RowRemove,
        RowChange,
        ColumnAdd,
        ColumnRemove,
        ColumnChange,
        CellChange,
        ChannelChange,
        ChannelAdd,
        ChannelRemove,
        ProjectNameChange,
        ProjectNameUnset,
        ChangeInstrument,
    }

    val bill = mutableListOf<BillableItem>()
    private val _int_queue = mutableListOf<Int>()
    private val _string_queue = mutableListOf<String>()

    fun has_entries(): Boolean {
        return this.bill.isNotEmpty()
    }

    fun get_next_entry(): BillableItem? {
        return if (this.bill.isEmpty()) {
            null
        } else {
            this.bill.removeFirst()
        }
    }

    fun get_next_int(): Int {
        return this._int_queue.removeFirst()
    }
    fun get_next_string(): String {
        return this._string_queue.removeFirst()
    }

    fun queue_project_name_change(new_name: String?) {
        if (new_name == null) {
            this.bill.add(BillableItem.ProjectNameUnset)
        } else {
            this._string_queue.add(new_name)
            this.bill.add(BillableItem.ProjectNameChange)
        }
    }

    fun queue_cell_changes(cells: List<EditorTable.Coordinate>) {
        for (i in 0 until cells.size) {
            this._int_queue.add(cells[i].y)
            this._int_queue.add(cells[i].x)
            this.bill.add(BillableItem.CellChange)
        }
    }
    fun queue_cell_change(cell: EditorTable.Coordinate) {
        this._int_queue.add(cell.y)
        this._int_queue.add(cell.x)
        this.bill.add(BillableItem.CellChange)
    }
    fun queue_column_changes(columns: List<Int>) {
        this._int_queue.addAll(columns)
        for (i in 0 until columns.size) {
            this.bill.add(BillableItem.ColumnChange)
        }
    }
    fun queue_column_change(column: Int) {
        this._int_queue.add(column)
        this.bill.add(BillableItem.ColumnChange)
    }

    fun queue_new_row(y: Int) {
        this._int_queue.add(y)
        this.bill.add(BillableItem.RowAdd)
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
    fun queue_set_context_menu_leaf_percussion() {
        TODO()
    }
    fun queue_set_context_menu_line_control_leaf() {
        TODO()
    }
    fun queue_set_context_menu_line_control_leaf_b() {
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
    fun queue_clear_context_menu() {
        TODO()
    }

    fun queue_line_label_refresh(y: Int) {
        TODO()
    }

    fun queue_row_change(y: Int) {
        this._int_queue.add(y)
        this.bill.add(BillableItem.RowChange)
    }

    fun queue_row_removal(y: Int, count: Int) {
        this._int_queue.add(y)
        this._int_queue.add(count)
        this.bill.add(BillableItem.RowRemove)
    }

    fun queue_enable_delete_and_copy_buttons() {
        TODO()
    }

    fun queue_config_drawer_redraw_export_button() {
        //activity.setup_project_config_drawer_export_button()
        TODO()
    }

    fun queue_add_channel(channel: Int) {
        //this._activity?.update_channel_instruments(notify_index)
        //             val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        //             if (channel_recycler.adapter != null) {
        //                 val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
        //                 channel_adapter.add_channel()
        //             }
        this._int_queue.add(channel)
        this.bill.add(BillableItem.ChannelAdd)
    }
    fun queue_refresh_channel(channel: Int) {
        //val channel_option_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        //if (channel_option_recycler.adapter != null) {
        //    val adapter = channel_option_recycler.adapter!! as ChannelOptionAdapter
        //    adapter.notifyItemChanged(adapter.itemCount - 1)
        //}
        this._int_queue.add(channel)
        this.bill.add(BillableItem.ChannelChange)
    }
    fun queue_remove_channel(channel: Int) {
        //val channel_recycler = main.findViewById<ChannelOptionRecycler>(R.id.rvActiveChannels)
        //if (channel_recycler.adapter != null) {
        //    val channel_adapter = (channel_recycler.adapter as ChannelOptionAdapter)
        //        channel_adapter.remove_channel(channel)
        //}
        this._int_queue.add(channel)
        this.bill.add(BillableItem.ChannelRemove)
    }

    fun queue_add_column(column: Int) {

        // (this.column_label_recycler.adapter!! as ColumnLabelAdapter).add_column(index)
        // (this.get_column_recycler().adapter as ColumnRecyclerAdapter).add_column(index)
        this._int_queue.add(column)
        this.bill.add(BillableItem.ColumnAdd)
    }

    fun queue_remove_column(column: Int) {
        this._int_queue.add(column)
        this.bill.add(BillableItem.ColumnRemove)
    }
}
