import unittest
from src.opusmanager.layer_cursor import CursorLayer as OpusManager

class HistoryLayerTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_move(self):
        manager = OpusManager.new()
        manager.add_channel(1)
        manager.add_channel(9)
        manager.split_grouping((1,0,0), [0], 5)

        cursor = manager.cursor
        assert cursor.to_list() == [0,0,0], "Cursor starting at bad position"

        manager.cursor_right()
        assert cursor.to_list() == [0,1,0], "Cursor didn't move right correctly"

        manager.cursor_left()
        assert cursor.to_list() == [0,0,0], "Cursor didn't move left correctly"

        manager.cursor_down()
        assert cursor.to_list() == [1,0,0], "Cursor didn't move down correctly"

        manager.cursor_right()
        assert cursor.to_list() == [1,0,1], "Cursor didn't move left within the beat"

        manager.cursor_up()
        assert cursor.to_list() == [0,0,0], "Cursor didn't move up correctly"

        # *Attempt* to move past boundaries
        manager.cursor_up()
        assert cursor.to_list() == [0,0,0], "Cursor moved below lower vertical limit"

        manager.cursor_left()
        assert cursor.to_list() == [0,0,0], "Cursor moved below lower horizontal limit"

        for i in range(manager.opus_beat_count - 1):
            manager.cursor_right()
        manager.cursor_right()
        assert cursor.to_list() == [0,manager.opus_beat_count - 1, 0], "Cursor moved above upper horizontal limit"

        for i in range(2):
            manager.cursor_down()
        manager.cursor_down()
        assert cursor.to_list() == [2,manager.opus_beat_count - 1, 0], "Cursor moved above upper vertical limit"

        cursor.set(1,1,0)
        manager.cursor_left()
        assert cursor.to_list() == [1,0,4], "cursor moved left, but didn't move to the last position in the beat"


    def test_set_percussion_event_at_cursor(self):
        manager = OpusManager.new()
        manager.add_channel(9)

        manager.cursor_down()
        manager.set_percussion_event_at_cursor()
        grouping = manager.get_grouping_at_cursor()
        assert grouping.is_event(), "Failed to set percussion event at cursor"

    def test_lines_at_cursor(self):
        manager = OpusManager.new()
        original_lines = len(manager.channel_groupings[0])

        manager.new_line_at_cursor()
        assert original_lines + 1 == len(manager.channel_groupings[0])

        manager.cursor_down()
        manager.remove_line_at_cursor()
        assert original_lines == len(manager.channel_groupings[0])

    def test_beats_at_cursor(self):
        manager = OpusManager.new()
        original_beats = list(manager.channel_groupings[0][0])
        manager.insert_beat_at_cursor()
        new_beats = list(manager.channel_groupings[0][0])
        assert original_beats[0] == new_beats[0] and original_beats[1] == new_beats[2], "insert_beat_at_cursor() inserted at wrong position"

        manager.cursor_right()
        manager.remove_beat_at_cursor()
        assert original_beats == list(manager.channel_groupings[0][0]), "remove_beat_at_cursor() removed wrong beat"


    def test_split_grouping_at_cursor(self):
        manager = OpusManager.new()
        manager.split_grouping_at_cursor()
        assert len(manager.get_beat_grouping((0,0,0))) == 2
        manager.split_grouping_at_cursor()
        assert len(manager.get_grouping((0,0,0), [0])) == 2

