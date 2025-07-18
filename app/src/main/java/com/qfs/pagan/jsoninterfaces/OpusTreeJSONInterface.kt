package com.qfs.pagan.jsoninterfaces

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONObject
import com.qfs.pagan.structure.rationaltree.ReducibleTree

class OpusTreeJSONInterface {
    companion object {
        fun <T> to_json(input: ReducibleTree<T>, event_generalizer_callback: (T) -> JSONObject?): JSONObject? {
            if (input.is_leaf() && !input.has_event()) {
                return null
            }

            val map = JSONHashMap()
            if (input.has_event()) {
                map["event"] = event_generalizer_callback(input.get_event()!!)
            } else {
                map["size"] = JSONInteger(input.size)
                val division_keys = input.divisions.keys.toList()

                // Only add entries with data, don't want to bloat with [1, null], [2, null]...etc
                val tmp_list = JSONList()
                for (position in division_keys) {
                    val tmp_entry = to_json(input.divisions[position]!!, event_generalizer_callback)
                    if (tmp_entry != null) {
                        tmp_list.add(
                            JSONList(JSONInteger(position), tmp_entry)
                        )
                    }
                }

                map["divisions"] = tmp_list
            }

            return map
        }

        fun <T> from_json(input: JSONHashMap, event_generalizer_callback: (JSONHashMap?) -> T?): ReducibleTree<T> {
            val new_tree = ReducibleTree<T>()
            val event_hashmap = input.get_hashmapn("event")
            if (event_hashmap != null) {
                new_tree.set_event(event_generalizer_callback(event_hashmap))
            } else {
                new_tree.set_size(input.get_int("size"))
                val divisions = input.get_listn("divisions")
                if (divisions != null) {
                    for (i in divisions.indices) {
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

        fun <T> from_v1_json(input: JSONHashMap, event_generalizer_callback: (JSONHashMap?) -> T?): ReducibleTree<T> {
            val new_tree = ReducibleTree<T>()
            val event_hashmap = input.get_hashmapn("event")
            if (event_hashmap != null) {
                new_tree.set_event(event_generalizer_callback(event_hashmap))
            } else {
                val children = input.get_listn("children")
                if (children != null) {
                    new_tree.set_size(children.size)
                    children.forEachIndexed { i: Int, child_json: JSONObject? ->
                        if (child_json == null) {
                            return@forEachIndexed
                        }
                        new_tree[i] = from_v1_json(child_json as JSONHashMap, event_generalizer_callback)
                    }
                }
            }
            return new_tree
        }

        fun <T> to_v1_json(input: ReducibleTree<T>, event_generalizer_callback: (T) -> JSONObject?): JSONObject? {
            if (input.is_leaf() && !input.has_event()) {
                return null
            }

            val map = JSONHashMap()
            if (input.has_event()) {
                map["event"] = event_generalizer_callback(input.get_event()!!)
                map["children"] = null
            } else {
                map["event"] = null
                map["children"] = JSONList(input.size) { i: Int ->
                    to_v1_json(input[i], event_generalizer_callback)
                }
            }

            return map
        }

        fun convert_v1_to_v3(input: JSONHashMap?, event_converter: (JSONHashMap) -> JSONHashMap?): JSONHashMap? {
            if (input == null) {
                return null
            }

            val output = JSONHashMap()

            val event_hashmap = input.get_hashmapn("event")
            if (event_hashmap != null) {
                output["event"] = event_converter(event_hashmap)
                output["size"] = 0
                output.set_null("divisions")
            } else {
                output.set_null("event")
                val tmp_children = input.get_list("children")
                output["size"] = tmp_children.size
                output["divisions"] = JSONList(tmp_children.size) { i: Int ->
                    val position = i
                    JSONList(
                        JSONInteger(position),
                        convert_v1_to_v3(tmp_children.get_hashmapn(i), event_converter)
                    )
                }
            }

            return output
        }
    }

}
