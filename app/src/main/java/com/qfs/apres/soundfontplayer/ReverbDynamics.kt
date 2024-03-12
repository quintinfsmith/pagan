package com.qfs.apres.soundfontplayer

data class ReverbDynamics(
    var room_size: Float = 10f,
    var decay: Float = 1f
) {
    companion object {
        const val SPEED_OF_SOUND: Float = 343f
    }

    var delay = (this.room_size * 2f) / ReverbDynamics.SPEED_OF_SOUND
    var bounces = (this.decay / this.delay).toInt()
    var factor = this.delay  / this.decay
}