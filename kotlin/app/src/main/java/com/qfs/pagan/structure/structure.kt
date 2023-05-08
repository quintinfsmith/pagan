package com.qfs.pagan.structure
import kotlin.math.*

fun greatest_common_denominator(first: Int, second: Int): Int {
    if (first == 0 || second == 0) {
        throw Exception("Can't gcd $first and $second")
    }
    var tmp: Int;
    var a = max(first, second);
    var b = min(first, second);
    return if (b > 0) {
        while ((a % b) > 0) {
            tmp = a % b
            a = b
            b = tmp
        }
        b
    } else {
        a
    }
}

fun get_prime_factors(n: Int): List<Int> {
    val primes: MutableList<Int> = mutableListOf();
    for (i in 2 until (n / 2)) {
        var is_prime = true;
        for (p in primes) {
            if (i % p == 0) {
                is_prime = false
                break
            }
        }
        if (is_prime) {
            primes.add(i)
        }
    }


    val factors: MutableList<Int> = mutableListOf()
    for (p in primes) {
        if (p > n / 2) {
            break
        } else if (n % p == 0) {
            factors.add(p)
        }
    }
    // No Primes found, n is prime
    if (factors.size == 0) {
        factors.add(n)
    }

    return factors
}

fun lowest_common_multiple(number_list: List<Int>): Int {
    val prime_factors: Array<List<Int>> = Array(number_list.size) { i ->
        get_prime_factors(number_list[i])
    }

    val common_factor_map: HashMap<Int, Int> = HashMap<Int, Int>()
    for (factors in prime_factors) {
        for (factor in factors) {
            if (! common_factor_map.containsKey(factor)) {
                common_factor_map[factor] = 0
            }
            val current = common_factor_map[factor]!!
            common_factor_map[factor] = max(current, factors.count { e -> e == factor })
        }
    }

    var output = 1;
    for (key in common_factor_map.keys) {
        output *= key * common_factor_map[key]!!
    }

    return output
}

class OpusTree<T> {
    class InvalidGetCall(): Exception("Can't call get() on leaf")
    data class ReducerTuple<T>(
        var denominator: Int,
        var indices: MutableList<Pair<Int, OpusTree<T>>>,
        var original_size: Int,
        var parent_node: OpusTree<T>
    )

    var size: Int = 0
    var divisions: HashMap<Int, OpusTree<T>> = HashMap<Int, OpusTree<T>>()
    var event: T? = null
    var parent: OpusTree<T>? = null

    fun get_parent(): OpusTree<T>? {
        return this.parent
    }
    fun getIndex(): Int? {
        if (this.parent == null) {
            return null
        }

        for ((i, node) in this.parent!!.divisions) {
            if (node == this) {
                return i
            }
        }

        // TODO: Throw error
        return null
    }

    fun set_size(new_size: Int, noclobber: Boolean = false) {
        if (! noclobber) {
            this.divisions.clear()
        } else if (new_size < this.size) {
            val to_delete: MutableList<Int> = mutableListOf();
            for (key in this.divisions.keys) {
                if (key >= new_size) {
                    to_delete.add(key)
                }
            }
            for (key in to_delete) {
                this.divisions.remove(key)
            }
        }

        this.size = new_size
    }

    fun resize(new_size: Int) {
        val factor: Double = new_size.toDouble() / this.size.toDouble()

        val new_divisions: HashMap<Int, OpusTree<T>> = HashMap<Int, OpusTree<T>>()
        for (current_index in this.divisions.keys) {
            val value = this.divisions[current_index]
            val new_index = current_index * factor
            new_divisions[new_index.toInt()] = value as OpusTree<T>
        }
        this.divisions = new_divisions
        this.size = new_size
    }

    fun reduce(target_size: Int = 1) {
        if (this.is_leaf()) {
            return
        }
        if (!this.is_flat()) {
            this.flatten()
        }

        val indices: MutableList<Pair<Int, OpusTree<T>>> = mutableListOf()
        for ((key, child_node) in this.divisions) {
            indices.add(Pair(key, child_node))
        }
        indices.sortWith(compareBy { it.first })

        val place_holder: OpusTree<T> = this.copy()
        val stack = mutableListOf<ReducerTuple<T>>(
            ReducerTuple(
                target_size,
                indices,
                this.size,
                place_holder
            )
        )

        while (stack.size > 0) {
            val element = stack.removeAt(0)
            val denominator: Int = element.denominator
            val original_size: Int = element.original_size
            val parent_node: OpusTree<T> = element.parent_node;
            val current_size = original_size / denominator

            // Create separate lists to represent the new equal groupings
            val split_indices = Array(denominator) { _ ->
                mutableListOf<Pair<Int, OpusTree<T>>>()
            }
            parent_node.set_size(denominator)

            // move the indices into their new lists
            for ((i, subtree) in element.indices) {
                val split_index = min(i / current_size, current_size)
                //val split_index = i / current_size
                split_indices[split_index].add(Pair(i % current_size, subtree.copy()))
            }

            for (i in 0 until denominator) {
                val working_indices = split_indices[i]
                if (working_indices.isEmpty() || parent_node.is_leaf()) {
                    continue
                }

                val working_node = parent_node.get(i)

                // Get the most reduces version of each index
                val minimum_divs = mutableSetOf<Int>()
                for ((index, subtree) in working_indices) {
                    if (index == 0) {
                        continue
                    }

                    val most_reduced: Int = current_size / greatest_common_denominator(current_size, index)

                    if (most_reduced > 1) {
                        minimum_divs.add(most_reduced)
                    }
                }
                val sorted_minimum_divs = minimum_divs.toMutableList()
                sorted_minimum_divs.sort()
                if (sorted_minimum_divs.isNotEmpty()) {
                    stack.add(
                        ReducerTuple(
                            sorted_minimum_divs[0],
                            working_indices,
                            current_size,
                            working_node
                        )
                    )
                } else {
                    val (_, event_tree) = working_indices.removeFirst()
                    if (event_tree.is_event()) {
                        working_node.set_event(event_tree.get_event()!!)
                    }
                }
            }
        }

        this.set_size(place_holder.size)
        for ((key, value) in place_holder.divisions) {
            this.divisions[key] = value
        }
    }

    fun copy(): OpusTree<T> {
        return this.copy(null)
    }

    fun copy(copy_func: ((tree: OpusTree<T>) -> T?)?): OpusTree<T> {
        val copied: OpusTree<T> = OpusTree<T>()
        copied.size = this.size
        for (key in this.divisions.keys) {
            val subdivision: OpusTree<T> = this.divisions[key] as OpusTree<T>
            val subcopy: OpusTree<T> = subdivision.copy()
            subcopy.set_parent(copied)
            copied.divisions[key] = subcopy
        }
        copied.event = this.get_event_copy(copy_func)
        return copied
    }
    fun get_event_copy(copy_func: ((tree: OpusTree<T>) -> T?)? = null): T? {
        return if (copy_func == null) {
            this.copy_event(this)
        } else {
            copy_func(this)
        }
    }

    private fun copy_event(tree: OpusTree<T>): T? {
        return tree.event
    }

    operator fun get(rel_index: Int): OpusTree<T> {
        if (this.is_leaf()) {
            throw InvalidGetCall()
        }

        val index = if (rel_index < 0) {
            this.size + rel_index
        } else {
            rel_index
        }

        if (index >= this.size) {
            throw IndexOutOfBoundsException()
        }

        val output: OpusTree<T>
        if (this.divisions.containsKey(index)) {
            output = this.divisions[index]!!
        } else {
            output = OpusTree()
            output.set_parent(this)
            this.divisions[index] = output
        }


        return output
    }

    fun get(path: List<Int>): OpusTree<T> {
        var working_tree = this
        for (p in path) {
            working_tree = working_tree[p]
        }
        return working_tree
    }

    fun flatten() {
        if (this.is_flat()) {
            return
        }

        val sizes: MutableList<Int> = mutableListOf()
        val subnode_backup: MutableList<Pair<Int, OpusTree<T>>> = mutableListOf()

        for (key in this.divisions.keys) {
            val child = this.divisions[key]!!
            if (! child.is_leaf()) {
                child.flatten()
                sizes.add(child.size)
            }
            subnode_backup.add(Pair(key, child))
        }
        val new_chunk_size: Int = lowest_common_multiple(sizes)
        val new_size = new_chunk_size * max(this.size, 1)

        this.set_size(new_size)
        for (pair in subnode_backup) {
            val i: Int = pair.first
            val child: OpusTree<T> = pair.second
            val offset: Int = i * new_chunk_size
            if (! child.is_leaf()) {
                for (key in child.divisions.keys) {
                    val grandchild = child.divisions[key]!!
                    if (grandchild.is_leaf()) {
                        val fine_offset: Int = (key * new_chunk_size) / child.size
                        this.divisions[offset + fine_offset] = grandchild
                    }
                }
            } else {
                this.divisions[offset] = child
            }
        }
    }

    fun is_flat(): Boolean {
        var noutput: Boolean = false;

        for (key in this.divisions.keys) {
            val child = this.divisions[key]!!
            noutput = noutput || ! child.is_leaf()
        }
        return ! noutput
    }

    fun is_leaf(): Boolean {
        return this.event != null || this.size == 0
    }

    fun is_event(): Boolean {
        return this.event != null
    }

    fun set(rel_index: Int, tree: OpusTree<T>) {
        val index = if (rel_index < 0) {
            this.size + rel_index
        } else {
            rel_index
        }

        this.divisions[index] = tree
    }

    fun set_event(event: T) {
        this.event = event
    }

    fun unset_event() {
        this.event = null
    }

    fun get_event(): T? {
        return this.event
    }

    fun clear_singles() {
        if (this.is_leaf()) {
            return
        }

        if (this.size == 1 && this.divisions.size == 1) {
            var child = this.divisions.remove(0)!!
            if (!child.is_event()) {
                this.set_size(child.size)
                for ((i, grandchild) in child.divisions) {
                    this.divisions[i] = grandchild
                    grandchild.parent = this
                }
            } else {
                this.event = child.get_event()
                return
            }
        }

        for (child in this.divisions.values) {
            child.clear_singles()
        }
    }

    fun replace_with(new_node: OpusTree<T>) {
        if (this.parent != null) {
            val parent = this.parent!!
            for (i in parent.divisions.keys) {
                if (parent.divisions[i] != this) {
                    continue
                }
                parent.divisions[i] = new_node
                break
            }
            new_node.set_parent(parent)
            this.parent = null
        }
    }

    fun insert(index: Int?, new_tree: OpusTree<T>) {
        val adj_index = index ?: this.size

        this.size += 1
        val new_indices = HashMap<Int, OpusTree<T>>()
        for (old_index in this.divisions.keys) {
            val node = this.divisions[old_index]!!
            if (old_index < adj_index) {
                new_indices[old_index] = node
            } else {
                new_indices[old_index + 1] = node
            }
        }
        new_indices[adj_index] = new_tree
        this.divisions = new_indices
        new_tree.set_parent(this)
    }

    protected fun set_parent(new_parent: OpusTree<T>) {
        this.parent = new_parent
    }

    fun pop(x: Int?=null): OpusTree<T> {
        val index = x ?: this.size - 1

        val output = this.divisions[index]!!
        val new_divisions = HashMap<Int, OpusTree<T>>()
        for (i in this.divisions.keys) {
            if (i < index) {
                new_divisions[i] = this.divisions[i]!!
            } else if (i > index) {
                new_divisions[i - 1] = this.divisions[i]!!
            }
        }
        this.divisions = new_divisions
        this.size = max(this.size - 1, 1)

        return output
    }

    fun detach() {
        val parent = this.get_parent() ?: return

        var index: Int? = null

        for (i in parent.divisions.keys) {
            val node = parent.divisions[i]
            if (this == node) {
                index = i
                break
            }
        }
        if (index != null) {
            parent.pop(index)
        }
        this.parent = null
    }

    public fun empty() {
        this.divisions = HashMap<Int, OpusTree<T>>()
        this.size = 0
    }

    fun split(split_func: (event: T) -> Int): List<OpusTree<T>> {
        val merged_map = HashMap<List<Pair<Int, Int>>, MutableList<T>>()
        for ((path, event) in this.get_events_mapped()) {
            if (!merged_map.containsKey(path)) {
                merged_map[path] = mutableListOf()
            }
            merged_map[path]?.add(event)
        }

        val unstructured_splits = HashMap<Int, MutableList<Pair<List<Pair<Int,Int>>, T>>>()
        for ((path, events) in merged_map) {
            for (event in events) {
                val key = split_func(event)
                if (!unstructured_splits.containsKey(key)) {
                    unstructured_splits[key] = mutableListOf()
                }
                unstructured_splits[key]?.add(Pair(path, event))
            }
        }
        val tracks: MutableList<OpusTree<T>> = mutableListOf()
        for ((key, events) in unstructured_splits) {
            val node = OpusTree<T>()
            for ((path, event) in events) {
                var working_node = node
                for ((x, size) in path) {
                    if (working_node.size != size) {
                        working_node.set_size(size)
                    }
                    working_node = working_node.get(x)
                }
                working_node = node
                for ((x, size) in path) {
                    working_node = working_node.get(x)
                }
                working_node.set_event(event)
            }
            tracks.add(node)
        }
        return tracks
    }

    fun get_events_mapped(): List<Pair<List<Pair<Int, Int>>, T>> {
        val output: MutableList<Pair<MutableList<Pair<Int,Int>>, T>> = mutableListOf()
        if (! this.is_leaf()) {
            for ((i, node) in this.divisions) {
                for ((path, event) in node.get_events_mapped()) {
                    val new_path = path.toMutableList()
                    new_path.add(0, Pair(i, this.size))
                    output.add(Pair(new_path, event))
                }
            }
        } else if (this.is_event()) {
            output.add(Pair(mutableListOf(), this.get_event()!!))
        }

        return output
    }

    fun get_max_child_weight(): Int {
        if (this.is_leaf()) {
            return 1
        }

        var max_weight = 1
        for (node in this.divisions.values) {
            if (node.is_leaf()) {
                continue
            }

            max_weight = max(max_weight, (node.size * node.get_max_child_weight()))
        }

        return max_weight
    }

    fun get_set_tree(): OpusTree<Set<T>> {
        val output = OpusTree<Set<T>>()
        if (this.is_event()) {
            output.event = setOf(this.get_event()!!)
        } else if (!this.is_leaf()) {
            output.set_size(this.size)
            for ((index, tree) in this.divisions) {
                output.set(index, tree.get_set_tree())
            }
        }

        return output
    }

    fun merge(tree: OpusTree<Set<T>>): OpusTree<Set<T>> {
        if (tree.is_leaf() && ! tree.is_event()) {
            return this.get_set_tree()
        }
        if (this.is_leaf() && ! this.is_event()) {
            return tree
        }

        return if (!this.is_event()) {
            if (!tree.is_leaf()) {
                this.__merge_structural(tree)
            } else {
                this.__merge_event_into_structural(tree)
            }
        } else if (!tree.is_leaf()) {
            this.__merge_structural_into_event(tree)
        } else {
            this.__merge_event(tree)
        }
    }

    fun __merge_structural_into_event(s_tree: OpusTree<Set<T>>): OpusTree<Set<T>> {
        val output = s_tree.copy()

        var working_tree = output
        while (!working_tree.is_leaf()) {
            working_tree = working_tree.get(0)
        }

        if (working_tree.is_event()) {
            val eventset = working_tree.get_event()!!.toMutableSet()
            eventset.add(this.get_event()!!)
            working_tree.set_event(eventset)
        } else {
            working_tree.set_event(setOf(this.get_event()!!))
        }

        return output
    }

    fun __merge_event_into_structural(e_tree: OpusTree<Set<T>>): OpusTree<Set<T>> {
        val output = this.get_set_tree()

        var working_tree = output
        while (!working_tree.is_leaf()) {
            working_tree = working_tree.get(0)
        }

        if (working_tree.is_event()) {
            val eventset = working_tree.get_event()!!.toMutableSet()
            for (elm in e_tree.get_event()!!) {
                eventset.add(elm)
            }
            working_tree.set_event(eventset)
        } else {
            working_tree.set_event(e_tree.get_event()!!)
        }

        return output
    }

    fun __merge_event(event_node: OpusTree<Set<T>>): OpusTree<Set<T>> {
        val output = OpusTree<Set<T>>()
        val eventset = event_node.get_event()!!.toMutableSet()
        eventset.add(this.get_event()!!)

        output.set_event(eventset)
        return output
    }

    private fun __merge_structural(tree: OpusTree<Set<T>>): OpusTree<Set<T>> {
        var original_size = this.size
        val other = tree.copy()

        val this_multi = this.get_set_tree()
        other.flatten()
        this_multi.flatten()
        val new_size = lowest_common_multiple(listOf(max(1, this_multi.size), max(1, other.size)))

        val factor = new_size / max(1, other.size)
        this_multi.resize(new_size)
        for ((index, subtree) in other.divisions) {
            val new_index = index * factor
            val subtree_into = this_multi.get(new_index)

            if (subtree.is_event()) {
                val eventset = if (subtree_into.is_event()) {
                    subtree_into.get_event()!!.toMutableSet()
                } else {
                    mutableSetOf()
                }

                for (elm in subtree.get_event()!!) {
                    eventset.add(elm)
                }

                subtree_into.set_event(eventset)
            }
        }

        return this_multi
    }

    fun to_string(): String {
        return if (this.is_event()) {
            "${this.get_event()!!}"
        } else if (this.is_leaf()) {
            "_"
        } else {
            var output = ""
            for (i in 0 until this.size) {
                output = if (i > 0) {
                    "$output,${this.get(i).to_string()}"
                } else {
                    "${this.get(i).to_string()}"
                }
            }
            "($output)"
        }
    }

    fun get_first_event_tree_position(): List<Int>? {
        if (this.is_event()) {
            return listOf<Int>()
        } else if (this.is_leaf()) {
            return null
        }



        val output = mutableListOf<Int>()
        for ((i, child) in this.divisions) {
            val result = child.get_first_event_tree_position() ?: continue

            output.add(i)
            for (j in result) {
                output.add(j)
            }
            return output
        }

        return null
    }

    fun get_quantization_map(input_factors: List<Int>): HashMap<Int, MutableList<Int>> {
        var n = this.size

        var product = 1
        // Get applicable product (ignore non-cofactors)
        for (factor in input_factors) {
            if (n % factor == 0) {
                product *= factor
                n /= factor
            }
        }

        var quotient = this.size / product


        var position_remap = HashMap<Int, MutableList<Int>>()
        for ((position, _) in this.divisions) {
            var ratio: Int = round(position.toFloat() / quotient.toFloat()).toInt()
            var new_position = ratio * quotient

            if (!position_remap.contains(new_position)) {
                position_remap[new_position] = mutableListOf()
            }
            position_remap[new_position]!!.add(position)
        }

        return position_remap
    }

    // Relative Position and Size
    fun get_flat_ratios(): Pair<Float, Float> {
        var top = this
        if (top.parent == null) {
            return Pair(0F, 1F)
        }

        var ratio = top.getIndex()!!.toFloat() / top.parent!!.size.toFloat()
        var total_divs = top.parent!!.size

        while (true) {
            top = top.parent!!
            if (top.parent != null) {
                ratio = (ratio / top.parent!!.size.toFloat()) + (top.getIndex()!!.toFloat() / top.parent!!.size.toFloat())
                total_divs *= top.parent!!.size
            } else {
                break
            }
        }

        return Pair(ratio, 1F / total_divs.toFloat())
    }
}
