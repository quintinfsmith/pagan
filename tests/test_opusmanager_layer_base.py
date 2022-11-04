import apres
import unittest
from src.opusmanager import OpusManager
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
        beat_grouping = manager.get_beat_grouping(beat_key)
        grouping = manager.get_grouping(beat_key, [0])
        initial_length = len(beat_grouping)
        manager.insert_after(beat_key, [0])
        assert len(beat_grouping) == initial_length + 1, "Didn't insert after"
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
        beat_grouping = manager.get_beat_grouping(beat_key)
        # Insert an empty grouping in the first beat
        manager.insert_after(beat_key, [0])

        # Then remove that grouping
        manager.remove(beat_key, [1])
        assert len(beat_grouping) == 1, "Remove Failed"

        # *Attempt* to remove last grouping in beat
        manager.remove(beat_key, [0])
        assert len(beat_grouping) == 1, "Removed a beat's last child"

        # *Attempt* to remove a beat with remove()
        try:
            manager.remove(beat_key, [])
        except InvalidPosition:
            assert True
        except Exception:
            assert False, "Incorrect exception type raised"
        assert len(beat_grouping.parent) == manager.opus_beat_count, "Removed a beat with remove()"

    def test_split_grouping(self):
        # Setup an opus
        manager = OpusManager.new()
        split_count = 5
        beat_key = (0,0,0)
        beat_grouping = manager.get_beat_grouping(beat_key)

        # Split a beat
        manager.split_grouping(beat_key, [], split_count)
        assert len(beat_grouping) == split_count, "Failed to split the beat"

        # Split an open leaf
        manager.split_grouping(beat_key, [split_count - 1], split_count)
        assert len(beat_grouping[split_count - 1]) == split_count, "Failed to split a grouping"

        # Split an event
        position = [split_count - 1, 0]
        manager.set_event(beat_key, position, 30)
        manager.split_grouping(beat_key, position, split_count)
        subgrouping = manager.get_grouping(beat_key, position)
        assert len(subgrouping) == split_count, "Failed to split event"

    def test_set_event(self):
        # Setup an opus
        manager = OpusManager.new()
        # Add percussion line
        manager.add_channel(9)
        beat_key = (0,0,0)
        event_value = 30
        beat_grouping = manager.get_beat_grouping(beat_key)

        # Set absolute event
        manager.set_event(beat_key, [0], event_value)
        grouping = manager.get_grouping(beat_key,[0])
        assert grouping.is_event(), "Failed to set event"
        assert list(grouping.get_events())[0].note == event_value, "Didn't set correct value"

        # Re-set event
        manager.set_event(beat_key, [0], event_value + 4)
        assert grouping.is_event(), "Failed to re-set event"

        # *Attempt* To set event at beat
        try:
            manager.set_event(beat_key, [], event_value)
            assert False, "Successfully set beat as an event"
        except InvalidPosition:
            assert True
        except Exception:
            assert False, "Failed to set beat as event, but failed incorrectly"

        p_beat_key = (9,0,0)
        # *Attempt* to set event on percussion channel
        try:
            manager.set_event(p_beat_key, [0], event_value)
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
        grouping = manager.get_grouping(beat_key, [0])
        assert grouping.is_event(), "Failed to set percussion event"

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
        grouping = manager.get_grouping(beat_key, [0])
        initial_instrument = list(grouping.get_events())[0].note
        manager.set_percussion_instrument(0, initial_instrument + 5)
        assert list(grouping.get_events())[0].note == initial_instrument + 5, "Failed to change existing event"
        assert manager.percussion_map[0] == initial_instrument + 5, "Failed to remap instrument"

    def test_unset(self):
        # Setup an opus
        manager = OpusManager.new()
        # Add percussion line
        beat_key = (0,0,0)
        event_value = 30
        beat_grouping = manager.get_beat_grouping(beat_key)

        manager.set_event(beat_key, [0], event_value)
        grouping = manager.get_grouping(beat_key,[0])
        manager.unset(beat_key, [0])
        assert grouping.is_open(), "Failed to unset event"

        # Make a little tree
        for i in range(4):
            manager.split_grouping(beat_key, [0] * i, 5)

        # and unset it
        manager.unset(beat_key, [0])
        assert manager.get_grouping(beat_key, [0]).is_open(), "Failed to unset a structural grouping"

        # *Attempt* to unset beat
        try:
            manager.unset(beat_key, [])
            assert False, "Successfully unset a beat"
        except InvalidPosition:
            assert True
        except Exception:
            assert False, "Incorrect exception in failing to unset beat"

    def test_insert_beat(self):
        # Setup an opus
        manager = OpusManager.new()
        initial_length = manager.opus_beat_count
        index = 2

        # Set an event to check if it has moved
        manager.set_event((0,0,index), [0], 40)

        manager.insert_beat(index - 1)
        assert manager.opus_beat_count == initial_length + 1, "Failed to increase length"
        assert manager.get_grouping((0,0,index + 1), [0]).is_event(), "Failed to move beats over"

    def test_remove_beat(self):
        # Setup an opus
        manager = OpusManager.new()
        initial_length = manager.opus_beat_count
        index = 2

        # Set an event to check if it has moved
        manager.set_event((0,0,index), [0], 40)

        manager.remove_beat(index - 1)
        assert manager.opus_beat_count == initial_length - 1, "Failed to decrease length"
        assert manager.get_grouping((0,0,index - 1), [0]).is_event(), "Failed to move beats over"

