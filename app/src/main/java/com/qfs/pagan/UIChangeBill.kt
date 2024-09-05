package com.qfs.pagan

class UIChangeBill {
    enum class BillableItem {
        RowAdd,
        RowRemove,
        RowChange,
        RowStateChange,
        ColumnAdd,
        ColumnRemove,
        ColumnChange,
        ColumnStateChange,
        CellChange,
        CellStateChange,
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
        LineLabelRefresh,
        ColumnLabelRefresh,
        FullRefresh
    }

    private val _bill = mutableListOf<BillableItem>()
    private var _full_refresh_flagged = false
    private val _int_queue = mutableListOf<Int>()
    private val queued_cells = Array<MutableSet<EditorTable.Coordinate>>(2) {
        mutableSetOf()
    }
    private val queued_columns = Array<MutableSet<Int>>(2) {
        mutableSetOf()
    }

    fun get_next_entry(): BillableItem? {
        return if (this._full_refresh_flagged) {
            this._full_refresh_flagged = false
            BillableItem.FullRefresh
        } else if (this._bill.isNotEmpty()) {
            this._bill.removeFirst()
        } else if (this.queued_columns[0].isNotEmpty()) {
            val columns = (this.queued_columns[0] - this.queued_columns[1]).toList()

            for (x in columns) {
                this._bill.add(BillableItem.ColumnStateChange)
                this._int_queue.add(x)
            }

            this.queued_columns[0].clear()
            this.get_next_entry()

        } else if (this.queued_columns[1].isNotEmpty()) {
            val columns = this.queued_columns[1].toList()

            for (x in columns) {
                this._bill.add(BillableItem.ColumnChange)
                this._int_queue.add(x)
            }

            this.queued_columns[1].clear()
            this.get_next_entry()
        } else if (this.queued_cells[0].isNotEmpty()) {
            val cells = (this.queued_cells[0] - this.queued_cells[1]).toList()

            this._int_queue.add(cells.size)
            for (cell in cells.toList()) {
                this._int_queue.add(cell.y)
                this._int_queue.add(cell.x)
            }
            this._bill.add(BillableItem.CellStateChange)

            this.queued_cells[0].clear()

            this.get_next_entry()

        } else if (this.queued_cells[1].isNotEmpty()) {
            val cells = this.queued_cells[1].toList()

            this._int_queue.add(cells.size)
            for (cell in cells.toList()) {
                this._int_queue.add(cell.y)
                this._int_queue.add(cell.x)
            }
            this._bill.add(BillableItem.CellChange)

            this.queued_cells[1].clear()

            this.get_next_entry()
        } else {
            null
        }
    }

    fun get_next_int(): Int {
        return this._int_queue.removeFirst()
    }

    fun queue_full_refresh() {
        this._full_refresh_flagged = true
        this.clear()
    }

    fun clear() {
        this._bill.clear()
        this._int_queue.clear()
        this.queued_cells[0].clear()
        this.queued_cells[1].clear()
        this.queued_columns[0].clear()
        this.queued_columns[1].clear()
    }

    fun queue_project_name_change() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ProjectNameChange)
    }

    fun queue_cell_changes(cells: List<EditorTable.Coordinate>, state_only: Boolean = false) {
        if (this._full_refresh_flagged) {
            return
        }

        val i = if (state_only) {
            0
        } else {
            1
        }

        this.queued_cells[i].addAll(cells)
    }

    fun queue_cell_change(cell: EditorTable.Coordinate, state_only: Boolean = false) {
        if (this._full_refresh_flagged) {
            return
        }

        val i = if (state_only) {
            0
        } else {
            1
        }

        this.queued_cells[i].add(cell)

    }

    fun queue_column_changes(columns: List<Int>, state_only: Boolean = false) {
        if (this._full_refresh_flagged) {
            return
        }

        val i = if (state_only) {
            0
        } else {
            1
        }


        this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
            columns.contains(coord.x)
        }.toSet()

        this.queued_columns[i].addAll(columns)
    }

    fun queue_column_change(column: Int, state_only: Boolean = false) {
        if (this._full_refresh_flagged) {
            return
        }

        val i = if (state_only) {
            0
        } else {
            1
        }

        this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
            coord.x == column
        }.toSet()

        this.queued_columns[i].add(column)
    }

    fun queue_new_row(y: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        for (i in 0 until this.queued_cells.size) {
            for (coord in this.queued_cells[i]) {
                if (coord.y >= y) {
                    coord.y += 1
                }
            }
        }

        this._int_queue.add(y)
        this._bill.add(BillableItem.RowAdd)
    }

    fun queue_refresh_context_menu() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuRefresh)
    }
    fun queue_set_context_menu_line() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetLine)
    }
    fun queue_set_context_menu_leaf() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetLeaf)
    }
    fun queue_set_context_menu_leaf_percussion() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetLeafPercussion)
    }
    fun queue_set_context_menu_line_control_leaf() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetControlLeaf)
    }
    fun queue_set_context_menu_line_control_leaf_b() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetControlLeafB)
    }
    fun queue_set_context_menu_linking() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetLinking)
    }
    fun queue_set_context_menu_column() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetColumn)
    }
    fun queue_set_context_menu_control_line() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuSetControlLine)
    }
    fun queue_clear_context_menu() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ContextMenuClear)
    }

    fun queue_column_label_refresh(x: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        this._int_queue.add(x)
        this._bill.add(BillableItem.ColumnLabelRefresh)
    }

    fun queue_line_label_refresh(y: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        this._int_queue.add(y)
        this._bill.add(BillableItem.LineLabelRefresh)
    }

    fun queue_row_change(y: Int, state_only: Boolean = false) {
        if (this._full_refresh_flagged) {
            return
        }

        val i = if (state_only) {
            0
        } else {
            1
        }

        this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
            coord.y == y
        }.toSet()

        this._int_queue.add(y)
        this._bill.add(BillableItem.RowChange)
    }

    fun queue_row_removal(y: Int, count: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        val check_range = y until y + count
        for (i in 0 until this.queued_cells.size) {
            this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
                check_range.contains(coord.y)
            }.toSet()


            for (coord in this.queued_cells[i]) {
                if (coord.y >= y + count) {
                    coord.y -= count
                }
            }
        }

        this._int_queue.add(y)
        this._int_queue.add(count)
        this._bill.add(BillableItem.RowRemove)
    }

    fun queue_enable_delete_and_copy_buttons() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ConfigDrawerEnableCopyAndDelete)
    }

    fun queue_config_drawer_redraw_export_button() {
        if (this._full_refresh_flagged) {
            return
        }

        this._bill.add(BillableItem.ConfigDrawerRefreshExportButton)
    }

    fun queue_add_channel(channel: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        this._int_queue.add(channel)
        this._bill.add(BillableItem.ChannelAdd)
    }

    fun queue_refresh_channel(channel: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        this._int_queue.add(channel)
        this._bill.add(BillableItem.ChannelChange)
    }

    fun queue_remove_channel(channel: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        this._int_queue.add(channel)
        this._bill.add(BillableItem.ChannelRemove)
    }

    fun queue_add_column(column: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        for (i in 0 until this.queued_cells.size) {
            for (coord in this.queued_cells[i]) {
                if (coord.x >= column) {
                    coord.x += 1
                }
            }

            val old_columns = this.queued_columns[i].toSet()
            this.queued_columns[i].clear()
            for (x in old_columns) {
                if (x < column) {
                    this.queued_columns[i].add(x)
                } else {
                    this.queued_columns[i].add(x + 1)
                }
            }
        }

        this._int_queue.add(column)
        this._bill.add(BillableItem.ColumnAdd)
    }

    fun queue_remove_column(column: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        for (i in 0 until this.queued_cells.size) {
            val queued_cell_set = this.queued_cells[i]
            queued_cell_set -= queued_cell_set.filter { coord: EditorTable.Coordinate ->
                coord.x == column
            }.toSet()

            for (coord in queued_cell_set) {
                if (coord.x > column) {
                    coord.x -= 1
                }
            }

            val old_columns = this.queued_columns[i].toSet()
            this.queued_columns[i].clear()

            for (x in old_columns) {
                if (x == column) {
                    continue
                } else if (x < column) {
                    this.queued_columns[i].add(x)
                } else {
                    this.queued_columns[i].add(x - 1)
                }
            }
        }

        this._int_queue.add(column)
        this._bill.add(BillableItem.ColumnRemove)
    }

    fun queue_refresh_choose_percussion_button(line_offset: Int) {
        if (this._full_refresh_flagged) {
            return
        }

        this._int_queue.add(line_offset)
        this._bill.add(BillableItem.PercussionButtonRefresh)
    }
}
