package com.qfs.apres.soundfontplayer

class ReverbBuffer(var sample_rate: Int, var reverb: ReverbDynamics) {
    private val reverb_cache_right = HashMap<Int, MutableSet<Pair<Int, Float>>>()
    private val reverb_cache_left = HashMap<Int, MutableSet<Pair<Int, Float>>>()
    private val frame_delay = this.sample_rate * this.reverb.delay

    fun add_reverb_point(value: Float, start_frame: Int, right_side: Boolean = false) {
        val next_frame = (start_frame.toFloat() + this.frame_delay).toInt()
        val reverb_cache = if (right_side) {
            this.reverb_cache_right
        } else {
            this.reverb_cache_left
        }

        if (!reverb_cache.containsKey(next_frame)) {
            reverb_cache[next_frame] = mutableSetOf()
        }

        reverb_cache[next_frame]!!.add(
            Pair(
                this.reverb.bounces,
                value * this.reverb.factor
            )
        )
    }

    fun clear() {
        this.reverb_cache_left.clear()
        this.reverb_cache_right.clear()
    }

    fun get_frame(frame: Int): Pair<Float, Float> {
        val next_frame = (frame.toFloat() + this.frame_delay).toInt()

        var output_left = 0f
        var output_right = 0f

        if (!this.reverb_cache_left.contains(next_frame)) {
            this.reverb_cache_left[next_frame] = mutableSetOf()
        }

        for ((bounces_remaining, value) in this.reverb_cache_left.remove(frame) ?: setOf()) {
            output_left += value
            if (bounces_remaining == 0) {
                continue
            }

            this.reverb_cache_left[next_frame]!!.add(
                Pair(
                    bounces_remaining - 1,
                    value * this.reverb.factor
                )
            )
        }

        if (!this.reverb_cache_right.contains(next_frame)) {
            this.reverb_cache_right[next_frame] = mutableSetOf()
        }

        for ((bounces_remaining, value) in this.reverb_cache_right.remove(frame) ?: setOf()) {
            output_right += value
            if (bounces_remaining == 0) {
                continue
            }

            this.reverb_cache_right[next_frame]!!.add(
                Pair(
                    bounces_remaining - 1,
                    value * this.reverb.factor
                )
            )
        }


        return Pair(output_left, output_right)
    }
}