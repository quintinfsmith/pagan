package com.qfs.pagan.structure.rationaltree

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.greatest_common_denominator
import com.qfs.pagan.structure.lowest_common_multiple
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.max
import kotlin.math.round

/**
 * A Tree data structure that can be flattened and re-inflated while keeping the proportional
 * positions of its data in-tact.
 */
class ReducibleTree<T> {
    companion object {

        fun <T> get_set_tree(input: ReducibleTree<T>): ReducibleTree<Set<T>> {
            val output = ReducibleTree<Set<T>>()

            if (!input.has_event()) {
                output.set_size(input.size)
                for ((index, tree) in input.divisions) {
                    output[index] = this.get_set_tree(tree)
                }
            } else {
                output.event = setOf(input.get_event()!!)
            }

            return output
        }

    }
    private data class ReducerTuple<T>(
        var denominator: Int,
        var indices: MutableList<Pair<Int, ReducibleTree<T>>>,
        var original_size: Int,
        var parent_node: ReducibleTree<T>
    )

    private var _real_size: Int = 0
    val size get() = max(1, this._real_size)
    var divisions = HashMap<Int, ReducibleTree<T>>()
    var event: T? = null
    var parent: ReducibleTree<T>? = null

    operator fun get(vararg rel_indices: Int): ReducibleTree<T> {
        var output: ReducibleTree<T> = this
        for (rel_index in rel_indices) {
            if (output.is_leaf()) {
                throw InvalidGetCall()
            }

            val index = if (rel_index < 0) {
                this._real_size + rel_index
            } else {
                rel_index
            }

            if (index >= this._real_size) {
                throw InvalidGetCall()
            }

            if (this.divisions.containsKey(index)) {
                output = this.divisions[index]!!
            } else {
                output = ReducibleTree()
                output.set_parent(this)
                this.divisions[index] = output
            }
        }

        return output
    }

    operator fun set(rel_index: Int, tree: ReducibleTree<T>) {
        val index = if (rel_index < 0) {
            this._real_size + rel_index
        } else {
            rel_index
        }
        this.divisions[index] = tree
        tree.parent = this
    }

    fun get_parent(): ReducibleTree<T>? {
        return this.parent
    }

    /**
     * Get index relative to siblings if applicable, null otherwise
     */
    fun get_index(): Int? {
        if (this.parent == null) {
            return null
        }

        for ((i, node) in this.parent!!.divisions) {
            if (node === this) {
                return i
            }
        }

        throw UntrackedInParentException()
    }

    /**
     * Change the number of children to [new_size].
     * When [noclobber], existing data will not be cleared where it's position is less than [new_size]
     * Will *NOT* maintain proportional positions.
     */
    fun set_size(new_size: Int, noclobber: Boolean = false) {
        if (! noclobber) {
            this.divisions.clear()
        } else if (new_size < this.size) {
            val to_delete: MutableList<Int> = mutableListOf()
            for (key in this.divisions.keys) {
                if (key >= new_size) {
                    to_delete.add(key)
                }
            }
            for (key in to_delete) {
                this.divisions.remove(key)
            }

        }

        this._real_size = new_size
    }

    /**
     * Change the number of children to [new_size].
     * Existing children are repositioned to maintain relative positions.
     * It should be noted that clobbering can happen when reducing size.
     */
    fun resize(new_size: Int) {
        val factor: Double = new_size.toDouble() / this._real_size.toDouble()
        val new_divisions = HashMap<Int, ReducibleTree<T>>()
        for (current_index in this.divisions.keys) {
            val value = this.divisions[current_index]
            val new_index = current_index * factor
            new_divisions[new_index.toInt()] = value as ReducibleTree<T>
        }

        this.divisions = new_divisions
        this._real_size = new_size
    }

    /**
     * Adjust the number of children to their lowest value (or value closest to [target_size]) without losing precision.
     */
    fun reduce(target_size: Int = 1) {
        if (this.is_leaf()) {
            return
        }
        if (!this.is_flat()) {
            this.flatten()
        }

        val indices: MutableList<Pair<Int, ReducibleTree<T>>> = mutableListOf()
        for ((key, child_node) in this.divisions) {
            if (child_node.has_event()) {
                indices.add(Pair(key, child_node))
            }
        }
        indices.sortWith(compareBy { it.first })
        val place_holder: ReducibleTree<T> = this.copy()
        val stack = mutableListOf(
            ReducerTuple(
                target_size,
                indices,
                this.size,
                place_holder
            )
        )

        while (stack.isNotEmpty()) {
            val element = stack.removeAt(0)
            val denominator: Int = element.denominator
            val original_size: Int = element.original_size
            val parent_node: ReducibleTree<T> = element.parent_node
            val current_size = original_size / denominator

            // Create separate lists to represent the new equal groupings
            val split_indices = Array(denominator) {
                mutableListOf<Pair<Int, ReducibleTree<T>>>()
            }
            parent_node.set_size(denominator)

            // move the indices into their new lists
            for ((i, subtree) in element.indices) {
                val split_index = i / current_size
                split_indices[split_index].add(Pair(i % current_size, subtree))
            }

            for (i in 0 until denominator) {
                val working_indices = split_indices[i]
                if (working_indices.isEmpty() || parent_node.is_leaf()) {
                    continue
                }

                val working_node = parent_node[i]

                // Get the most reduces version of each index
                val minimum_divs = mutableSetOf<Int>()
                for ((index, _) in working_indices) {
                    if (index == 0) {
                        continue
                    }

                    val most_reduced: Int = current_size / greatest_common_denominator(
                        current_size,
                        index
                    )

                    if (most_reduced > 1) {
                        minimum_divs.add(most_reduced)
                    }
                }

                if (minimum_divs.isNotEmpty()) {
                    val sorted_minimum_divs = minimum_divs.toMutableList()
                    sorted_minimum_divs.sort()

                    stack.add(
                        ReducerTuple(
                            sorted_minimum_divs[0],
                            working_indices,
                            current_size,
                            working_node
                        )
                    )
                } else {
                    val (_, event_tree) = working_indices.removeAt(0)
                    if (event_tree.has_event()) {
                        working_node.set_event(event_tree.get_event()!!)
                    }
                }
            }
        }

        place_holder.clear_singles()

        if (place_holder.has_event()) {
            this.set_event(place_holder.get_event())
        } else {
            this.set_size(place_holder.size)
            for ((key, value) in place_holder.divisions) {
                this[key] = value
            }
        }
    }

    /**
     * Copy the tree. If set, [copy_func] Should be a function that returns a copy of the event,
     * But could return any event of type [T] if you know what you're doing.
     */
    fun copy(copy_func: ((event: T) -> T)? = null): ReducibleTree<T> {
        val copied = ReducibleTree<T>()
        copied._real_size = this._real_size
        for (key in this.divisions.keys) {
            val subdivision: ReducibleTree<T> = this.divisions[key] as ReducibleTree<T>
            val subcopy: ReducibleTree<T> = subdivision.copy(copy_func)
            subcopy.set_parent(copied)
            copied.divisions[key] = subcopy
        }
        if (this.event != null) {
            copied.event = if (copy_func == null) {
                this.event
            } else {
                 copy_func(this.event!!)
            }
        }
        return copied
    }

    fun flatten() {
        if (this.has_event() || this.is_flat()) {
            return
        }

        val sizes: MutableList<Int> = mutableListOf()
        val subnode_backup: MutableList<Pair<Int, ReducibleTree<T>>> = mutableListOf()

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
            val child: ReducibleTree<T> = pair.second
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
        var noutput = false

        for (key in this.divisions.keys) {
            val child = this.divisions[key]!!
            noutput = noutput || ! child.is_leaf()
        }
        return ! noutput
    }

    fun is_leaf(): Boolean {
        return this.event != null || (this.divisions.isEmpty() && this._real_size == 0)
    }

    fun has_event(): Boolean {
        return this.event != null
    }

    fun set_event(event: T?) {
        this.event = event
        this._real_size = 0
        this.divisions.clear()
    }

    fun unset_event() {
        this.event = null
        this._real_size = 0
        this.divisions.clear()
    }

    fun get_event(): T? {
        return this.event
    }

    fun get_path(): List<Int> {
        var tree = this
        val output = mutableListOf<Int>()
        while (tree.parent != null) {
            output.add(tree.get_index()!!)
            tree = tree.parent!!
        }
        return output.asReversed()
    }

    fun clear_singles() {
        if (this.is_leaf()) {
            return
        }

        for (child in this.divisions.values) {
            child.clear_singles()
        }

        if (this.size == 1 && this.divisions.size == 1) {
            val child = this.divisions.remove(0)!!
            if (!child.has_event()) {
                this.set_size(child.size)
                for ((i, grandchild) in child.divisions) {
                    this.divisions[i] = grandchild
                    grandchild.parent = this
                }
            } else {
                this.set_event(child.get_event())
            }
        }
    }

    fun replace_with(new_node: ReducibleTree<T>) {
        if (this.parent != null) {
            val parent = this.parent!!
            for (i in parent.divisions.keys) {
                if (parent.divisions[i] !== this) {
                    continue
                }
                parent.divisions[i] = new_node
                break
            }
            new_node.set_parent(parent)
            this.parent = null
        }
    }

    fun insert(index: Int?, new_tree: ReducibleTree<T>? = null) {
        val adj_index = index ?: this.size
        val adj_new_tree = new_tree ?: ReducibleTree()

        this._real_size += 1
        val new_indices = HashMap<Int, ReducibleTree<T>>()
        for (old_index in this.divisions.keys) {
            val node = this.divisions[old_index]!!
            if (old_index < adj_index) {
                new_indices[old_index] = node
            } else {
                new_indices[old_index + 1] = node
            }
        }
        new_indices[adj_index] = adj_new_tree
        this.divisions = new_indices
        adj_new_tree.set_parent(this)
    }

    private fun set_parent(new_parent: ReducibleTree<T>) {
        this.parent = new_parent
    }

    /**
     * Detach and return child at [x] or last immediate child of the node.
     */
    fun pop(x: Int? = null): ReducibleTree<T> {
        val index = x ?: (this.size - 1)
        val output = this.divisions.remove(index)!!
        for (i in this.divisions.keys.sorted()) {
            if (i > index) {
                this.divisions[i - 1] = this.divisions.remove(i)!!
            }
        }
        this._real_size = max(this._real_size - 1, 0)
        return output
    }

    /**
     * If the tree is attached to a parent tree, remove it.
     */
    fun detach() {
        val parent = this.get_parent() ?: return

        var index: Int? = null

        for ((i, node) in parent.divisions) {
            if (this === node) {
                index = i
                break
            }
        }
        if (index != null) {
            parent.pop(index)
        }
        this.parent = null
    }

    /**
     * Remove all children of the tree.
     */
    fun empty() {
        this.divisions = HashMap()
        this._real_size = 0
    }

    fun split(split_func: (event: T) -> Int): List<ReducibleTree<T>> {
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
        val tracks: MutableList<ReducibleTree<T>> = mutableListOf()
        for ((_, events) in unstructured_splits) {
            val node = ReducibleTree<T>()
            for ((path, event) in events) {
                var working_node = node
                for ((x, size) in path) {
                    if (working_node._real_size != size) {
                        working_node.set_size(size)
                    }
                    working_node = working_node[x]
                }
                working_node = node
                for ((x, _) in path) {
                    working_node = working_node[x]
                }
                working_node.set_event(event)
            }
            tracks.add(node)
        }
        return tracks
    }

    /**
     * Get a flat representation of the tree.
     * Return type is a pair with the first element being a list of index/size of each given tree
     * and the second element being the event data
     */
    fun get_events_mapped(): List<Pair<List<Pair<Int, Int>>, T>> {
        val output: MutableList<Pair<MutableList<Pair<Int,Int>>, T>> = mutableListOf()
        if (! this.is_leaf()) {
            for ((i, node) in this.divisions) {
                for ((path, event) in node.get_events_mapped()) {
                    val new_path = path.toMutableList()
                    new_path.add(0, Pair(i, this._real_size))
                    output.add(Pair(new_path, event))
                }
            }
        } else if (this.has_event()) {
            output.add(Pair(mutableListOf(), this.get_event()!!))
        }

        return output
    }

    /**
     * Get the largest size of all branches.
     */
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

    fun get_total_child_weight(): Int {
        if (this.is_leaf()) {
            return 1
        }

        var max_weight = 1
        for (node in this.divisions.values) {
            if (node.is_leaf()) {
                continue
            }

            max_weight = max(max_weight, node.get_total_child_weight())
        }

        return max_weight * this.size
    }

    /**
     * Get a copy of this tree merged with another where conflicts are avoided by
     * converted event data into sets of event data
     */
    fun merge(tree: ReducibleTree<Set<T>>): ReducibleTree<Set<T>> {
        if ((tree.is_leaf() && ! tree.has_event()) || tree.is_eventless()) {
            return ReducibleTree.get_set_tree(this)
        }
        if ((this.is_leaf() && ! this.has_event()) || this.is_eventless()) {
            return tree
        }

        return if (!this.has_event()) {
            if (tree.is_leaf()) {
                this.__merge_event_into_structural(tree)
            } else {
                this.__merge_structural(tree)
            }
        } else if (!tree.is_leaf()) {
            this.__merge_structural_into_event(tree)
        } else {
            this.__merge_event(tree)
        }
    }

    /**
     * Merge a tree without events into a tree that has no children but does have an event
     */
    private fun __merge_structural_into_event(s_tree: ReducibleTree<Set<T>>): ReducibleTree<Set<T>> {
        val output = s_tree.copy()

        var working_tree = output
        while (!working_tree.is_leaf()) {
            working_tree = working_tree[0]
        }

        if (working_tree.has_event()) {
            val eventset = working_tree.get_event()!!.toMutableSet()
            eventset.add(this.get_event()!!)
            working_tree.set_event(eventset)
        } else {
            working_tree.set_event(setOf(this.get_event()!!))
        }

        return output
    }

    /**
     * merge a tree without children and an event into a tree that has no event and some number of children
     */
    private fun __merge_event_into_structural(e_tree: ReducibleTree<Set<T>>): ReducibleTree<Set<T>> {
        val output = ReducibleTree.get_set_tree(this)

        var working_tree = output
        while (!working_tree.is_leaf()) {
            working_tree = working_tree[0]
        }

        if (working_tree.has_event()) {
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

    /**
     * merge trees with events. Conflicts are handle by handle event data as sets.
     */
    private fun __merge_event(event_node: ReducibleTree<Set<T>>): ReducibleTree<Set<T>> {
        val output = ReducibleTree<Set<T>>()
        val eventset = event_node.get_event()!!.toMutableSet()
        eventset.add(this.get_event()!!)

        output.set_event(eventset)
        return output
    }

    /**
     * merge to trees without immediate events.
     */
    private fun __merge_structural(tree: ReducibleTree<Set<T>>): ReducibleTree<Set<T>> {
        val other = tree.copy()

        val this_multi = ReducibleTree.get_set_tree(this)
        other.flatten()
        this_multi.flatten()

        val factor = this_multi.size
        this_multi.resize(this_multi.size * other.size)

        for ((index, subtree) in other.divisions) {
            val new_index = index * factor
            val subtree_into = this_multi[new_index]

            if (subtree.has_event()) {
                val eventset: MutableSet<T> = if (subtree_into.has_event()) {
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
        return if (this.has_event()) {
            "${this.get_event()!!}"
        } else if (this.is_leaf()) {
            "_"
        } else {
            var output = ""
            for (i in 0 until this.size) {
                output = if (i > 0) {
                    "$output,${this[i].to_string()}"
                } else {
                    this[i].to_string()
                }
            }
            "($output)"
        }
    }

    /**
     * Do a depth-first search and return the path taken to find the first event, if it exists.
     */
    fun get_first_event_tree_position(): List<Int>? {
        if (this.has_event()) {
            return listOf()
        } else if (this.is_leaf()) {
            return null
        }
        val output = mutableListOf<Int>()
        for (i in this.divisions.keys.toList().sorted()) {
            val child = this.divisions[i]!!
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

        val quotient = this.size / product

        val position_remap = HashMap<Int, MutableList<Int>>()
        for ((position, _) in this.divisions) {
            val ratio: Int = round(position.toFloat() / quotient.toFloat()).toInt()
            val new_position = ratio * quotient

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

        var ratio = top.get_index()!!.toFloat() / top.parent!!.size.toFloat()
        var total_divs = top.parent!!.size

        while (true) {
            top = top.parent!!
            if (top.parent != null) {
                ratio = (ratio / top.parent!!.size.toFloat()) + (top.get_index()!!.toFloat() / top.parent!!.size.toFloat())
                total_divs *= top.parent!!.size
            } else {
                break
            }
        }

        return Pair(ratio, 1F / total_divs.toFloat())
    }

    /**
     * Depth-first traversal of the tree. Call [callback] at every node.
     */
    fun traverse(callback: (ReducibleTree<T>, T?) -> Unit) {
        if (! this.is_leaf()) {
            for ((_, tree) in this.divisions) {
                tree.traverse(callback)
            }
        }
        callback(this, this.event)
    }

    /**
     * Does this tree or it's descendants have an event?
     */
    fun is_eventless(): Boolean {
        var has_event = false
        this.traverse { _: ReducibleTree<T>, event: T? ->
            if (event != null) {
                has_event = true
                return@traverse
            }
        }
        return !has_event
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ReducibleTree<*>) {
            if (this.has_event() != other.has_event()) {
                false
            } else if (this.has_event() && other.has_event()) {
                this.get_event() == other.get_event()
            } else if (this.is_leaf() && other.is_leaf()) {
                true
            } else if (other._real_size == this._real_size) {
                var is_match = true
                for (i in 0 until this.size) {
                    if (this[i] != other[i]) {
                        is_match = false
                        break
                    }
                }
                is_match
            } else {
                false
            }
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return if (this.has_event()) {
            this.get_event()!!.hashCode()
        } else if (this.is_leaf()) {
            0
        } else {
            var output = 0
            for (i in 0 until this.size) {
                output = (output shl 1)
                if (this.divisions.containsKey(i)) {
                    output = output.xor(this.divisions[i].hashCode())
                }
            }
            output
        }
    }

    fun get_rational_position(position: List<Int>): Rational {
        var rational_position = Rational(0, 1)
        var working_tree = this
        var r = 1
        for (p in position) {
            r *= working_tree.size
            rational_position += Rational(p, r)
            try {
                working_tree = working_tree[p]
            } catch (e: InvalidGetCall) {
                break
            }
        }

        return rational_position
    }

    fun get_closest_position(position: List<Int>): List<Int> {
        return this.get_closest_position(this.get_rational_position(position))
    }

    fun get_tree_width(position: List<Int>): Int {
        var output = 1
        var working_tree = this
        for (p in position) {
            if (working_tree.is_leaf()) {
                break
            }

            output *= working_tree.size
            working_tree = working_tree[p]
        }

        return output
    }

    fun get_closest_position(rational_position: Rational): List<Int> {
        var working_tree = this
        var working_position = rational_position.copy()
        val output = mutableListOf<Int>()

        while (!working_tree.is_leaf()) {
            val child_position = working_position.numerator * working_tree.size / working_position.denominator

            working_position -= Rational(child_position, working_tree.size)
            working_position *= working_tree.size
            working_tree = working_tree[child_position]

            output.add(child_position)
        }

        return output
    }
}