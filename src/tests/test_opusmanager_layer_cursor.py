import unittest
from src.opusmanager.layer_cursor import CursorLayer as OpusManager
from src.opusmanager.miditree import MIDITreeEvent

class HistoryLayerTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_move(self):
        manager = OpusManager.new()
        manager.add_channel(1)
        manager.add_channel(9)
        manager.split_tree((1,0,0), [], 5)

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
        tree = manager.get_tree_at_cursor()
        assert tree.is_event(), "Failed to set percussion event at cursor"

    def test_lines_at_cursor(self):
        manager = OpusManager.new()
        original_lines = len(manager.channel_lines[0])

        manager.new_line_at_cursor()
        assert original_lines + 1 == len(manager.channel_lines[0])

        manager.cursor_down()
        manager.remove_line_at_cursor()
        assert original_lines == len(manager.channel_lines[0])

    def test_beats_at_cursor(self):
        manager = OpusManager.new()
        original_beats = list(manager.channel_lines[0][0])
        manager.insert_beat_at_cursor()
        new_beats = list(manager.channel_lines[0][0])
        assert original_beats[0] == new_beats[0] and original_beats[1] == new_beats[2], "insert_beat_at_cursor() inserted at wrong position"

        manager.cursor_right()
        manager.remove_beat_at_cursor()

        assert manager.opus_beat_count == len(original_beats)
        assert len(manager.channel_lines[0][0]) == len(original_beats)
        assert original_beats == list(manager.channel_lines[0][0]), "remove_beat_at_cursor() removed wrong beat"


    def test_split_tree_at_cursor(self):
        manager = OpusManager.new()
        manager.split_tree_at_cursor()
        assert len(manager.get_beat_tree((0,0,0))) == 2, "Failed to split beat at cursor"
        manager.split_tree_at_cursor()
        assert len(manager.get_tree((0,0,0), [0])) == 2, "Failed to split regular tree at cursor"

    def test_jump_to_beat(self):
        manager = OpusManager.new()
        manager.jump_to_beat(3)
        assert manager.cursor.get_triplet() == (0,0,3), "Failed to jump to beat"

    def test_dec_increment_event_at_cursor(self):
        manager = OpusManager.new()
        manager.set_event((0,0,0),[0], MIDITreeEvent(24))
        manager.increment_event_at_cursor()
        tree = manager.get_tree_at_cursor()
        assert tree.get_event().note == 25, "Failed to increment beat at cursor"

        manager.decrement_event_at_cursor()
        assert tree.get_event().note == 24, "Failed to decrement beat at cursor"

        manager.set_event((0,0,0),[0], MIDITreeEvent(11, relative=True))
        manager.increment_event_at_cursor()
        assert tree.get_event().note == 12,"Failed to increment relative event at cursor (under 12)"

        manager.increment_event_at_cursor()
        assert tree.get_event().note == 24, "Failed to increment relative event at cursor (>= 12)"

        manager.decrement_event_at_cursor()
        assert tree.get_event().note == 12,"Failed to decrement relative event at cursor (>= 12)"

        manager.decrement_event_at_cursor()
        assert tree.get_event().note == 11, "Failed to increment relative event at cursor (<= 12)"

        manager.cursor_right()
        tree = manager.get_tree_at_cursor()
        manager.decrement_event_at_cursor()
        assert tree.is_open(), "Should've failed to decrement non-event"

        manager.increment_event_at_cursor()
        assert tree.is_open(), "Should've failed to increment non-event"
