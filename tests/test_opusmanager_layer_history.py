import unittest
from src.opusmanager.layer_history import HistoryLayer as OpusManager
from src.opusmanager.errors import InvalidPosition

class HistoryLayerTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_undo_empty(self):
        manager = OpusManager.new()
        failed = False
        try:
            manager.apply_undo()
        except Exception:
            failed = True
        assert not failed, "apply_undo() on empty stack didn't fail quietly"

    def test_set_percussion_instrument(self):
        manager = OpusManager.new()
        manager.add_channel(9)
        original_instrument = manager.DEFAULT_PERCUSSION
        manager.set_percussion_instrument(0, original_instrument + 1)

        manager.apply_undo()

        assert manager.percussion_map.get(0, original_instrument + 1) == original_instrument, "Failed to undo set_percussion_instrument()"

    def test_overwrite_beat(self):
        manager = OpusManager.new()
        from_beat = (0, 0, 0)
        to_beat = (0, 0, 1)
        manager.set_event(from_beat, [0], 35)
        manager.overwrite_beat(from_beat, to_beat)

        manager.apply_undo()

        from_grouping = manager.get_beat_grouping(from_beat)
        to_grouping = manager.get_beat_grouping(to_beat)

        assert not from_grouping.matches(to_grouping), "Failed to undo overwrite_beat()"

    def test_remove(self):
        manager = OpusManager.new()

        #standard remove()
        manager.insert_after((0,0,0), [0])
        manager.remove((0,0,0), [1])
        manager.apply_undo()
        assert len(manager.get_beat_grouping((0,0,0))) == 2, "Failed to undo remove()"

        # remove event
        manager.add_channel(1)
        manager.split_grouping((1,0,0), [0], 2)
        manager.set_event((1,0,0), [0,0], 35)
        manager.remove((1,0,0), [0, 0])
        manager.apply_undo()
        grouping = manager.get_grouping((1,0,0), [0,0])
        assert list(grouping.get_events())[0].note, "Failed to undo remove() on an event"

        # Remove Percussion event
        manager.add_channel(9)
        manager.split_grouping((9,0,0), [0], 2)
        manager.set_percussion_event((9,0,0), [0,0])
        manager.remove((9,0,0), [0, 0])
        manager.apply_undo()
        grouping = manager.get_grouping((9,0,0), [0, 0])
        assert list(grouping.get_events())[0].note, "Failed to undo remove() on a percussion event"

        # *attempt* to undo a remove on an empty beat
        original = manager.get_grouping((0,0,1), [0])
        manager.remove((0,0,1), [0])
        manager.apply_undo()

        assert original == manager.get_grouping((0,0,1), [0]), "Something changed when it shouldn't have in remove() undo"

    def test_swap_channels(self):
        manager = OpusManager.new()
        manager.add_channel(1)
        line_a = manager.channel_groupings[0][0]
        line_b = manager.channel_groupings[1][0]

        manager.swap_channels(0, 1)
        manager.apply_undo()
        assert line_a == manager.channel_groupings[0][0] and line_b == manager.channel_groupings[1][0], "Failed to undo swap_channels()"

    def test_new_line(self):
        manager = OpusManager.new()
        manager.new_line(0)
        manager.apply_undo()

        assert len(manager.channel_groupings[0]) == 1, "Failed to undo new_line()"

    def test_remove_line(self):
        manager = OpusManager.new()
        manager.new_line(0)
        manager.remove_line(0, 1)
        manager.apply_undo()

        assert len(manager.channel_groupings[0]) == 2, "Failed to undo remove_line()"

    def test_insert_after(self):
        manager = OpusManager.new()

        manager.insert_after((0,0,0), [0])
        manager.apply_undo()
        assert len(manager.get_beat_grouping((0,0,0))) == 1, "Failed to undo insert_after()"

    def test_split_grouping(self):
        manager = OpusManager.new()

        manager.split_grouping((0,0,0), [0], 5)
        manager.apply_undo()
        assert len(manager.get_grouping((0,0,0), [0])) == 1, "Failed to undo split_grouping()"


    def test_insert_beat(self):
        manager = OpusManager.new()
        original_length = manager.opus_beat_count
        manager.insert_beat(0)
        manager.apply_undo()

        assert original_length == manager.opus_beat_count, "Failed to undo insert_beat()"

    def test_remove_beat(self):
        manager = OpusManager.new()
        original_length = manager.opus_beat_count
        beat_checks = []
        for i, beat in enumerate(manager.channel_groupings[0]):
            beat_checks.append(beat)

        manager.remove_beat(1)
        manager.apply_undo()
        assert manager.opus_beat_count == original_length, "Failed to undo remove_beat()"


        undone_incorrectly = False
        for i, beat in enumerate(beat_checks):
            # Don't need to check the beat that was removed
            if i == 1:
                continue
            undone_incorrectly |= beat != manager.channel_groupings[0][i]

        assert not undone_incorrectly, "remove_beat() undone, but not correctly"

    def test_set_event(self):
        manager = OpusManager.new()
        manager.set_event((0,0,0), [0], 35)
        manager.apply_undo()

        grouping = manager.get_grouping((0,0,0), [0])
        assert grouping.is_open(), "Failed to undo set_event() on open grouping"

        manager.set_event((0,0,0), [0], 35)
        manager.set_event((0,0,0), [0], 36)
        manager.apply_undo()
        grouping = manager.get_grouping((0,0,0), [0])
        assert list(grouping.get_events())[0].note == 35, "Failed to undo set_event() on event grouping"

    def test_unset(self):
        manager = OpusManager.new()

        manager.set_event((0,0,0), [0], 35)

        manager.unset((0,0,0), [0])
        manager.apply_undo()

        grouping = manager.get_grouping((0,0,0), [0])
        assert list(grouping.get_events())[0].note == 35, "Failed to undo unset()"


