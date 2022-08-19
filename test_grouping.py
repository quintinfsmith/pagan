import unittest
import time
import os
import math
import apres
from apres import MIDIController

from structures import Grouping

class GroupingTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_reduce(self):
        grouping = Grouping()
        grouping.set_size(420)
        grouping[0].add_event((0,0,0,0))
        grouping[105].add_event((0,1,0,0)) # /4 [1]
        grouping[315].add_event((0,3,0,0)) # /4 [3]
        grouping[35].add_event((0,35,0,0)) # /4 [0] /3 [1]
        grouping[70].add_event((0,70,0,0)) # /4 [0] /3 [2]
        grouping[20].add_event((0,20,0,0)) # /4 [0] /3 [0] /7 [5]

        grouping.reduce(4)
        assert len(grouping) == 4
        assert grouping[1].get_events().pop()[1] == 1
        assert grouping[3].get_events().pop()[1] == 3

        assert grouping[0][0][0].get_events().pop()[1] == 0
        assert grouping[0][0][4].get_events().pop()[1] == 20
        assert grouping[0][1].get_events().pop()[1] == 35
        assert grouping[0][2].get_events().pop()[1] == 70

    def test_merge(self):
        pairs = [
            (Grouping(), [4,3,5]),
            (Grouping(), [7,11])
        ]
        numerator = 0
        for i, (topgrouping, sizes) in enumerate(pairs):
            lcm = math.lcm(*sizes)
            stack = [(0, 0, lcm, topgrouping)]
            while stack:
                depth, xoffset_total, parent_size, grouping = stack.pop()
                if depth < len(sizes):
                    new_size = sizes[depth]
                    grouping.set_size(new_size)
                    chunk_size = parent_size / sizes[depth]
                    for j in range(new_size):
                        current_x_offset = j * chunk_size
                        stack.append((
                            depth + 1,
                            xoffset_total + current_x_offset,
                            chunk_size,
                            grouping[j]
                        ))
                else:
                    grouping.add_event((i, numerator, xoffset_total, lcm))
                    numerator += 1

        ga = pairs[0][0]
        ga.merge(pairs[1][0])
        events = ga.get_events_mapped()

        for path, event in events:
            offset = 0
            working_grouping = ga
            for index, size in path:
                working_grouping = working_grouping[index]
            assert event in working_grouping.get_events()

        ga.flatten()
        for path, event in events:
            print(path, event, len(ga))
            sizes = []
            offset = 0
            reducer = 1
            for (i, size) in path:
                reducer *= size
                offset += (1 / reducer) * i

            assert offset == event[2] / event[3]
            assert event in ga[int(offset * len(ga))].get_events()


    def test_flatten(self):
        topgrouping = Grouping()
        sizes = [3,4,11]
        numerator = 0
        stack = [(0, 0, topgrouping)]
        lcm = math.lcm(*sizes)
        while stack:
            depth, xoffset_total, grouping = stack.pop(0)
            if depth < len(sizes):
                new_size = sizes[depth]
                grouping.set_size(new_size)
                for j in range(new_size):
                    current_x_offset = j * (lcm / sizes[depth])
                    stack.append((depth + 1, xoffset_total + current_x_offset, grouping[j]))
            else:
                grouping.add_event((0, numerator, xoffset_total, lcm))
                numerator += 1

        topgrouping.flatten()

        assert len(topgrouping) == lcm

    def test_get_events_mapped(self):
        grouping = Grouping()
        grouping.set_size(4)
        grouping[0].set_size(3)
        grouping[0][0].set_size(7)

        grouping[0][0][0].add_event((0,0,0,0))
        grouping[0][0][4].add_event((0,1,0,0))
        grouping[0][1].add_event((0,2,0,0))
        grouping[0][2].add_event((0,3,0,0))
        grouping[1].add_event((0,4,0,0))
        grouping[3].add_event((0,5,0,0))
        mapped_events = grouping.get_events_mapped()
        assert len(mapped_events) == 6

    def _test_split_func(self, value):
        return value[1]

    def test_split(self):
        grouping = Grouping()
        grouping.set_size(4)
        grouping[0].set_size(3)
        grouping[0][0].set_size(7)

        grouping[0][0][0].add_event((0,0,0,0))
        grouping[0][0][4].add_event((0,1,0,0))
        grouping[0][1].add_event((0,2,0,0))
        grouping[0][2].add_event((0,3,0,0))
        grouping[1].add_event((0,4,0,0))
        grouping[3].add_event((0,5,0,0))

        splits = grouping.split(self._test_split_func)





