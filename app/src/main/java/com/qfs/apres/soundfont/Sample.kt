package com.qfs.apres.soundfont

data class Sample(val ptr: Long) {
    constructor(
        name: String,
        loopStart: Int,
        loopEnd: Int,
        sampleRate: Int,
        originalPitch: Int,
        pitchCorrection: Int,
        linked_sample: Sample?,
        sampleType: Int,
        data_placeholder: Pair<Int, Int>,
    ): this(
        create(
            name,
            loopStart,
            loopEnd,
            sampleRate,
            originalPitch,
            pitchCorrection,
            linked_sample != null,
            linked_sample?.ptr ?: 0,
            sampleType,
            data_placeholder.first,
            data_placeholder.second
        )
    )
    companion object {
        init {
            System.loadLibrary("pagan")
        }

        external fun create(
            name: String,
            loopStart: Int,
            loopEnd: Int,
            sampleRate: Int,
            originalPitch: Int,
            pitchCorrection: Int,
            is_linked: Boolean,
            linked_sample: Long,
            sampleType: Int,
            data_placeholder_start: Int,
            data_placeholder_end: Int
        ): Long

    }

    val name: String
        get() = this.get_name_inner(this.ptr)
    val data_placeholder: Pair<Int, Int>
        get() = this._get_placeholder()

    val sample_type: Int
        get() = this.get_sample_type_inner(this.ptr)

    val linked_sample: Long?
        get() = if (this.is_linked_inner(this.ptr)) { this.get_linked_sample_inner(this.ptr) } else { null }

    external fun get_sample_type_inner(ptr: Long): Int
    external fun set_data_inner(ptr: Long, data: ShortArray)
    external fun get_data_placeholders(ptr: Long): Array<Int>
    external fun get_linked_sample_inner(ptr: Long): Long
    external fun is_linked_inner(ptr: Long): Boolean
    external fun get_name_inner(ptr: Long): String

    fun set_data(data: ShortArray) {
        this.set_data_inner(this.ptr, data)
    }

    private fun _get_placeholder(): Pair<Int, Int> {
        val array = this.get_data_placeholders(this.ptr)
        return Pair(array[0], array[1])
    }
}