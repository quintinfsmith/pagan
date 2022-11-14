package radixulous.app.opusmanager
import radixulous.app.structure.OpusTree

class OpusEvent { }
data class BeatKey {
    var channel: Int,
    var line_offset: Int,
    var beat: Int
}

open class OpusManagerBase {
    var channel_trees: Array<MutableList<OpusTree>> = Array(16, { _ -> mutableListOf() })
    var opus_beat_count: Int = 1
    var path: String? = null
    var percussion_map: HashMap<Int, Int>: HashMap<Int, Int>()
    public static fun load(path: String) { }
    public static fun new(): OpusManagerBase { }
    open private fun _load(path: String) { }
    open private fun _new() { }
    open public fun insert_after(beat_key: BeatKey, position:   List<Int>) {}
    open public fun remove(beat_key: BeatKey, position: List<Int>) { }
    open public fun set_percussion_event(beat_key: BeatKey, position: List<Int>) { }
    open public fun set_event(beat_key: BeatKey, position: List<Int>, value: Int, relative: Boolean = False) { }
    open public fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) { }
    open public fun unset(beat_key: BeatKey, position: List<Int>) { }
    open public fun add_channel(channel: Int) { }
    open public fun change_line_channel(old_channel: Int, line_index: Int, new_channel: Int) { }
    open public fun insert_beat(index: Int?) { }
    open public fun move_line(channel: Int, old_index: Int, new_index: Int) { }

    open public fun new_line(channel: Int, index: Int? = null) { }
    open public fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) { }
    open public fun remove_beat(beat_index: Int) { }
    open public fun remove_channel(channel: Int) { }
    open public fun remove_line(channel: Int, index: Int? = null) { }

    open public fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) { }

    open public fun replace_beat(beat_key: BeatKey, tree: OpusTree<OpusEvent>) { }

    open public fun save(path: String? = null) { }

    open public fun swap_channels(channel_a: Int, channel_b: Int) { }

    //open public fun export(path: String? = null, kwargs: HashMap) { }

    public fun get_beat_tree(beat_key: BeatKey): OpusTree<OpusEvent> { }
    public fun get_tree(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent> {

    }

    private fun set_beat_count(new_count: Int) { }
    private fun get_working_dir(): String? { }
    private fun load_folder(path: String) { }
    private fun load_file(path: String) { }

    public fun import_midi(path: String) { }
}
