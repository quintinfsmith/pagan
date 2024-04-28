package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

interface OpusEvent

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

@Serializable
sealed class OpusControlEvent: OpusEvent {
    open fun get_leaf_label(): String { TODO() }
    abstract fun copy(): OpusControlEvent
}

@Serializable
data class OpusTempoEvent(var value: Float): OpusControlEvent() {
    override fun get_leaf_label(): String {
        return this.value.toInt().toString()
    }

    override fun copy(): OpusTempoEvent {
        return OpusTempoEvent(this.value)
    }
}

@Serializable
data class OpusVolumeEvent(var value: Float, var transition: Int = 0): OpusControlEvent() {
    override fun get_leaf_label(): String {
        return this.value.toInt().toString()
    }

    override fun copy(): OpusVolumeEvent {
        return OpusVolumeEvent(this.value, this.transition)
    }
}

@Serializable
data class OpusReverbEvent(var value: Float): OpusControlEvent() {
    override fun get_leaf_label(): String {
        return this.value.toString()
    }

    override fun copy(): OpusReverbEvent {
        return OpusReverbEvent(this.value)
    }
}

@Serializable
data class OpusEventSTD(
    var note: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
): OpusEvent

