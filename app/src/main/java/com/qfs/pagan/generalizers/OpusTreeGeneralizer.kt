package com.qfs.pagan.generalizers

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
                                to_json(input.divisions[position]!!, event_generalizer_callback)
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
            if (event_hashmap != null) {
                new_tree.set_event(event_generalizer_callback(event_hashmap))
            } else {
                new_tree.set_size(input.get_int("size"))
                val divisions = input.get_listn("divisions")
                if (divisions != null) {
                    for (i in divisions.list.indices) {
                        val pair = divisions.get_list(i)
                        if (pair.get_hashmapn(1) != null) {
                            new_tree[pair.get_int(0)] = from_json(
                                pair.get_hashmap(1),
                                event_generalizer_callback
                            )
                        }
                    }
                }
            }
            return new_tree
        }

        fun <T> from_v1_json(input: ParsedHashMap, event_generalizer_callback: (ParsedHashMap?) -> T?): OpusTree<T> {
            val new_tree = OpusTree<T>()
            val event_hashmap = input.get_hashmapn("event")
            if (event_hashmap != null) {
                new_tree.set_event(event_generalizer_callback(event_hashmap))
            } else {
                val children = input.get_listn("children")
                if (children != null) {
                    new_tree.set_size(children.list.size)
                    children.list.forEachIndexed { i: Int, child_json: ParsedObject? ->
                        if (child_json == null) {
                            return@forEachIndexed
                        }
                        new_tree[i] = from_v1_json(child_json as ParsedHashMap, event_generalizer_callback)
                    }
                }
            }
            return new_tree
        }

        fun <T> to_v1_json(input: OpusTree<T>, event_generalizer_callback: (T) -> ParsedObject?): ParsedObject? {
            if (input.is_leaf() && !input.is_event()) {
                return null
            }

            val map = HashMap<String, ParsedObject?>()
            if (input.is_event()) {
                map["event"] = event_generalizer_callback(input.get_event()!!)
                map["children"] = null
            } else {
                map["event"] = null
                map["children"] = ParsedList(
                    MutableList(input.size) { i: Int ->
                        to_v1_json(input[i], event_generalizer_callback)
                    }
                )
            }

            return ParsedHashMap(map)
        }

        fun convert_v1_to_v3(input: ParsedHashMap?, event_converter: (ParsedHashMap) -> ParsedHashMap?): ParsedHashMap? {
            if (input == null) {
                return null
            }

            val output = ParsedHashMap()

            val event_hashmap = input.get_hashmapn("event")
            if (event_hashmap != null) {
                output["event"] = event_converter(event_hashmap)
                output["size"] = 0
                output.set_null("divisions")
            } else {
                output.set_null("event")
                val tmp_children = input.get_list("children")
                output["size"] = tmp_children.list.size
                output["divisions"] = ParsedList(
                    MutableList(tmp_children.list.size) { i: Int ->
                        val position = i
                        ParsedList(
                            mutableListOf(
                                ParsedInt(position),
                                convert_v1_to_v3(tmp_children.get_hashmapn(i), event_converter)
                            )
                        )
                    }
                )
            }

            return output
        }
    }

}
