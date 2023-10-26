package com.qfs.apres.event

abstract class ControlChange(channel: Int, controller: Int, value: Int): ChannelVoiceMessage(0xB0, channel, arrayOf(controller, value))