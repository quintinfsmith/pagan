package radixulous.app.structure
import kotlin.math.*

fun greatest_common_denominator(first: Int, second: Int): Int {
    var tmp: Int;
    var a = max(first, second);
    var b = min(first, second);
    while ((a % b) > 0) {
        tmp = a % b
        a = b
        b = tmp
    }
    return b
}

fun get_prime_factors(n: Int): List<Int> {
    var primes: MutableList<Int> = mutableListOf();
    for (i in 2 .. (n / 2) - 1) {
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

    // No Primes found, n is prime
    if (primes.size == 0) {
        primes.add(n)
    }

    var factors: MutableList<Int> = mutableListOf()
    for (p in primes) {
        if (p > n / 2) {
            break
        } else if (n % p == 0) {
            factors.add(p)
        }
    }

    return factors
}

fun lowest_common_multiple(number_list: List<Int>): Int {
    var prime_factors: Array<List<Int>> = Array(number_list.size, {
        i -> get_prime_factors(number_list[i])
    })
    var common_factor_map: HashMap<Int, Int> = HashMap<Int, Int>()
    for (factors in prime_factors) {
        for (factor in factors) {
            if (! common_factor_map.containsKey(factor)) {
                common_factor_map[factor] = 0
            }
            var current = common_factor_map[factor]!!
            common_factor_map[factor] = max(current, factors.count({e -> e == factor}))
        }
    }
    var output = 0;
    for (key in common_factor_map.keys) {
        output += key * common_factor_map[key]!!
    }

    return output
}

data class ReducerTuple<T>(
    var denominator: Int,
    var indices: MutableList<Pair<Int, OpusTree<T>>>,
    var original_size: Int,
    var parent_node: OpusTree<T>
)

public class OpusTree<T> {
    var size: Int = 1
    var divisions: HashMap<Int, OpusTree<T>> = HashMap<Int, OpusTree<T>>()
    var event: T? = null
    var parent: OpusTree<T>? = null

    fun get_parent(): OpusTree<T>? {
        return this.parent
    }
    fun has_parent(): Boolean {
        return this.parent != null
    }
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

        var new_divisions: HashMap<Int, OpusTree<T>> = HashMap<Int, OpusTree<T>>()
        for (current_index in this.divisions.keys) {
            var value = this.divisions[current_index]
            var new_index = current_index * factor
            new_divisions[new_index.toInt()] = value as OpusTree<T>
        }
        this.divisions = new_divisions
        this.size = new_size
    }

    fun reduce(target_size: Int) {
        var indices: MutableList<Pair<Int, OpusTree<T>>> = mutableListOf()
        for (key in this.divisions.keys) {
            indices.add(Pair(key, this.divisions[key] as OpusTree<T>))
        }
        indices.sortWith(compareBy { it.first })

        var place_holder: OpusTree<T> = this.copy()
        var stack: MutableList<ReducerTuple<T>> = mutableListOf(
            ReducerTuple(target_size, indices, this.size, place_holder)
        )

        while (stack.size > 0) {
            var element = stack.removeAt(0)
            var denominator: Int = element.denominator
            var original_size: Int = element.original_size
            var parent_node: OpusTree<T> = element.parent_node;

            var current_size = original_size / denominator;

            var split_indices: Array<MutableList<Pair<Int, OpusTree<T>>>> = Array(denominator, { _ -> mutableListOf() })
            for (index_pair in element.indices) {
                var child_index = index_pair.first;
                var split_index = child_index / current_size
                split_indices[split_index].add(Pair(index_pair.first % current_size, index_pair.second.copy()))
            }

            for (i in 0..denominator - 1) {
                var working_indices = split_indices[i]
                if (working_indices.size == 0) {
                    continue
                }

                var working_node = parent_node.get(i)
                var minimum_divs: MutableList<Int> = mutableListOf()
                for (index_pair in working_indices) {
                    var index: Int = index_pair.first
                    var most_reduced: Int = current_size / greatest_common_denominator(current_size, index)

                    if (most_reduced > 1) {
                        minimum_divs.add(most_reduced)
                    }
                }

                // Remove duplicates in minimum divs
                var j = 0;
                var previous_value = 0;
                while (j < minimum_divs.size) {
                    if (minimum_divs[i] == previous_value) {
                        minimum_divs.removeAt(j);
                    } else {
                        j += 1
                    }
                    previous_value = minimum_divs[j];
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
            this.divisions[key] = place_holder.divisions[key] as OpusTree<T>
        }
    }

    fun copy(): OpusTree<T> {
        val copied: OpusTree<T> = OpusTree<T>()
        copied.size = this.size
        for (key in this.divisions.keys) {
            val subdivision: OpusTree<T> = this.divisions[key] as OpusTree<T>
            val subcopy: OpusTree<T> = subdivision.copy()
            subcopy.set_parent(copied)
            copied.divisions[key] = subcopy
        }

        copied.event = this.event

        return copied
    }

    fun get(rel_index: Int): OpusTree<T> {
        var index = if (rel_index < 0) {
            this.size + rel_index
        } else {
            rel_index
        }

        var output: OpusTree<T>
        if (this.divisions.containsKey(index)) {
            output = this.divisions[index] as OpusTree<T>
        } else {
            output = OpusTree<T>()
            output.set_parent(this)
            this.divisions[index] = output
        }

        return output
    }

    fun flatten() {
        if (this.is_flat()) {
            return
        }

        var sizes: MutableList<Int> = mutableListOf()
        var subnode_backup: MutableList<Pair<Int, OpusTree<T>>> = mutableListOf()

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
            var child: OpusTree<T> = pair.second
            var offset: Int = i * new_chunk_size
            if (! child.is_leaf()) {
                for (key in child.divisions.keys) {
                    var grandchild = child.divisions[key]!!
                    if (grandchild.is_leaf()) {
                        var fine_offset: Int = (key * new_chunk_size) / child.size
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
            var child = this.divisions[key]!!
            noutput = noutput || ! child.is_leaf()
        }
        return ! noutput
    }

    fun is_leaf(): Boolean {
        return this.event != null || this.divisions.keys.size == 0
    }

    fun is_event(): Boolean {
        return this.event != null
    }

    fun set(rel_index: Int, tree: OpusTree<T>) {
        var index = if (rel_index < 0) {
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
        var stack: MutableList<OpusTree<T>> = mutableListOf()
        for (k in this.divisions.keys) {
            stack.add(this.divisions[k]!!)
        }

        while (stack.size > 0) {
            var working_node = stack.removeAt(0)
            if (working_node.is_leaf()) {
                continue
            }
            if (working_node.size == 1) {
                var subnode = working_node.divisions[0]!!
                if (! subnode.is_leaf()) {
                    working_node.replace_with(subnode)
                    stack.add(subnode)
                }
            } else {
                for (child in working_node.divisions.values) {
                    stack.add(child)
                }
            }
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
        }
    }

    fun insert(index: Int?, new_tree: OpusTree<T>) {
        var adj_index = if (index == null) {
            this.size
        } else {
            index
        }

        this.size += 1
        var new_indices = HashMap<Int, OpusTree<T>>()
        for (old_index in this.divisions.keys) {
            var node = this.divisions[old_index]!!
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
        var index = if (x == null) {
            this.size - 1
        } else {
            x
        }

        var output = this.divisions[index]!!
        val new_divisions = HashMap<Int, OpusTree<T>>()
        for (i in this.divisions.keys) {
            if (i < index) {
                new_divisions[i] = this.divisions[i]!!
            } else if (i > index) {
                new_divisions[i - 1] = this.divisions[i]!!
            }
        }
        this.divisions = new_divisions
        this.size -= 1

        return output
    }

    public fun empty() {
        this.divisions = HashMap<Int, OpusTree<T>>()
        this.size = 1
    }
}
