data class ReducerTuple<T>(
    var denominator: Int,
    var indices: MutableList<Pair<Int, Structure<T>>>,
    var original_size: Int,
    var parent_node: Structure<T>
)

class Structure<T> {
    var size: Int = 1
    var divisions: HashMap<Int, Structure<T>> = HashMap<Int, Structure<T>>()
    var event: T? = null
    var parent: Structure<T>? = null

    fun set_size(new_size: Int, noclobber: Boolean = false) {
        if (! noclobber) {
            this.divisions.clear()
        } else if (new_size < this.size) {
            var to_delete: MutableList<Int> = mutableListOf();
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
        var factor: Double = new_size.toDouble() / this.size.toDouble()

        var new_divisions: HashMap<Int, Structure<T>> = HashMap<Int, Structure<T>>()
        for (current_index in this.divisions.keys) {
            var value = this.divisions.get(current_index)
            var new_index = current_index * factor
            new_divisions.put(new_index.toInt(), value as Structure<T>)
        }
        this.divisions = new_divisions
        this.size = new_size
    }

    fun reduce(target_size: Int) {
        var indices: MutableList<Pair<Int, Structure<T>>> = mutableListOf()
        for (key in this.divisions.keys) {
            indices.add(Pair(key, this.divisions.get(key) as Structure<T>))
        }
        indices.sortWith(compareBy { it.first })

        var place_holder: Structure<T> = this.copy()
        var stack: MutableList<ReducerTuple<T>> = mutableListOf(
            ReducerTuple(target_size, indices, this.size, place_holder)
        )

        while (stack.size > 0) {
            var element = stack.removeAt(0)
            var denominator: Int = element.denominator
            var indices: MutableList<Pair<Int, Structure<T>>> = element.indices
            var original_size: Int = element.original_size
            var parent_node: Structure<T> = element.parent_node;

            var current_size = original_size / denominator;


            var split_indices: Array<MutableList<Pair<Int, Structure<T>>>> = Array(denominator, { i -> mutableListOf() })
            for (index_pair in indices) {
                var child_node = index_pair.second as Structure;
                var child_index = index_pair.first;
                var split_index = child_index / current_size
                split_indices[split_index].add(Pair(index_pair.first % current_size, index_pair.second.copy()))
            }

            for (i in 0..denominator) {
                var working_indices = split_indices.get(i)
                if (working_indices.size == 0) {
                    continue
                }

                var working_node = parent_node.get(i)
                var minimum_divs: MutableList<Int> = mutableListOf()
                for (index_pair in working_indices) {
                    var index: Int = index_pair.first
                    var working_subnode: Structure<T> = index_pair.second
                    var most_reduced: Int = current_size / greatest_common_divisor(current_size, index)

                    if (most_reduced > 1) {
                        minimum_divs.add(most_reduced)
                    }
                }

                // Remove duplicates in minimum divs
                var i = 0;
                var previous_value = 0;
                while (i < minimum_divs.size) {
                    if (minimum_divs[i] == previous_value) {
                        minimum_divs.removeAt(i);
                    } else {
                        i += 1
                    }
                    previous_value = minimum_divs[i];
                }
                minimum_divs.sort()


                if (minimum_divs.size > 0) {
                    stack.add(ReducerTuple(
                        denominator = minimum_divs[0],
                        indices = working_indices,
                        original_size = current_size,
                        parent_node = working_node
                    ))
                } else {
                    working_node.event = working_indices[0].second.event
                }
            }
        }
        this.set_size(place_holder.size)
        for (key in place_holder.divisions.keys) {
            this.divisions.put(key, place_holder.divisions[key] as Structure<T>)
        }
    }

    fun copy(): Structure<T> {
        var copied: Structure<T> = Structure<T>()
        copied.size = this.size
        for (key in this.divisions.keys) {
            var subdivision: Structure<T> = this.divisions.get(key) as Structure<T>
            var subcopy: Structure<T> = subdivision.copy()
            subcopy.parent = copied
            copied.divisions.put(key, subcopy)
        }

        copied.event = this.event

        return copied
    }

    fun get(index: Int): Structure<T> {
        var output: Structure<T>
        if (this.divisions.containsKey(index)) {
            output = this.divisions[index] as Structure<T>
        } else {
            output = Structure<T>()
            output.parent = this
            this.divisions.put(index, output)
        }

        return output
    }

    fun flatten() {
        if (this.is_flat()) {
            return
        }

        var sizes: MutableList<Int> = mutableListOf()
        var subnode_backup: MutableList<Pair<Int, Structure<T>>> = mutableListOf()
        var original_size = this.size

        for (key in this.divisions.keys) {
            var child = this.divisions[key]!!
            if (! child.is_leaf()) {
                child.flatten()
                sizes.add(child.size)
            }
            subnode_backup.add(Pair(key, child))
        }
        var new_chunk_size: Int = lowest_common_multiple(sizes)
        var new_size = new_chunk_size * this.size


        this.set_size(new_size)
        for (pair in subnode_backup) {
            var i: Int = pair.first
            var child: Structure<T> = pair.second
            var offset: Int = i * new_chunk_size
            if (! child.is_leaf()) {
                for (key in child.divisions.keys) {
                    var grandchild = child.divisions[key]!!
                    if (grandchild.is_leaf()) {
                        var fine_offset: Int = (key * new_chunk_size) / child.size
                        this.divisions.set(offset + fine_offset, grandchild)
                    }
                }
            } else {
                this.divisions.set(offset, child)
            }
        }
    }

    fun is_flat(): Boolean {
        var noutput: Boolean = false;

        for (key in this.divisions.keys) {
            var child = this.divisions[key]!!
            noutput = noutput || ! child.is_leaf()
        }
        return ! noutput
    }

    fun is_leaf(): Boolean {
        return this.event != null || this.divisions.keys.size == 0
    }
}

fun greatest_common_denominator(first: Int, second: Int): Int {
    
}

fun lowest_common_multiple(number_list: Int): Int {

}

