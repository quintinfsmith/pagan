package com.qfs.apres.event

 class ProgramChange(channel: Int, program: Int): ChannelVoiceMessage(0xC0, channel, arrayOf<Int>(program)) {
    fun get_program(): Int {
        return this.get_data(0)
    }

    fun set_program(program: Int) {
        this.set_data(0, program)
    }
}