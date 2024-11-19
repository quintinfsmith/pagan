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
        ContextMenuSetChannel,
        ContextMenuSetLeaf,
        ContextMenuSetLeafPercussion,
        ContextMenuSetControlLeaf,
        ContextMenuSetControlLeafB,
        ContextMenuSetRange,
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

        fun get(path: List<Int>): Node {
            return if (path.isEmpty()) {
                this
            } else if (path == listOf(0)) {
                this.sub_nodes[path[0]]
            } else {
                this.sub_nodes[path[0]].get(path.subList(1, path.size))
            }
        }


        fun remove_node(path: List<Int>) {
            val next = path[0]
            if (path.size == 1) {
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

        fun clear() {
            this.sub_nodes.clear()
            this.bill.clear()
            this.int_queue.clear()
        }
    }

    private val ui_lock = UILock()
    private val _tree: Node = Node()
    private val working_path = mutableListOf<Int>()
    
    fun consolidate() {
        val queued_cells = Array<MutableSet<EditorTable.Coordinate>>(2) {
            mutableSetOf()
        }
        val queued_columns = Array<MutableSet<Int>>(2) {
            mutableSetOf()
        }
        val queued_line_labels = mutableSetOf<Int>()
        val queued_column_labels = mutableSetOf<Int>()

        var stack = mutableListOf<Node>(this._tree)
        while (stack.isNotEmpty()) {
            var node = stack.removeFirst()
            for (bill_item in node.bill) {
                // The Specified BillableItems will be manually added to the end of the queue after some calculations
                // The rest can be handled FIFO
                when (bill_item) {
                    BillableItem.FullRefresh,
                    BillableItem.CellChange,
                    BillableItem.CellStateChange,
                    BillableItem.ColumnChange,
                    BillableItem.ColumnStateChange,
                    BillableItem.LineLabelRefresh,
                    BillableItem.ColumnLabelRefresh -> { }
                    else -> {
                        this._tree.bill.add(bill_item)
                    }
                }

                when (bill_item) {
                    BillableItem.FullRefresh -> {
                        this._tree.clear()
                        this._tree.bill.add(bill_item)
                        return
                    }

                    BillableItem.RowAdd -> {
                        val y = node.int_queue.removeFirst()
                        for (i in 0 until queued_cells.size) {
                            for (coord in queued_cells[i]) {
                                if (coord.y >= y) {
                                    coord.y += 1
                                }
                            }
                        }

                        val new_queue_line_labels = mutableSetOf<Int>()
                        for (i in queued_line_labels) {
                            if (i > y) {
                                new_queue_line_labels.add(i + 1)
                            } else {
                                new_queue_line_labels.add(i)
                            }
                        }

                        queued_line_labels.clear()
                        queued_line_labels += new_queue_line_labels

                        this._tree.int_queue.add(y)
                    }

                    BillableItem.RowRemove -> {
                        val y = node.int_queue.removeFirst()
                        val count = node.int_queue.removeFirst()

                        val check_range = y until y + count
                        for (i in 0 until queued_cells.size) {
                            queued_cells[i] -= queued_cells[i].filter { coord: EditorTable.Coordinate ->
                                check_range.contains(coord.y)
                            }.toSet()


                            for (coord in queued_cells[i]) {
                                if (coord.y >= y + count) {
                                    coord.y -= count
                                }
                            }
                        }

                        val new_queue_line_labels = mutableSetOf<Int>()
                        for (i in queued_line_labels) {
                            if (i >= y + count) {
                                new_queue_line_labels.add(i - count)
                            } else if (i < y) {
                                new_queue_line_labels.add(i)
                            }
                        }

                        queued_line_labels.clear()
                        queued_line_labels += new_queue_line_labels

                        this._tree.int_queue.add(y)
                        this._tree.int_queue.add(count)
                    }

                    BillableItem.ColumnAdd -> {
                        val column = node.int_queue.removeFirst()

                        for (i in 0 until queued_cells.size) {
                            for (coord in queued_cells[i]) {
                                if (coord.x >= column) {
                                    coord.x += 1
                                }
                            }

                            val old_columns = queued_columns[i].toSet()
                            queued_columns[i].clear()
                            for (x in old_columns) {
                                if (x < column) {
                                    queued_columns[i].add(x)
                                } else {
                                    queued_columns[i].add(x + 1)
                                }
                            }
                        }

                        val new_queue_column_labels = mutableSetOf<Int>()
                        for (i in queued_column_labels) {
                            if (i > column) {
                                new_queue_column_labels.add(i + 1)
                            } else {
                                new_queue_column_labels.add(i)
                            }
                        }

                        queued_column_labels.clear()
                        queued_column_labels += new_queue_column_labels

                        this._tree.int_queue.add(column)
                    }

                    BillableItem.ColumnRemove -> {
                        val column = node.int_queue.removeFirst()
                        for (i in 0 until queued_cells.size) {
                            val queued_cell_set = queued_cells[i]
                            queued_cell_set -= queued_cell_set.filter { coord: EditorTable.Coordinate ->
                                coord.x == column
                            }.toSet()

                            for (coord in queued_cell_set) {
                                if (coord.x > column) {
                                    coord.x -= 1
                                }
                            }

                            val old_columns = queued_columns[i].toSet()
                            queued_columns[i].clear()

                            for (x in old_columns) {
                                if (x == column) {
                                    continue
                                } else if (x < column) {
                                    queued_columns[i].add(x)
                                } else {
                                    queued_columns[i].add(x - 1)
                                }
                            }
                        }

                        val new_queue_column_labels = mutableSetOf<Int>()
                        for (i in queued_column_labels) {
                            if (i > column) {
                                new_queue_column_labels.add(i + 1)
                            } else if (i < column) {
                                new_queue_column_labels.add(i)
                            }
                        }

                        queued_column_labels.clear()
                        queued_column_labels += new_queue_column_labels

                        this._tree.int_queue.add(column)
                    }

                    BillableItem.ChannelChange,
                    BillableItem.ChannelAdd,
                    BillableItem.ChannelRemove -> {
                        this._tree.int_queue.add(node.int_queue.removeFirst())
                    }

                    BillableItem.LineLabelRefresh -> {
                        queued_line_labels.add(node.int_queue.removeFirst())
                    }
                    BillableItem.ColumnLabelRefresh -> {
                        queued_column_labels.add(node.int_queue.removeFirst())
                    }

                    BillableItem.PercussionButtonRefresh -> {
                        this._tree.int_queue.add(node.int_queue.removeFirst())
                    }

                    BillableItem.RowChange,
                    BillableItem.RowStateChange -> {
                        val i = if (bill_item == BillableItem.RowChange) {
                            1
                        } else {
                            0
                        }

                        val y = node.int_queue.removeFirst()

                        queued_cells[i] -= queued_cells[i].filter { coord: EditorTable.Coordinate ->
                            coord.y == y
                        }.toSet()

                        this._tree.int_queue.add(y)
                    }

                    BillableItem.ColumnChange,
                    BillableItem.ColumnStateChange -> {
                        val count = node.int_queue.removeFirst()
                        val columns = Array<Int>(count) {
                            node.int_queue.removeFirst()
                        }

                        val i = if (bill_item == BillableItem.ColumnChange) {
                            1
                        } else {
                            0
                        }

                        queued_cells[i] -= queued_cells[i].filter { coord: EditorTable.Coordinate ->
                            columns.contains(coord.x)
                        }.toSet()

                        queued_columns[i].addAll(columns)
                    }

                    BillableItem.CellChange,
                    BillableItem.CellStateChange -> {
                        val count = node.int_queue.removeFirst()
                        val i = if (bill_item == BillableItem.CellChange) {
                            1
                        } else {
                            0
                        }

                        for (j in 0 until count) {
                            queued_cells[i].add(
                                EditorTable.Coordinate(
                                    node.int_queue.removeFirst(),
                                    node.int_queue.removeFirst()
                                )
                            )
                        }
                    }

                    BillableItem.ContextMenuSetChannel,
                    BillableItem.ProjectNameChange,
                    BillableItem.ContextMenuRefresh,
                    BillableItem.ContextMenuSetLine,
                    BillableItem.ContextMenuSetLeaf,
                    BillableItem.ContextMenuSetLeafPercussion,
                    BillableItem.ContextMenuSetControlLeaf,
                    BillableItem.ContextMenuSetControlLeafB,
                    BillableItem.ContextMenuSetRange,
                    BillableItem.ContextMenuSetColumn,
                    BillableItem.ContextMenuSetControlLine,
                    BillableItem.ContextMenuClear,
                    BillableItem.ConfigDrawerEnableCopyAndDelete,
                    BillableItem.ConfigDrawerRefreshExportButton -> { }

                }
            }

            stack.addAll(0, node.sub_nodes)
        }

        if (queued_columns[0].isNotEmpty()) {
            val columns = (queued_columns[0] - queued_columns[1]).toList()

            for (x in columns) {
                this._tree.bill.add(BillableItem.ColumnStateChange)
                this._tree.int_queue.add(x)
            }
        }

        if (queued_columns[1].isNotEmpty()) {
            val columns = queued_columns[1].toList()

            for (x in columns) {
                this._tree.bill.add(BillableItem.ColumnChange)
                this._tree.int_queue.add(x)
            }

        } 

        if (queued_cells[0].isNotEmpty()) {
            val cells = (queued_cells[0] - queued_cells[1]).toList()

            this._tree.int_queue.add(cells.size)
            for (cell in cells.toList()) {
                this._tree.int_queue.add(cell.y)
                this._tree.int_queue.add(cell.x)
            }
            this._tree.bill.add(BillableItem.CellStateChange)
        } 

        if (queued_cells[1].isNotEmpty()) {
            val cells = queued_cells[1].toList()

            this._tree.int_queue.add(cells.size)
            for (cell in cells.toList()) {
                this._tree.int_queue.add(cell.y)
                this._tree.int_queue.add(cell.x)
            }
            this._tree.bill.add(BillableItem.CellChange)
        } 

        for (y in queued_line_labels) {
            this._tree.bill.add(BillableItem.LineLabelRefresh)
            this._tree.int_queue.add(y)
        }

        for (x in queued_column_labels) {
            this._tree.bill.add(BillableItem.ColumnLabelRefresh)
            this._tree.int_queue.add(x)
        }
    }

    fun get_next_entry(): BillableItem? {
        return if (this._tree.bill.isNotEmpty()) {
            this._tree.bill.removeFirst()
        } else {
            null
        }
    }

    fun get_next_int(): Int {
        return this._tree.int_queue.removeFirst()
    }

    fun clear() {
        this._tree.clear()
    }

    fun queue_cell_changes(cells: List<EditorTable.Coordinate>, state_only: Boolean = false) {
        val working_tree = this.get_working_tree() ?: return
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
        val working_tree = this.get_working_tree() ?: return
        if (state_only) {
            working_tree.bill.add(BillableItem.CellStateChange)
        } else {
            working_tree.bill.add(BillableItem.CellChange)
        }


        working_tree.int_queue.add(1)
        working_tree.int_queue.add(cell.y)
        working_tree.int_queue.add(cell.x)
    }

    fun queue_column_changes(columns: List<Int>, state_only: Boolean = false) {
        val working_tree = this.get_working_tree() ?: return
        val bill_item = if (state_only) {
            BillableItem.ColumnStateChange
        } else {
            BillableItem.ColumnChange
        }

        working_tree.bill.add(bill_item)
        working_tree.int_queue.add(columns.size)
        for (column in columns) {
            working_tree.int_queue.add(column)
        }
    }

    fun queue_column_change(column: Int, state_only: Boolean = false) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(
            if (state_only) {
                BillableItem.ColumnStateChange
            } else {
                BillableItem.ColumnChange
            }
        )

        working_tree.int_queue.add(1)
        working_tree.int_queue.add(column)
    }

    fun queue_new_row(y: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.bill.add(BillableItem.RowAdd)
    }

    fun queue_refresh_context_menu() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuRefresh)
    }

    fun queue_set_context_menu_line() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetLine)
    }

    fun queue_set_context_menu_leaf() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetLeaf)
    }

    fun queue_set_context_menu_leaf_percussion() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetLeafPercussion)
    }

    fun queue_set_context_menu_line_control_leaf() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetControlLeaf)
    }

    fun queue_set_context_menu_line_control_leaf_b() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetControlLeafB)
    }

    fun queue_set_context_menu_range() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetRange)
    }

    fun queue_set_context_menu_column() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetColumn)
    }

    fun queue_set_context_menu_control_line() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetControlLine)
    }

    fun queue_set_context_menu_channel() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuSetChannel)
    }

    fun queue_clear_context_menu() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ContextMenuClear)
    }

    fun queue_enable_delete_and_copy_buttons() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ConfigDrawerEnableCopyAndDelete)
    }

    fun queue_config_drawer_redraw_export_button() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ConfigDrawerRefreshExportButton)
    }
    fun queue_project_name_change() {
        val working_tree = this.get_working_tree() ?: return
        working_tree.bill.add(BillableItem.ProjectNameChange)
    }

    fun queue_column_label_refresh(x: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(x)
        working_tree.bill.add(BillableItem.ColumnLabelRefresh)
    }

    fun queue_line_label_refresh(y: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.bill.add(BillableItem.LineLabelRefresh)
    }

    fun queue_row_change(y: Int, state_only: Boolean = false) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.bill.add(
            if (state_only) {
                BillableItem.RowStateChange
            } else {
                BillableItem.RowChange
            }
        )
    }

    fun queue_row_removal(y: Int, count: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.int_queue.add(count)
        working_tree.bill.add(BillableItem.RowRemove)
    }

    fun queue_add_channel(channel: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(channel)
        working_tree.bill.add(BillableItem.ChannelAdd)
    }

    fun queue_refresh_channel(channel: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(channel)
        working_tree.bill.add(BillableItem.ChannelChange)
    }

    fun queue_remove_channel(channel: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(channel)
        working_tree.bill.add(BillableItem.ChannelRemove)
    }

    fun queue_add_column(column: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(column)
        working_tree.bill.add(BillableItem.ColumnAdd)
    }

    fun queue_remove_column(column: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(column)
        working_tree.bill.add(BillableItem.ColumnRemove)
    }

    fun queue_refresh_choose_percussion_button(line_offset: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(line_offset)
        working_tree.bill.add(BillableItem.PercussionButtonRefresh)
    }

    fun queue_full_refresh() {
        this._tree.get(this.working_path).bill.add(BillableItem.FullRefresh)
    }
        
    fun get_working_tree(): Node? {
        return if (this.is_full_locked()) {
            null
        } else {
            this._tree.get(this.working_path)
        }
    }

    fun lock_full() {
        this.ui_lock.lock_full()
        val working_tree = this._tree.get(this.working_path)
        this.working_path.add(working_tree.sub_nodes.size)
        working_tree.new_node()
    }

    fun lock_partial() {
        this.ui_lock.lock_partial()
        val working_tree = this._tree.get(this.working_path)
        this.working_path.add(working_tree.sub_nodes.size)
        working_tree.new_node()
    }

    fun unlock() {
        this.ui_lock.unlock()
        this.working_path.removeLast()
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
