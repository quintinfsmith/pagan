#import unittest
#import time
#import os
#import math
#import apres
#from apres import MIDIController
#
#from src.opusmanager.structures import OpusTree
#
#class OpusTreeTest(unittest.TestCase):
#    def setUp(self):
#        pass
#
#    def tearDown(self):
#        pass
#
#    def test_reduce(self):
#        tree = OpusTree()
#        tree.set_size(420)
#        tree[0].set_event((0,0,0,0))
#        tree[105].set_event((0,1,0,0)) # /4 [1]
#        tree[315].set_event((0,3,0,0)) # /4 [3]
#        tree[35].set_event((0,35,0,0)) # /4 [0] /3 [1]
#        tree[70].set_event((0,70,0,0)) # /4 [0] /3 [2]
#        tree[20].set_event((0,20,0,0)) # /4 [0] /3 [0] /7 [5]
#
#        tree.reduce(4)
#        assert len(tree) == 4
#        assert tree[1].get_event()[1] == 1
#        assert tree[3].get_event()[1] == 3
#
#        assert tree[0][0][0].get_event()[1] == 0
#        assert tree[0][0][4].get_event()[1] == 20
#        assert tree[0][1].get_event()[1] == 35
#        assert tree[0][2].get_event()[1] == 70
#
#    def test_merge(self):
#        pairs = [
#            (OpusTree(), [4,3,5, 13, 11, 35]),
#            (OpusTree(), [7,11, 2])
#        ]
#        numerator = 0
#        for i, (toptree, sizes) in enumerate(pairs):
#            lcm = 1
#            for s in sizes:
#                lcm *= s
#
#            stack = [(0, 0, lcm, toptree)]
#            while stack:
#                depth, xoffset_total, parent_size, tree = stack.pop()
#                if depth < len(sizes):
#                    new_size = sizes[depth]
#                    tree.set_size(new_size)
#                    chunk_size = parent_size / sizes[depth]
#                    for j in range(new_size):
#                        current_x_offset = j * chunk_size
#                        stack.append((
#                            depth + 1,
#                            xoffset_total + current_x_offset,
#                            chunk_size,
#                            tree[j]
#                        ))
#                else:
#                    tree.set_event((i, numerator, xoffset_total, lcm))
#                    numerator += 1
#
#        ga = pairs[0][0]
#        ga.merge(pairs[1][0])
#        events = ga.get_events_mapped()
#
#        for path, event in events:
#            offset = 0
#            working_tree = ga
#            for index, size in path:
#                working_tree = working_tree[index]
#            assert event == working_tree.get_event()
#
#        ga.flatten()
#        for path, event in events:
#            sizes = []
#            offset = 0
#            reducer = 1
#            for (i, size) in path:
#                reducer *= size
#                # Keeping the offset large here to keep rounding errors at bay
#                offset += (len(ga) * i / reducer)
#
#            assert offset / len(ga) == event[2] / event[3]
#            assert offset % 1 == 0
#            assert event == ga[int(offset)].get_event()
#
#
#    def test_flatten(self):
#        toptree = OpusTree()
#        sizes = [3,4,11]
#        numerator = 0
#        stack = [(0, 0, toptree)]
#        lcm = math.lcm(*sizes)
#        while stack:
#            depth, xoffset_total, tree = stack.pop(0)
#            if depth < len(sizes):
#                new_size = sizes[depth]
#                tree.set_size(new_size)
#                for j in range(new_size):
#                    current_x_offset = j * (lcm / sizes[depth])
#                    stack.append((depth + 1, xoffset_total + current_x_offset, tree[j]))
#            else:
#                tree.set_event((0, numerator, xoffset_total, lcm))
#                numerator += 1
#
#        toptree.flatten()
#
#        assert len(toptree) == lcm
#
#    def test_get_events_mapped(self):
#        tree = OpusTree()
#        tree.set_size(4)
#        tree[0].set_size(3)
#        tree[0][0].set_size(7)
#
#        tree[0][0][0].set_event((0,0,0,0))
#        tree[0][0][4].set_event((0,1,0,0))
#        tree[0][1].set_event((0,2,0,0))
#        tree[0][2].set_event((0,3,0,0))
#        tree[1].set_event((0,4,0,0))
#        tree[3].set_event((0,5,0,0))
#        mapped_events = tree.get_events_mapped()
#        assert len(mapped_events) == 6
#
#    def _test_split_func(self, value, events):
#        return value[1]
#
#    def test_split(self):
#        tree = OpusTree()
#        tree.set_size(4)
#        tree[0].set_size(3)
#        tree[0][0].set_size(7)
#
#        tree[0][0][0].set_event((0,0,0,0))
#        tree[0][0][4].set_event((0,1,0,0))
#        tree[0][1].set_event((0,2,0,0))
#        tree[0][2].set_event((0,3,0,0))
#        tree[1].set_event((0,4,0,0))
#        tree[3].set_event((0,5,0,0))
#
#        splits = tree.split(self._test_split_func)
#
#
#
#
#
