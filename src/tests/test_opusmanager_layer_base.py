import apres
import unittest
from src.opusmanager.layer_base import OpusManagerBase as OpusManager
from src.opusmanager.miditree import MIDITreeEvent
from src.opusmanager.errors import InvalidPosition

class OpusManagerTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_new(self):
        manager = OpusManager.new()
        assert manager.opus_beat_count != 0, "new() didn't create any empty beats"

    def test_insert_after(self):
        manager = OpusManager.new()

        beat_key = (0,0,0)
        beat_tree = manager.get_beat_tree(beat_key)
        tree = manager.get_tree(beat_key, [0])
        initial_length = len(beat_tree)
        manager.insert_after(beat_key, [0])
        assert len(beat_tree) == initial_length + 1, "Didn't insert after"
        try:
            manager.insert_after(beat_key, [])
            assert False, "Shouldn't be able to insert after a beat"
        except InvalidPosition:
            assert True
        except Exception:
            assert False, "Incorrect exception type raised"

    def test_remove(self):
        # Setup an opus
        manager = OpusManager.new()
        beat_key = (0,0,0)
        beat_tree = manager.get_beat_tree(beat_key)
        # Insert an empty tree in the first beat
        manager.insert_after(beat_key, [0])

        # Then remove that tree
        manager.remove(beat_key, [1])
        assert len(beat_tree) == 1, "Remove Failed"

        # *Attempt* to remove last tree in beat
        manager.remove(beat_key, [0])
        assert len(beat_tree) == 1, "Removed a beat's last child"

        # Check that the siblings get adjusted
        for i in range(3):
            manager.insert_after(beat_key, [0])
        tree = manager.get_tree(beat_key, [3])
        manager.remove(beat_key, [2])
        assert tree == manager.get_tree(beat_key, [2]), "sibling didn't get adjusted"


    def test_split_tree(self):
        # Setup an opus
        manager = OpusManager.new()
        split_count = 5
        beat_key = (0,0,0)

        # Split a beat
        manager.split_tree(beat_key, [], split_count)
        beat_tree = manager.get_beat_tree(beat_key)
        assert len(beat_tree) == split_count, "Failed to split the beat"

        # Split an open leaf
        manager.split_tree(beat_key, [split_count - 1], split_count)
        beat_tree = manager.get_beat_tree(beat_key)
        assert len(beat_tree[split_count - 1]) == split_count, "Failed to split a tree"

        # Split an event
        position = [split_count - 1, 0]
        manager.set_event(beat_key, position, MIDITreeEvent(30))
        manager.split_tree(beat_key, position, split_count)
        subtree = manager.get_tree(beat_key, position)
        assert len(subtree) == split_count, "Failed to split event"

    def test_set_event(self):
        # Setup an opus
        manager = OpusManager.new()
        # Add percussion line
        manager.add_channel(9)
        beat_key = (0,0,0)
        event_value = 30
        beat_tree = manager.get_beat_tree(beat_key)

        # Set absolute event
        manager.set_event(beat_key, [0], MIDITreeEvent(event_value))
        tree = manager.get_tree(beat_key,[0])
        assert tree.is_event(), "Failed to set event"
        assert tree.get_event().note == event_value, "Didn't set correct value"

        # Re-set event
        manager.set_event(beat_key, [0], MIDITreeEvent(event_value + 4))
        tree = manager.get_tree(beat_key, [0])
        assert tree.get_event().note == event_value + 4, "Failed to re-set event"

        # Overwrite existing structural node
        manager.split_tree(beat_key, [0], 2)
        manager.set_event(beat_key, [0], MIDITreeEvent(event_value - 4))
        tree = manager.get_tree(beat_key, [0])
        assert tree.is_event(), "Failed to overwrite existing structure"

        p_beat_key = (9,0,0)
        # *Attempt* to set event on percussion channel
        try:
            manager.set_event(p_beat_key, [0], MIDITreeEvent(event_value))
            assert False, "Successfully set a normal event on percussion channel"
        except IndexError:
            assert True
        except Exception:
            assert False, "Failed to set normal event on percussion channel *BUT* didn't fail correctly"


    def test_set_percussion_event(self):
        # Setup an opus
        manager = OpusManager.new()
        # Add percussion line
        manager.add_channel(9)
        beat_key = (9,0,0)
        manager.set_percussion_event(beat_key, [0])
        tree = manager.get_tree(beat_key, [0])
        assert tree.is_event(), "Failed to set percussion event"


        # Overwrite existing structural node
        manager.split_tree(beat_key, [0], 2)
        manager.set_percussion_event(beat_key, [0])
        tree = manager.get_tree(beat_key, [0])
        assert tree.is_event(), "Failed to overwrite existing structure"


        # *Attempt* to set non-percussion event
        beat_key = (0,0,0)
        try:
            manager.set_percussion_event(beat_key, [0])
            assert False, "Successfully set percussion event outside of channel 9"
        except IndexError:
            assert True
        except Exception:
            assert False, "Failed to set percussion event outside channel 9 but didn't fail correctly"

    def test_set_percussion_instrument(self):
        # Setup an opus
        manager = OpusManager.new()
        # Add percussion line
        manager.add_channel(9)
        beat_key = (9,0,0)
        manager.set_percussion_event(beat_key, [0])
        tree = manager.get_tree(beat_key, [0])
        initial_instrument = tree.get_event().note
        manager.set_percussion_instrument(0, initial_instrument + 5)
        assert tree.get_event().note == initial_instrument + 5, "Failed to change existing event"
        assert manager.percussion_map[0] == initial_instrument + 5, "Failed to remap instrument"

    def test_unset(self):
        # Setup an opus
        manager = OpusManager.new()
        # Add percussion line
        beat_key = (0,0,0)
        event_value = 30
        beat_tree = manager.get_beat_tree(beat_key)

        manager.set_event(beat_key, [0], MIDITreeEvent(event_value))
        manager.unset(beat_key, [0])
        tree = manager.get_tree(beat_key,[0])
        assert tree.is_open(), "Failed to unset event"

        # Make a little tree
        for i in range(4):
            manager.split_tree(beat_key, [0] * i, 5)

        # and unset it
        manager.unset(beat_key, [0])
        assert manager.get_tree(beat_key, [0]).is_open(), "Failed to unset a structural tree"


    def test_insert_beat(self):
        # Setup an opus
        manager = OpusManager.new()
        initial_length = manager.opus_beat_count
        index = 2

        # Set an event to check if it has moved
        manager.set_event((0,0,index), [0], MIDITreeEvent(40))

        manager.insert_beat(index - 1)
        assert manager.opus_beat_count == initial_length + 1, "Failed to increase length"
        assert manager.get_tree((0,0,index + 1), [0]).is_event(), "Failed to move beats over"

    def test_remove_beat(self):
        # Setup an opus
        manager = OpusManager.new()
        initial_length = manager.opus_beat_count
        index = 2

        # Set an event to check if it has moved
        manager.set_event((0,0,index), [0], MIDITreeEvent(40))

        manager.remove_beat(index - 1)
        assert manager.opus_beat_count == initial_length - 1, "Failed to decrease length"
        assert manager.get_tree((0,0,index - 1), [0]).is_event(), "Failed to move beats over"

    def test_change_line_channel(self):
        manager = OpusManager.new()
        manager.add_channel(1)
        line = manager.channel_lines[1][0]
        manager.change_line_channel(1, 0, 0)

        assert line == manager.channel_lines[0][1], "Failed to move line to new channel"

    def test_swap_channels(self):
        index_a = 0
        index_b = 1
        # Set up an opus
        manager = OpusManager.new()

        manager.new_line(0)
        # Add a channel to be  swapped with 0
        manager.add_channel(1)
        manager.new_line(1)
        channel_a_line_a = manager.channel_lines[index_a][0]
        channel_a_line_b = manager.channel_lines[index_a][1]
        channel_b_line_a = manager.channel_lines[index_b][0]
        channel_b_line_b = manager.channel_lines[index_b][1]

        manager.swap_channels(index_a, index_b)
        assert channel_a_line_a == manager.channel_lines[index_b][0] \
            and channel_b_line_a == manager.channel_lines[index_a][0] \
            and channel_a_line_b == manager.channel_lines[index_b][1] \
            and channel_b_line_b == manager.channel_lines[index_a][1], "Didn't swap lines in channels correctly"

    def test_remove_channel(self):
        # Set up an opus
        manager = OpusManager.new()

        manager.new_line(1)

        manager.remove_channel(1)
        assert not manager.channel_lines[1], "Failed to removed channel 1"

    def test_remove_line(self):
        # Set up an opus
        manager = OpusManager.new()
        manager.add_channel(1)
        for i in range(16):
            manager.new_line(1)

        index = 12
        line = manager.channel_lines[1][index]

        manager.remove_line(1, index - 1)
        # Checking that the line *before* the line to check is removed
        assert manager.channel_lines[1][index - 1] == line, "Failed to remove line in channel 1"

        current_length = len(manager.channel_lines[1])

        line_checks = []
        for l in manager.channel_lines[1]:
            line_checks.append(l)

        manager.remove_line(1)

        assert len(manager.channel_lines[1]) == current_length - 1, "Failed to remove line given no index"
        removed_incorrect_line = False
        for l, line in enumerate(line_checks[0:-1]):
            removed_incorrect_line |= line != manager.channel_lines[1][l]
        assert not removed_incorrect_line, "Removed wrong line (should've assumed -1)"

    def test_add_line(self):
        manager = OpusManager.new()
        line = manager.channel_lines[0][0]
        manager.new_line(0, 0)
        assert line == manager.channel_lines[0][1], "Failed to insert line at index"

    def test_move_line(self):
        manager = OpusManager.new()
        line = manager.channel_lines[0][0]
        for i in range(12):
            manager.new_line(0)
        manager.move_line(channel=0, old_index=0, new_index=2)
        assert line == manager.channel_lines[0][2]

        manager.move_line(channel=0, old_index=2, new_index=-1)
        assert line == manager.channel_lines[0][-1], "Failed to move line to Nth-from-last"


        manager.move_line(channel=0, old_index=-1, new_index=0)
        assert line == manager.channel_lines[0][0], "Failed to move line by Nth-from-last index"

        manager.move_line(0, 0, 0)
        assert line == manager.channel_lines[0][0], "Something went wrong when moving a line to the same position"

        # *Attempt* bad indices
        try:
            manager.move_line(channel=0, old_index=100, new_index=1)
            assert False, "Should not be able to move out of bounds"
        except IndexError:
            assert True
        except Exception:
            assert False, "Should have raised index error"

        # *Attempt* bad indices
        try:
            manager.move_line(channel=0, old_index=0, new_index=-100)
            assert False, "Should not be able to move out of bounds"
        except IndexError:
            assert True
        except Exception:
            assert False, "Should have raised index error"

    def test_overwrite_beat(self):
        manager = OpusManager.new()
        beat_a = (0,0,0)
        beat_b = (0,0,1)

        # Set up beat_a
        manager.split_tree(beat_a, [0], 3)
        manager.split_tree(beat_a, [1], 2)
        manager.set_event(beat_a, [1,0], MIDITreeEvent(24))
        manager.set_event(beat_a, [0], MIDITreeEvent(25))

        manager.overwrite_beat(beat_b, beat_a)

        assert manager.get_beat_tree(beat_a).matches(manager.get_beat_tree(beat_b)), "Failed to overwrite beat"

