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
        grouping_a = Grouping()
        grouping_a.set_size(4)
        grouping_b = Grouping()
        grouping_b.set_size(4)
        for i in range(4):
            grouping_b[i].add_event((0,i,0,0))

        assert len(grouping_a) == 4
        grouping_a.merge(grouping_b)
        assert len(grouping_a) == 4

    def test_flatten(self):
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

        grouping.flatten()

        assert len(grouping) == math.lcm(4,3,7)
        assert grouping[0].get_events().pop()[1] == 0
        assert grouping[4].get_events().pop()[1] == 1
        assert grouping[7].get_events().pop()[1] == 2
        assert grouping[14].get_events().pop()[1] == 3
        assert grouping[21].get_events().pop()[1] == 4
        assert grouping[63].get_events().pop()[1] == 5



