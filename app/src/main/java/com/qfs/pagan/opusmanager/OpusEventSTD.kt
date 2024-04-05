package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

interface OpusEvent {}

enum class Transition {
    Linear
}
enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

@Serializable
data class OpusControlEvent(
    var value: Float,
    var duration: Int = 0,
    var transition: Transition = Transition.Linear
): OpusEvent

@Serializable
data class OpusEventSTD(
    var note: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
): OpusEvent

