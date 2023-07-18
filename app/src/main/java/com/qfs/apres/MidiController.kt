package com.qfs.apres

import android.content.Context
import com.qfs.apres.event.MIDIEvent
import kotlin.concurrent.thread

// Reference code base on https://github.com/android/ndk-samples/tree/main/native-midi
open class MidiController(var context: Context) {
    var virtualDevices: MutableList<VirtualMidiDevice> = mutableListOf()

    fun registerVirtualDevice(device: VirtualMidiDevice) {
        this.virtualDevices.add(device)
        device.setMidiController(this)
    }
    fun unregisterVirtualDevice(device: VirtualMidiDevice) {
        val index = this.virtualDevices.indexOf(device)
        if (index >= 0) {
            this.virtualDevices.removeAt(index)
        }
    }

    // TODO: Native midi support
    //private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager


    //// Selected Device(s)
    //private var incomingDevice : MidiDevice? = null
    //private var outgoingDevice : MidiDevice? = null
    //private val outgoingPort: MidiInputPort? = null

    //private var process_queue: MutableList<MIDIEvent> = mutableListOf()

    //init {
    //    val midiDevices = getIncomingDevices() // method defined in snippet above
    //    if (midiDevices.isNotEmpty()) {
    //        this.openIncomingDevice(midiDevices[0])
    //    }
    //}

    //class OpenIncomingDeviceListener : OnDeviceOpenedListener {
    //    open external fun startReadingMidi(incomingDevice: MidiDevice?, portNumber: Int)
    //    open external fun stopReadingMidi()
    //    override fun onDeviceOpened(device: MidiDevice) {
    //        this.startReadingMidi(device, 0 /*mPortNumber*/)
    //    }
    //}

    //open fun openIncomingDevice(devInfo: MidiDeviceInfo?) {
    //    midiManager.openDevice(devInfo, OpenIncomingDeviceListener(), null)
    //}

    //open fun closeIncomingDevice() {
    //    if (this.incomingDevice != null) {
    //        // Native API
    //        this.incomingDevice = null
    //    }
    //}
    fun receiveMessage(event: MIDIEvent, source: VirtualMidiDevice) {
        // Rebroadcast to listening devices
        for (device in this.virtualDevices) {
            if (device == source) {
                continue
            }
            thread {
                device.receiveMessage(event)
            }
        }
    }

    //open fun onNativeMessageReceive(message: ByteArray) {
    //    var event = event_from_bytes(message.toMutableList()) ?: return
    //    this.receiveMessage(event)
    //    this.process_queue.add(event)
    //    // Messages are received on some other thread, so switch to the UI thread
    //    // before attempting to access the UI
    //    // UiThreadStatement.runOnUiThread(Runnable { showReceivedMessage(message) })
    //}

    //// Send Device
    //class OpenOutgoingDeviceListener : OnDeviceOpenedListener {
    //    open external fun startWritingMidi(sendDevice: MidiDevice?, portNumber: Int)
    //    open external fun stopWritingMidi()
    //    override fun onDeviceOpened(device: MidiDevice) {
    //        this.startWritingMidi(device, 0 /*mPortNumber*/)
    //    }
    //}

    //open fun openSendDevice(devInfo: MidiDeviceInfo?) {
    //    this.midiManager.openDevice(devInfo, OpenOutgoingDeviceListener(), null)
    //}

    //open fun closeSendDevice() {
    //    if (this.outgoingDevice != null) {
    //        // Native API
    //        this.outgoingDevice = null
    //    }
    //}

    //fun sendMessage(event: MIDIEvent) {
    //    var bytes = event.as_bytes()
    //    this.writeMidi(bytes, bytes.size)
    //}

    //private fun getOutgoingDevices(): List<MidiDeviceInfo> {
    //    return midiManager.devices.filter { it.outputPortCount > 0 }
    //}

    //private fun getIncomingDevices() : List<MidiDeviceInfo> {
    //    return midiManager.devices.filter { it.inputPortCount > 0 }
    //}

    ////
    //// Native API stuff
    ////
    //open fun loadNativeAPI() {
    //    System.loadLibrary("native_midi")
    //}

    //open external fun writeMidi(data: ByteArray?, length: Int)
}