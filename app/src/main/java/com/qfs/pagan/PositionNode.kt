package com.qfs.pagan

class PositionNode(var position: Int? = null) {
    var previous: PositionNode? = null
    fun to_list(): List<Int> {
        val output = mutableListOf<Int>()
        if (this.previous != null) {
            for (v in this.previous!!.to_list()) {
                output.add(v)
            }
        }
        if (this.position != null) {
            output.add(this.position!!)
        }
        return output
    }
}