package com.qfs.pagan.uibill

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
