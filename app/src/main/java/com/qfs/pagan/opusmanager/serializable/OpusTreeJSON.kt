package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

@Serializable
data class OpusTreeJSON<T>(
    var event: T?,
    var children: List<OpusTreeJSON<T>?>?
)

