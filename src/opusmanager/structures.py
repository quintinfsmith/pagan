"""
    Specialized generic structures to help with midi note processing.
    Only OpusTree at the moment.
"""
from __future__ import annotations
import math, json
from enum import Enum, auto
from typing import Optional, List, Tuple, Dict, TypeVar

class BadStateError(Exception):
    """Thrown if an incompatible operation is attempted on a OpusTree Object"""
class SelfAssignError(Exception):
    """Thrown when a OpusTree is assigned to itself as a subtree"""

T = TypeVar("T")

class OpusTree:
    """
        Tree-like structure that can be flattened and
        unflattened as necessary while keeping relative positions
    """
    uuid_gen = 0
    def __init__(self):
        self.size: int = 1
        self.divisions = {}
        self.event: Optional[T] = None
        self.parent: Optional[OpusTree] = None
        self.uuid: int = OpusTree.uuid_gen
        OpusTree.uuid_gen += 1

    def get_uuid(self):
        return self.uuid

    def __str__(self):
        output = ''
        if self.is_event():
            output += str(self.event)
        else:
            for i, tree in self.divisions.items():
                tree_str = str(tree).strip()
                if tree_str:
                    tab = "\t" * self.get_depth()
                    output += f"{tab}{i+1}/{len(self)}) \t{tree_str}\n"

        return output.strip() + "\n"

    def __len__(self):
        return self.size

    def __getitem__(self, index_or_slice) -> OpusTree:
        """
            Get the node at the specified index.
            Will create a new tree if none exists yet
        """
        if self.is_event():
            raise BadStateError()

        if isinstance(index_or_slice, int):
            output = self.__getitem_by_index(index_or_slice)
        else:
            output = self.__get_slice(index_or_slice)

        return output

    def __getitem_by_index(self, i: int) -> OpusTree:
        if i < 0:
            i = self.size + i

        if i >= self.size:
            raise IndexError()

        try:
            output = self.divisions[i]
        except KeyError:
            output = self.__class__()
            self[i] = output
            output.parent = self

        return output

    def __get_slice(self, s: slice) -> OpusTree:
        output = self.__class__()
        if s.start is None:
            start = 0
        else:
            start = s.start

        if s.step is None:
            step = 1
        else:
            step = s.step

        if s.stop is None:
            stop = len(self)
        else:
            stop = s.stop

        output.set_size((stop - start) // step)
        for i, node in self.divisions.items():
            if start <= i < stop and (i + start) % step == 0:
                output[i - start] = node

        return output

    def __setitem__(self, i: int, node: OpusTree):
        """Assign an existing OpusTree to be a node."""

        if node == self:
            raise SelfAssignError()

        if i < 0:
            i = self.size + i

        if i >= self.size:
            raise IndexError(i, self.size)


        node.parent = self
        self.divisions[i] = node

        if self.is_event():
            event = self.get_event()
            self.event = None
            while not node.is_leaf():
                node = node[0]

            if node.is_event():
                node.event += event
            else:
                node.set_event(event)

    def get_parent(self) -> Optional[OpusTree]:
        return self.parent

    def split(self, split_func) -> List[OpusTree]:
        mapped_events = self.get_events_mapped()
        merged_map = {}
        for path, event in mapped_events:
            tpath = tuple(path)
            if tpath not in merged_map:
                merged_map[tpath] = []
            merged_map[tpath].append(event)

        unstructured_splits = {}
        for path, events in merged_map.items():
            for event in events:
                key = split_func(event, events)
                if key not in unstructured_splits:
                    unstructured_splits[key] = []
                unstructured_splits[key].append((path, event))

        tracks = []
        for key, events in unstructured_splits.items():
            node = self.__class__()
            for (path, event) in events:
                working_node = node
                for (x, size) in path:
                    if len(working_node) != size:
                        working_node.set_size(size)
                    working_node = working_node[x]

                working_node = node
                for (x, size) in path:
                    working_node = working_node[x]

                working_node.set_event(event, merge=True)

            tracks.append(node)

        return tracks

    def get_events_mapped(self):
        output = []
        if not self.is_leaf():
            for i, node in self.divisions.items():
                for (path, event) in node.get_events_mapped():
                    path.insert(0, (i, len(self)))
                    output.append((path, event))

        elif self.is_event():
            output.append(([], self.event))

        return output

    def insert(self, index, new_node):
        self.size += 1
        new_indices = {}
        for old_index, node in self.divisions.items():
            if index > old_index:
                new_indices[old_index] = node
            else:
                new_indices[old_index + 1] = node
        new_indices[index] = new_node
        self.divisions = new_indices
        new_node.parent = self

    def merge(self, tree):
        if tree.is_open():
            return

        if not self.is_event():
            if not tree.is_leaf():
                self.__merge_structural(tree)
            else:
                self.__merge_event_into_structural(tree)
        elif not tree.is_open():
            if not tree.is_leaf():
                self.__merge_structural_into_event(tree)
            else:
                self.__merge_event(tree)

    def __merge_structural_into_event(self, s_tree):
        clone_tree = self.copy()
        self.unset_event()

        self.set_size(len(s_tree))
        self.__merge_structural(s_tree)

        self.__merge_event_into_structural(clone_tree)

    def __merge_event_into_structural(self, e_tree):
        working_tree = self
        while not working_tree.is_leaf():
            working_tree = working_tree[0]

        # TODO: require events have __add__
        working_tree.event += e_tree.event

    def __merge_event(self, event_node):
        # TODO: require events have __add__
        self.event += event_node.event

    def __merge_structural(self, tree):
        original_size = len(self)
        clone = tree.copy()
        clone.flatten()
        self.flatten()

        new_size = math.lcm(len(self), len(clone))
        factor = new_size // len(clone)
        self.resize(new_size)

        for index, subtree in clone.divisions.items():
            new_index = index * factor
            subtree_into = self[new_index]

            if subtree_into.is_open():
                self[new_index] = subtree
            else:
                subtree_into.merge(subtree)

        self.reduce(max(original_size, len(tree)))

    def clear(self):
        for item in self:
            del item
        self.set_size(1)

    def is_open(self) -> bool:
        """Check that this has no subtrees and is not an event"""
        return not self.divisions and not self.is_event()

    def is_event(self) -> bool:
        """Check if this grouping has any events"""
        return self.event is not None

    def is_leaf(self) -> bool:
        """Check that this grouping is neither event nor structural"""
        return self.is_event() or self.is_open()

    def is_flat(self) -> bool:
        """Check if this tree has only leafs"""
        is_flat = True
        for child in self.divisions.values():
            if not child.is_leaf():
                is_flat = False
                break
        return is_flat

    def set_size(self, size: int, noclobber: bool = False):
        """Resize a grouping if it doesn't have any events. Will clobber existing subgroupings."""
        if self.is_event():
            raise BadStateError()

        if not noclobber:
            self.divisions = {}
        else:
            # TODO: Maybe think about this. might be able to be faster
            to_delete = set()
            for k in self.divisions:
                if k >= size:
                    to_delete.add(k)
            for k in to_delete:
                del self.divisions[k]

        self.size = size

    def resize(self, new_size: int):
        """Resize even if it has events, won't clobber data, but precision *may* be lost."""
        if self.is_event():
            raise BadStateError()

        new_divisions = {}
        factor = new_size / self.size

        for current_index, value in self.divisions.items():
            new_index = int(current_index * factor)
            new_divisions[new_index] = value

        self.divisions = new_divisions
        self.size = new_size

    def reduce(self, target_size=1):
        """
            Reduce a flat list of event groupings into smaller divisions
            while keeping the correct ratios.
            (eg midi events to musical notation)
        """
        if self.is_leaf():
            return

        if not self.is_flat():
            self.flatten()

        # Get the active indeces on the current level
        indeces = []
        for i, child_node in self.divisions.items():
            indeces.append((i, child_node))
        indeces.sort()

        # Use a temporary OpusTree to build the reduced version
        place_holder = self.copy()
        stack = [(target_size, indeces, self.size, place_holder)]
        while stack:
            denominator, indeces, previous_size, tree = stack.pop(0)
            current_size = previous_size // denominator

            # Create separate lists to represent the new equal trees
            split_indeces = []
            for _ in range(denominator):
                split_indeces.append([])
            tree.set_size(denominator)

            # move the indeces into their new lists
            for i, subtree in indeces:
                split_index = i // current_size
                split_indeces[split_index].append((i % current_size, subtree.copy()))

            for i in range(denominator):
                working_indeces = split_indeces[i]
                if not working_indeces:
                    continue

                working_tree = tree[i]

                # Get the most reduced version of each index
                minimum_divs = []
                for index, subtree in working_indeces:
                    most_reduced = int(current_size / math.gcd(current_size, index))
                    # mod the indeces to match their new relative positions
                    if most_reduced > 1:
                        minimum_divs.append(most_reduced)

                minimum_divs = list(set(minimum_divs))
                minimum_divs.sort()
                if minimum_divs:
                    stack.append((
                        minimum_divs[0],
                        working_indeces,
                        current_size,
                        working_tree
                    ))
                else: # Leaf
                    _, event_tree = working_indeces.pop(0)
                    if event_tree.is_event():
                        working_tree.set_event(event_tree.get_event(), merge=True)

        self.set_size(len(place_holder))
        for i, tree in place_holder.divisions.items():
            self[i] = tree

    # Attempt at speeding things up drastically slowed things down
    #def flatten(self):
    #    mapped_events = self.get_events_mapped()
    #    sizes = set()
    #    numerated = []
    #    for path, event in mapped_events:
    #        # Find a common denominator
    #        denominator = 1
    #        for i, (_x, size) in enumerate(path[::-1]):
    #            denominator *= int(math.pow(size, i + 1))

    #        numerator = 0
    #        numerator_factor = 1
    #        for x, size in path:
    #            numerator_factor *= size
    #            numerator += (x * denominator) // numerator_factor

    #        gcd = math.gcd(numerator, denominator)
    #        numerated.append((numerator, denominator, event))
    #        sizes.add(denominator)

    #    new_size = math.lcm(*list(sizes))
    #    self.set_size(new_size)

    #    for numerator, denominator, event in numerated:
    #        position = numerator * new_size // denominator
    #        self[position].add_event(event)


    def flatten(self):
        """Merge all subtrees into single level, preserving ratios"""
        # TODO: This gets pretty slow when 3+ deep
        # Tried a different method, added 10 seconds.
        # May need to do this in rust.
        sizes = []
        subtree_backup = []
        # First, recursively merge sub-subtrees into subtrees
        for i, child in self.divisions.items():
            if not child.is_leaf():
                if not child.is_flat():
                    child.flatten()
                sizes.append(len(child))

            subtree_backup.append((i, child))

        new_chunk_size = math.lcm(*sizes)
        new_size = new_chunk_size * len(self)

        self.set_size(new_size)
        for i, child in subtree_backup:
            offset = i * new_chunk_size
            if not child.is_leaf():
                for j, grandchild in enumerate(list(child)):
                    if grandchild.is_event():
                        fine_offset = int(j * new_chunk_size / len(child))
                        self[offset + fine_offset] = grandchild
            else:
                self[offset] = child



    def set_event(self, event: T, merge: bool = False) -> None:
        if merge and self.event is not None:
            self.event += event
        else:
            self.event = event


    def unset_event(self) -> None:
        self.event = None

    def get_event(self) -> Optional[T]:
        if self.is_event():
            return self.event
        else:
            return None

    def get_depth(self):
        """Find how many parents/supertrees this tree has """
        depth = 0
        working_tree = self
        while working_tree is not None:
            depth += 1
            working_tree = working_tree.parent
        return depth

    def __list__(self):
        output = []
        for i in range(self.size):
            grouping = self[i]
            output.append(grouping)
        return output

    def copy(self):
        new_tree = self.__class__()
        new_tree.size = self.size
        #new_tree.parent = self.parent

        for i, tree in self.divisions.items():
            new_tree.divisions[i] = tree.copy()
            new_tree.divisions[i].parent = new_tree

        new_tree.event = self.event

        return new_tree

    #TODO: Think of better name
    def clear_singles(self):
        """
            Shortens branches on the tree where an only-child-node has more than 1 child
            (ie X->1->Y becomes X->Y)
        """
        stack = list(self)
        while stack:
            working_tree = stack.pop(0)
            if not working_tree.is_leaf():
                if len(working_tree) == 1:
                    subtree = working_tree[0]
                    if not subtree.is_leaf():
                        working_tree.replace_with(subtree)
                        stack.append(subtree)
                else:
                    for child in working_tree:
                        stack.append(child)

    def replace_with(self, new_tree):
        new_tree.parent = self.parent
        if self.parent is not None:
            for i, child in enumerate(self.parent):
                if child != self:
                    continue
                self.parent[i] = new_tree
                break

    def pop(self, index=-1):
        if index == -1:
            index = self.size - 1
        output = self.divisions[index]
        new_divisions = {}
        for i, d in self.divisions.items():
            if i < index:
                new_divisions[i] = d
            elif i > index:
                new_divisions[i - 1] = d
        self.divisions = new_divisions
        self.size -= 1
        return output


def get_prime_factors(n):
    primes = []
    for i in range(2, n // 2):
        is_prime = True
        for p in primes:
            if i % p == 0:
                is_prime = False
                break
        if is_prime:
            primes.append(i)

    # No primes found, n must be prime
    if not primes:
        primes = [n]

    factors = []
    for p in primes:
        if p > n / 2:
            break
        if n % p == 0:
            factors.append(p)

    return factors
