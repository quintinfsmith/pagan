package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

@Serializable
data class JSONWrapper(
    val v: Int,
    val d: LoadedJSONData
)
