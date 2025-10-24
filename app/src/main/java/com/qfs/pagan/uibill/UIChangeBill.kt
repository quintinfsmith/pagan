package com.qfs.pagan.uibill

import com.qfs.pagan.EditorTable
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.rationaltree.ReducibleTree

/**
* A queue of UI update commands to be executed once it is safe to do so.
*/
class UIChangeBill {
    val ui_lock = UILock()
    private var _tree: Node = Node()
    private val working_path = mutableListOf<Int>()

    fun consolidate() {
        val queued_cells = mutableSetOf<Pair<EditorTable.Coordinate, ReducibleTree<*>?>>()
        val queued_columns = Array<MutableSet<Int>>(2) { mutableSetOf() }
        val queued_line_labels = mutableSetOf<Int>()
        val queued_column_labels = mutableSetOf<Int>()
        var queued_context_menu: BillableItem? = null
        var queued_cursor_scroll: Array<Int>? = null
        val stack = mutableListOf<Node>(this._tree)
        this._tree = Node()
        while (stack.isNotEmpty()) {
            val node = stack.removeAt(0)
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
                    BillableItem.ColumnLabelRefresh,
                    BillableItem.ContextMenuRefresh,
                    BillableItem.ContextMenuSetLine,
                    BillableItem.ContextMenuSetChannel,
                    BillableItem.ContextMenuSetLeaf,
                    BillableItem.ContextMenuSetLeafPercussion,
                    BillableItem.ContextMenuSetControlLeaf,
                    BillableItem.ContextMenuSetControlLeafB,
                    BillableItem.ContextMenuSetRange,
                    BillableItem.ContextMenuSetColumn,
                    BillableItem.ContextMenuSetControlLine,
                    BillableItem.ForceScroll,
                    BillableItem.ContextMenuClear -> {}
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

                    BillableItem.ForceScroll -> {
                        queued_cursor_scroll = Array(7) {
                            node.int_queue.removeAt(0)
                        }
                    }

                    BillableItem.RowAdd -> {
                        val y = node.int_queue.removeAt(0)
                        for ((coord, _) in queued_cells) {
                            if (coord.y >= y) {
                                coord.y += 1
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
                        val y = node.int_queue.removeAt(0)
                        val count = node.int_queue.removeAt(0)

                        val check_range = y until y + count
                        queued_cells -= queued_cells.filter { (coord: EditorTable.Coordinate, _: ReducibleTree<*>?) ->
                            check_range.contains(coord.y)
                        }.toSet()

                        for ((coord, _) in queued_cells) {
                            if (coord.y >= y + count) {
                                coord.y -= count
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
                        val column = node.int_queue.removeAt(0)

                        for ((coord, _) in queued_cells) {
                            if (coord.x >= column) {
                                coord.x += 1
                            }
                        }

                        for (i in 0 until queued_columns.size) {
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
                        val column = node.int_queue.removeAt(0)

                        queued_cells -= queued_cells.filter { (coord: EditorTable.Coordinate, _) ->
                            coord.x == column
                        }.toSet()

                        for ((coord, _) in queued_cells) {
                            if (coord.x > column) {
                                coord.x -= 1
                            }
                        }

                        for (i in 0 until queued_columns.size) {
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
                        this._tree.int_queue.add(node.int_queue.removeAt(0))
                    }

                    BillableItem.LineLabelRefresh -> {
                        queued_line_labels.add(node.int_queue.removeAt(0))
                    }
                    BillableItem.ColumnLabelRefresh -> {
                        queued_column_labels.add(node.int_queue.removeAt(0))
                    }

                    BillableItem.PercussionButtonRefresh -> {
                        this._tree.int_queue.add(node.int_queue.removeAt(0))
                        this._tree.int_queue.add(node.int_queue.removeAt(0))
                    }

                    BillableItem.RowChange -> {
                        val y = node.int_queue.removeAt(0)
                        queued_cells -= queued_cells.filter { (coord: EditorTable.Coordinate, _) ->
                            coord.y == y
                        }.toSet()
                        this._tree.int_queue.add(y)
                    }

                    BillableItem.RowStateChange -> {
                        val y = node.int_queue.removeAt(0)
                        queued_cells -= queued_cells.filter { (coord: EditorTable.Coordinate, cell_tree: ReducibleTree<*>?) ->
                            coord.y == y && cell_tree == null
                        }.toSet()
                        this._tree.int_queue.add(y)
                    }

                    BillableItem.ColumnChange -> {
                        val column = node.int_queue.removeAt(0)
                        queued_cells -= queued_cells.filter { (coord: EditorTable.Coordinate, _) ->
                            coord.x == column
                        }.toSet()

                        queued_columns[1].add(column)
                    }

                    BillableItem.ColumnStateChange -> {
                        val column = node.int_queue.removeAt(0)
                        queued_cells -= queued_cells.filter { (coord: EditorTable.Coordinate, cell_tree: ReducibleTree<*>?) ->
                            coord.x == column && cell_tree == null
                        }.toSet()

                        queued_columns[0].add(column)
                    }

                    BillableItem.CellStateChange,
                    BillableItem.CellChange -> {
                        val state_only = bill_item == BillableItem.CellStateChange
                        queued_cells.add(
                            Pair(
                                EditorTable.Coordinate(
                                    node.int_queue.removeAt(0),
                                    node.int_queue.removeAt(0)
                                ),
                                if (state_only) null else node.tree_queue.removeAt(0)
                            )
                        )
                    }

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
                    BillableItem.ContextMenuSetChannel -> {
                        queued_context_menu = bill_item
                    }

                    BillableItem.ProjectNameChange,
                    BillableItem.ConfigDrawerEnableCopyAndDelete,
                    BillableItem.ConfigDrawerRefreshExportButton -> { }

                }
            }

            stack.addAll(0, node.sub_nodes)
        }

        if (queued_cursor_scroll != null) {
            this._tree.bill.add(BillableItem.ForceScroll)
            this._tree.int_queue.addAll(queued_cursor_scroll)
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

        val processed_cell_coord = mutableSetOf<EditorTable.Coordinate>()

        queued_cells.filter { (_, tree) -> tree != null }.let { cells ->
            if (cells.isEmpty()) return@let

            this._tree.int_queue.add(cells.size)
            for ((coord, tree) in cells) {
                processed_cell_coord.add(coord)
                this._tree.int_queue.add(coord.y)
                this._tree.int_queue.add(coord.x)
                this._tree.tree_queue.add(tree!!)
            }
            this._tree.bill.add(BillableItem.CellChange)
        }

        queued_cells.filter { (coord, tree) -> processed_cell_coord.contains(coord) && tree == null }.let { cells ->
            if (cells.isEmpty()) return@let

            this._tree.int_queue.add(cells.size)
            for ((coord, _) in cells) {
                this._tree.int_queue.add(coord.y)
                this._tree.int_queue.add(coord.x)
            }
            this._tree.bill.add(BillableItem.CellStateChange)
        }

        for (y in queued_line_labels) {
            this._tree.bill.add(BillableItem.LineLabelRefresh)
            this._tree.int_queue.add(y)
        }

        for (x in queued_column_labels) {
            this._tree.bill.add(BillableItem.ColumnLabelRefresh)
            this._tree.int_queue.add(x)
        }

        if (queued_context_menu != null) {
            this._tree.bill.add(queued_context_menu)
        }
    }

    fun get_next_entry(): BillableItem? {
        return if (this._tree.bill.isNotEmpty()) {
            this._tree.bill.removeAt(0)
        } else {
            null
        }
    }

    fun get_next_int(): Int {
        return this._tree.int_queue.removeAt(0)
    }

    fun get_next_tree(): ReducibleTree<*> {
        return this._tree.tree_queue.removeAt(0)
    }

    fun clear() {
        this._tree.clear()
    }

    fun queue_cell_state_changes(cells: List<EditorTable.Coordinate>) {
        for (cell in cells) {
            this.queue_cell_change(cell)
        }
    }

    fun queue_cell_change(cell: EditorTable.Coordinate, tree: ReducibleTree<*>? = null) {
        val working_tree = this.get_working_tree() ?: return
        if (tree == null) {
            working_tree.bill.add(BillableItem.CellStateChange)
        } else {
            working_tree.bill.add(BillableItem.CellChange)
            working_tree.tree_queue.add(tree)
        }

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

        for (column in columns) {
            working_tree.bill.add(bill_item)
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

    fun queue_refresh_choose_percussion_button(channel: Int, line_offset: Int) {
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(channel)
        working_tree.int_queue.add(line_offset)
        working_tree.bill.add(BillableItem.PercussionButtonRefresh)
    }

    fun queue_full_refresh(restore_position: Boolean = false) {
        val working_tree = this.get_working_tree(true) ?: return
        working_tree.bill.add(BillableItem.FullRefresh)
    }

    fun queue_force_scroll(y: Int, x: Int, offset: Rational, offset_width: Rational, force: Boolean) {
        // editor_table.scroll_to_position(y = y, x = beat, offset = offset, offset_width = offset_width, force = force)
        val working_tree = this.get_working_tree() ?: return
        working_tree.int_queue.add(y)
        working_tree.int_queue.add(x)
        working_tree.int_queue.add(offset.numerator)
        working_tree.int_queue.add(offset.denominator)
        working_tree.int_queue.add(offset_width.numerator)
        working_tree.int_queue.add(offset_width.denominator)
        working_tree.int_queue.add(if (force) 1 else 0)
        working_tree.bill.add(BillableItem.ForceScroll)
    }

    fun get_working_tree(force: Boolean = false): Node? {
        // Force is used ONLY to apply FullRefresh
        return if (this.is_full_locked() && ! force) {
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
        if (this.working_path.isNotEmpty()) {
            this.working_path.removeAt(this.working_path.size - 1)
        }
    }

    fun get_level(): Int {
        return this.ui_lock.get_level()
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