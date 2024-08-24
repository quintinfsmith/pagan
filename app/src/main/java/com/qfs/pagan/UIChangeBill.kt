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
        ContextMenuRefresh,
        ContextMenuSetLine,
        ContextMenuSetLeaf,
        ContextMenuSetLeafPercussion,
        ContextMenuSetControlLeaf,
        ContextMenuSetControlLeafB,
        ContextMenuSetLinking,
        ContextMenuSetColumn,
        ContextMenuSetControlLine,
        ContextMenuClear,
        ConfigDrawerEnableCopyAndDelete,
        ConfigDrawerRefreshExportButton,
        PercussionButtonRefresh,
        LineLabelRefresh
    }

    val bill = mutableListOf<BillableItem>()
    private val _int_queue = mutableListOf<Int>()

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

    fun queue_project_name_change() {
        this.bill.add(BillableItem.ProjectNameChange)
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
        this.bill.add(BillableItem.ContextMenuRefresh)
    }
    fun queue_set_context_menu_line() {
        this.bill.add(BillableItem.ContextMenuSetLine)
    }
    fun queue_set_context_menu_leaf() {
        this.bill.add(BillableItem.ContextMenuSetLeaf)
    }
    fun queue_set_context_menu_leaf_percussion() {
        this.bill.add(BillableItem.ContextMenuSetLeafPercussion)
    }
    fun queue_set_context_menu_line_control_leaf() {
        this.bill.add(BillableItem.ContextMenuSetControlLeaf)
    }
    fun queue_set_context_menu_line_control_leaf_b() {
        this.bill.add(BillableItem.ContextMenuSetControlLeafB)
    }
    fun queue_set_context_menu_linking() {
        this.bill.add(BillableItem.ContextMenuSetLinking)
    }
    fun queue_set_context_menu_column() {
        this.bill.add(BillableItem.ContextMenuSetColumn)
    }
    fun queue_set_context_menu_control_line() {
        this.bill.add(BillableItem.ContextMenuSetControlLine)
    }
    fun queue_clear_context_menu() {
        this.bill.add(BillableItem.ContextMenuClear)
    }

    fun queue_line_label_refresh(y: Int) {
        this._int_queue.add(y)
        this.bill.add(BillableItem.LineLabelRefresh)
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
        this.bill.add(BillableItem.ConfigDrawerEnableCopyAndDelete)
    }

    fun queue_config_drawer_redraw_export_button() {
        this.bill.add(BillableItem.ConfigDrawerRefreshExportButton)
    }

    fun queue_add_channel(channel: Int) {
        this._int_queue.add(channel)
        this.bill.add(BillableItem.ChannelAdd)
    }
    fun queue_refresh_channel(channel: Int) {
        this._int_queue.add(channel)
        this.bill.add(BillableItem.ChannelChange)
    }
    fun queue_remove_channel(channel: Int) {
        this._int_queue.add(channel)
        this.bill.add(BillableItem.ChannelRemove)
    }

    fun queue_add_column(column: Int) {
        this._int_queue.add(column)
        this.bill.add(BillableItem.ColumnAdd)
    }

    fun queue_remove_column(column: Int) {
        this._int_queue.add(column)
        this.bill.add(BillableItem.ColumnRemove)
    }

    fun queue_refresh_choose_percussion_button(line_offset: Int) {
        this._int_queue.add(line_offset)
        this.bill.add(BillableItem.PercussionButtonRefresh)
    }
}
