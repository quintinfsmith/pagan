package com.qfs.pagan.opusmanager.generalizers

import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt
import com.qfs.json.ParsedList
import com.qfs.json.ParsedObject
import com.qfs.pagan.structure.OpusTree

class OpusTreeGeneralizer {
    companion object {
        fun <T> to_json(input: OpusTree<T>, event_generalizer_callback: (T) -> ParsedObject?): ParsedObject? {
            if (input.is_leaf() && !input.is_event()) {
                return null
            }

            val map = HashMap<String, ParsedObject?>()
            if (input.is_event()) {
                map["event"] = event_generalizer_callback(input.get_event()!!)
            } else {
                map["size"] = ParsedInt(input.size)
                val division_keys = input.divisions.keys.toList()
                map["divisions"] = ParsedList(
                    MutableList(division_keys.size) { i: Int ->
                        val position = division_keys[i]
                        ParsedList(
                            mutableListOf(
                                ParsedInt(position),
                                OpusTreeGeneralizer.to_json(input.divisions[position]!!, event_generalizer_callback)
                            )
                        )
                    }
                )
            }

            return ParsedHashMap(map)
        }

        fun <T> from_json(input: ParsedHashMap, event_generalizer_callback: (ParsedHashMap?) -> T?): OpusTree<T> {
            val new_tree = OpusTree<T>()
            val event_hashmap = input.get_hashmapn("event")
            if (event_hashmap != null ) {
                new_tree.set_event(event_generalizer_callback(event_hashmap))
            } else {
                val children = input.get_listn("children")
                if (children != null) {
                    new_tree.set_size(children.list.size)
                    children.list.forEachIndexed { i: Int, child_json: ParsedObject? ->
                        if (child_json == null) {
                            return@forEachIndexed
                        }
                        new_tree.set(i, OpusTreeGeneralizer.from_json(child_json as ParsedHashMap, event_generalizer_callback))
                    }
                }
            }
            return new_tree
        }
    }

}
