package com.qfs.apres.soundfontplayer

data class ReverbDynamics(
    var room_size: Float = 10f,
    var decay: Float = 1f
) {
    companion object {
        const val SPEED_OF_SOUND: Float = 343f
    }
    fun get_travel_duration(): Float {
        return (this.room_size * 2f) / ReverbDynamics.SPEED_OF_SOUND
    }
    fun get_reduction_factor(): Float {
        return this.get_travel_duration() / this.decay
    }
}