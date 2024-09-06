package com.qfs.pagan

import kotlin.math.max

class UIChangeBill {
    class UILock {
        companion object {
            const val FULL = 2
            const val PARTIAL = 1
            const val NONE = 0
        }

        var flag = NONE
        var level = 0
        fun lock_partial() {
            this.flag = max(this.flag, UILock.PARTIAL)
            this.level += 1
        }

        fun lock_full() {
            this.flag = max(this.flag, UILock.FULL)
            this.level += 1
        }

        fun unlock() {
            this.level -= 1
            if (this.level == 0) {
                this.flag = UILock.NONE
            }
        }

        fun is_locked(): Boolean {
            return this.level > 0
        }

        fun is_full_locked(): Boolean {
            return this.flag == UILock.FULL
        }
    }

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

    class Node {
        val bill = mutableListOf<BillableItem>()
        val int_queue = mutableListOf<Int>()
        val sub_nodes = mutableListOf<Node>()
        fun new_node() {
            this.sub_nodes.add(Node())
        }
        fun remove_node(path: List<Int>) {
            val next = path[0]
            if (path.isEmpty()) {
                this.sub_nodes.removeAt(next)
            } else {
                this.sub_nodes[next].remove_node(path.subList(1, path.size))
            }
        }

        fun last(): Node {
            var working_node = this
            while (working_node.sub_nodes.isNotEmpty()) {
                working_node = working_node.sub_nodes.last()
            }
            return working_node
        }
        fun remove_last() {
            val path = mutableListOf<Int>()
            var working_node = this
            while (working_node.sub_nodes.isNotEmpty()) {
                path.add(working_node.sub_nodes.size - 1)
                working_node = working_node.sub_nodes.last()
            }
            if (path.isNotEmpty()) {
                this.remove_node(path)
            }
        }
    }


    private val ui_lock = UILock()
    private val _tree: Node = Node()

    fun consolidate() {
        val _consolidated_bill = mutableListOf<BillableItem>()
        val _consolidated_int_queue = mutableListOf<Int>()
        val queued_cells = Array<MutableSet<EditorTable.Coordinate>>(2) {
            mutableSetOf()
        }
        val queued_columns = Array<MutableSet<Int>>(2) {
            mutableSetOf()
        }

        var stack = mutableListOf<Node>(this._tree)
        while (stack.isNotEmpty()) {
            var node = stack.removeFirst()
            for (bill_item in node.bill) {
                when (bill_item) {
                    BillableItem.RowAdd -> { }
                    BillableItem.RowRemove -> { }
                    BillableItem.RowChange -> { }
                    BillableItem.RowStateChange -> { }
                    BillableItem.ColumnAdd -> { }
                    BillableItem.ColumnRemove -> { }
                    BillableItem.ColumnChange -> { }
                    BillableItem.ColumnStateChange -> { }
                    BillableItem.CellChange -> { }
                    BillableItem.CellStateChange -> { }
                    BillableItem.ChannelChange -> { }
                    BillableItem.ChannelAdd -> { }
                    BillableItem.ChannelRemove -> { }

                    BillableItem.ProjectNameChange,
                    BillableItem.ContextMenuRefresh,
                    BillableItem.ContextMenuSetLine,
                    BillableItem.ContextMenuSetLeaf,
                    BillableItem.ContextMenuSetLeafPercussion,
                    BillableItem.ContextMenuSetControlLeaf,
                    BillableItem.ContextMenuSetControlLeafB,
                    BillableItem.ContextMenuSetLinking,
                    BillableItem.ContextMenuSetColumn,
                    BillableItem.ContextMenuSetControlLine,
                    BillableItem.ContextMenuClear,
                    BillableItem.ConfigDrawerEnableCopyAndDelete,
                    BillableItem.ConfigDrawerRefreshExportButton -> {
                        this._tree.bill.add(bill_item)
                    }

                    BillableItem.PercussionButtonRefresh -> { }

                    BillableItem.LineLabelRefresh -> { }
                    BillableItem.ColumnLabelRefresh -> { }
                    BillableItem.FullRefresh -> { }
                }
            }

            stack.addAll(node.sub_nodes)
        }
    }

    fun get_next_entry(): BillableItem? {
        return if (this.is_full_locked()) {
            this.is_full_locked() = false
            BillableItem.FullRefresh
        } else if (this._bill.isNotEmpty()) {
            this._bill.removeFirst()
        } else if (this.queued_columns[0].isNotEmpty()) {
            val columns = (this.queued_columns[0] - this.queued_columns[1]).toList()

            for (x in columns) {
                this._working_bill.last().add(BillableItem.ColumnStateChange)
                this._working_int_queue.last().add(x)
            }

            this.queued_columns[0].clear()
            this.get_next_entry()

        } else if (this.queued_columns[1].isNotEmpty()) {
            val columns = this.queued_columns[1].toList()

            for (x in columns) {
                this._working_bill.last().add(BillableItem.ColumnChange)
                this._working_int_queue.last().add(x)
            }

            this.queued_columns[1].clear()
            this.get_next_entry()
        } else if (this.queued_cells[0].isNotEmpty()) {
            val cells = (this.queued_cells[0] - this.queued_cells[1]).toList()

            this._working_int_queue.last().add(cells.size)
            for (cell in cells.toList()) {
                this._working_int_queue.last().add(cell.y)
                this._working_int_queue.last().add(cell.x)
            }
            this._working_bill.last().add(BillableItem.CellStateChange)

            this.queued_cells[0].clear()

            this.get_next_entry()

        } else if (this.queued_cells[1].isNotEmpty()) {
            val cells = this.queued_cells[1].toList()

            this._working_int_queue.last().add(cells.size)
            for (cell in cells.toList()) {
                this._working_int_queue.last().add(cell.y)
                this._working_int_queue.last().add(cell.x)
            }
            this._working_bill.last().add(BillableItem.CellChange)

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
        TODO()
        this.clear()
    }


    fun clear() {
        this._consolidated_bill.clear()
        this._consolidated_int_queue.clear()
        this._working_bill.clear()
        this._working_int_queue.clear()
        this.queued_cells[0].clear()
        this.queued_cells[1].clear()
        this.queued_columns[0].clear()
        this.queued_columns[1].clear()
    }

    //fun queue_cell_changes(cells: List<EditorTable.Coordinate>, state_only: Boolean = false) {
    //    if (this.is_full_locked()) {
    //        return
    //    }

    //    val i = if (state_only) {
    //        0
    //    } else {
    //        1
    //    }

    //    this.queued_cells[i].addAll(cells)
    //}

    //fun queue_cell_change(cell: EditorTable.Coordinate, state_only: Boolean = false) {
    //    if (this.is_full_locked()) {
    //        return
    //    }

    //    val i = if (state_only) {
    //        0
    //    } else {
    //        1
    //    }

    //    this.queued_cells[i].add(cell)
    //}
    //fun queue_column_changes(columns: List<Int>, state_only: Boolean = false) {
    //    if (this.is_full_locked()) {
    //        return
    //    }

    //    val i = if (state_only) {
    //        0
    //    } else {
    //        1
    //    }


    //    this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
    //        columns.contains(coord.x)
    //    }.toSet()

    //    this.queued_columns[i].addAll(columns)
    //}
    //fun queue_column_change(column: Int, state_only: Boolean = false) {
    //    if (this.is_full_locked()) {
    //        return
    //    }

    //    val i = if (state_only) {
    //        0
    //    } else {
    //        1
    //    }

    //    this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
    //        coord.x == column
    //    }.toSet()

    //    this.queued_columns[i].add(column)
    //}
    // fun queue_new_row(y: Int) {
    //     if (this.is_full_locked()) {
    //         return
    //     }

    //     for (i in 0 until this.queued_cells.size) {
    //         for (coord in this.queued_cells[i]) {
    //             if (coord.y >= y) {
    //                 coord.y += 1
    //             }
    //         }
    //     }

    //     this._working_int_queue.last().add(y)
    //     this._working_bill.last().add(BillableItem.RowAdd)
    // }




    fun queue_cell_changes(cells: List<EditorTable.Coordinate>, state_only: Boolean = false) {
        if (this.is_full_locked()) {
            return
        }

        val working_tree = this._tree.last()

        working_tree.bill.add(
            if (state_only) {
                BillableItem.CellStateChange
            } else {
                BillableItem.CellChange
            }
        )

        working_tree.int_queue.add(cells.size)
        for (cell in cells) {
            working_tree.int_queue.add(cell.y)
            working_tree.int_queue.add(cell.x)
        }
    }

    fun queue_cell_change(cell: EditorTable.Coordinate, state_only: Boolean = false) {
        if (this.is_full_locked()) {
            return
        }
        if (state_only) {
            this._working_bill.last().add(BillableItem.CellStateChange)
        } else {
            this._working_bill.last().add(BillableItem.CellChange)
        }

        this._working_int_queue.last().add(cell.y)
        this._working_int_queue.last().add(cell.x)
    }

    fun queue_column_changes(columns: List<Int>, state_only: Boolean = false) {
        if (this.is_full_locked()) {
            return
        }

        val bill_item = if (state_only) {
            BillableItem.ColumnStateChange
        } else {
            BillableItem.ColumnChange
        }

        for (column in columns) {
            this._working_bill.last().add(bill_item)
            this._working_int_queue.last().add(column)
        }
    }

    fun queue_column_change(column: Int, state_only: Boolean = false) {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(
            if (state_only) {
                BillableItem.ColumnStateChange
            } else {
                BillableItem.ColumnChange
            }
        )
        this._working_int_queue.last().add(column)
    }


    fun queue_new_row(y: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(y)
        this._working_bill.last().add(BillableItem.RowAdd)
    }


    fun queue_refresh_context_menu() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuRefresh)
    }
    fun queue_set_context_menu_line() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetLine)
    }
    fun queue_set_context_menu_leaf() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetLeaf)
    }
    fun queue_set_context_menu_leaf_percussion() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetLeafPercussion)
    }
    fun queue_set_context_menu_line_control_leaf() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetControlLeaf)
    }
    fun queue_set_context_menu_line_control_leaf_b() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetControlLeafB)
    }
    fun queue_set_context_menu_linking() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetLinking)
    }
    fun queue_set_context_menu_column() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetColumn)
    }
    fun queue_set_context_menu_control_line() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuSetControlLine)
    }
    fun queue_clear_context_menu() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ContextMenuClear)
    }
    fun queue_enable_delete_and_copy_buttons() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ConfigDrawerEnableCopyAndDelete)
    }

    fun queue_config_drawer_redraw_export_button() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ConfigDrawerRefreshExportButton)
    }
    fun queue_project_name_change() {
        if (this.is_full_locked()) {
            return
        }

        this._working_bill.last().add(BillableItem.ProjectNameChange)
    }

    fun queue_column_label_refresh(x: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(x)
        this._working_bill.last().add(BillableItem.ColumnLabelRefresh)
    }

    fun queue_line_label_refresh(y: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(y)
        this._working_bill.last().add(BillableItem.LineLabelRefresh)
    }

    fun queue_row_change(y: Int, state_only: Boolean = false) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(y)
        this._working_bill.last().add(
            if (state_only) {
                BillableItem.RowStateChange
            } else {
                BillableItem.RowChange
            }
        )
    }

    // fun queue_row_change(y: Int, state_only: Boolean = false) {
    //     if (this.is_full_locked()) {
    //         return
    //     }

    //     val i = if (state_only) {
    //         0
    //     } else {
    //         1
    //     }

    //     this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
    //         coord.y == y
    //     }.toSet()

    //     this._working_int_queue.last().add(y)
    //     this._working_bill.last().add(BillableItem.RowChange)
    // }


    fun queue_row_removal(y: Int, count: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(y)
        this._working_int_queue.last().add(count)
        this._working_bill.last().add(BillableItem.RowRemove)
    }

    // fun queue_row_removal(y: Int, count: Int) {
    //     if (this.is_full_locked()) {
    //         return
    //     }

    //     val check_range = y until y + count
    //     for (i in 0 until this.queued_cells.size) {
    //         this.queued_cells[i] -= this.queued_cells[i].filter { coord: EditorTable.Coordinate ->
    //             check_range.contains(coord.y)
    //         }.toSet()


    //         for (coord in this.queued_cells[i]) {
    //             if (coord.y >= y + count) {
    //                 coord.y -= count
    //             }
    //         }
    //     }

    //     this._working_int_queue.last().add(y)
    //     this._working_int_queue.last().add(count)
    //     this._working_bill.last().add(BillableItem.RowRemove)
    // }


    fun queue_add_channel(channel: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(channel)
        this._working_bill.last().add(BillableItem.ChannelAdd)
    }

    fun queue_refresh_channel(channel: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(channel)
        this._working_bill.last().add(BillableItem.ChannelChange)
    }

    fun queue_remove_channel(channel: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(channel)
        this._working_bill.last().add(BillableItem.ChannelRemove)
    }

    fun queue_add_column(column: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(column)
        this._working_bill.last().add(BillableItem.ColumnAdd)
    }

    // fun queue_add_column(column: Int) {
    //     if (this.is_full_locked()) {
    //         return
    //     }

    //     for (i in 0 until this.queued_cells.size) {
    //         for (coord in this.queued_cells[i]) {
    //             if (coord.x >= column) {
    //                 coord.x += 1
    //             }
    //         }

    //         val old_columns = this.queued_columns[i].toSet()
    //         this.queued_columns[i].clear()
    //         for (x in old_columns) {
    //             if (x < column) {
    //                 this.queued_columns[i].add(x)
    //             } else {
    //                 this.queued_columns[i].add(x + 1)
    //             }
    //         }
    //     }

    //     this._working_int_queue.last().add(column)
    //     this._working_bill.last().add(BillableItem.ColumnAdd)
    // }

    fun queue_remove_column(column: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(column)
        this._working_bill.last().add(BillableItem.ColumnRemove)
    }

    // fun queue_remove_column(column: Int) {
    //     if (this.is_full_locked()) {
    //         return
    //     }

    //     for (i in 0 until this.queued_cells.size) {
    //         val queued_cell_set = this.queued_cells[i]
    //         queued_cell_set -= queued_cell_set.filter { coord: EditorTable.Coordinate ->
    //             coord.x == column
    //         }.toSet()

    //         for (coord in queued_cell_set) {
    //             if (coord.x > column) {
    //                 coord.x -= 1
    //             }
    //         }

    //         val old_columns = this.queued_columns[i].toSet()
    //         this.queued_columns[i].clear()

    //         for (x in old_columns) {
    //             if (x == column) {
    //                 continue
    //             } else if (x < column) {
    //                 this.queued_columns[i].add(x)
    //             } else {
    //                 this.queued_columns[i].add(x - 1)
    //             }
    //         }
    //     }

    //     this._working_int_queue.last().add(column)
    //     this._working_bill.last().add(BillableItem.ColumnRemove)
    // }

    fun queue_refresh_choose_percussion_button(line_offset: Int) {
        if (this.is_full_locked()) {
            return
        }

        this._working_int_queue.last().add(line_offset)
        this._working_bill.last().add(BillableItem.PercussionButtonRefresh)
    }

    fun lock_full() {
        this.ui_lock.lock_full()
        this._tree.new_node()
    }

    fun lock_partial() {
        this.ui_lock.lock_partial()
        this._tree.new_node()
    }

    fun is_locked(): Boolean {
        return this.ui_lock.is_locked()
    }

    fun is_full_locked(): Boolean {
        return this.ui_lock.is_full_locked()
    }

    fun cancel_most_recent() {
        this._tree.remove_last()
    }

}
