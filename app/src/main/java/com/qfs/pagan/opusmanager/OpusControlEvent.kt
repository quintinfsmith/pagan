package com.qfs.pagan.opusmanager

import com.qfs.json.*

abstract class OpusControlEvent(): OpusEvent() {
    abstract val control_name: String
    abstract fun populate_json(map: ParsedHashMap)
    override fun to_json(): ParsedHashMap {
        val output = ParsedHashMap(
            hashMapOf<String, ParsedObject?>(
                "c" to ParsedString(this.control_name)
            )
        )
        this.populate_json(output)
        return output
    }
}

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

class OpusTempoEvent(var value: Float): OpusControlEvent() {
    override val control_name = "Tempo"
    override fun populate_json(map: ParsedHashMap) {
        map.hash_map["tempo"] = ParsedFloat(this.value)
    }
}

class OpusVolumeEvent(var value: Int, var transition: Int = 0): OpusControlEvent() {
    override val control_name = "Volume"
    override fun populate_json(map: ParsedHashMap) {
        map.hash_map["volume"] = ParsedInt(this.value)
        map.hash_map["transition"] = ParsedInt(this.transition)
    }
}

class OpusReverbEvent(var value: Float): OpusControlEvent() {
    override val control_name = "Reverb"
    override fun populate_json(map: ParsedHashMap) {
        map.hash_map["wetness"] = ParsedFloat(this.value)
    }
}

