package com.qfs.apres

import android.annotation.SuppressLint
import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceInfo.PortInfo.TYPE_INPUT
import android.media.midi.MidiDeviceInfo.PortInfo.TYPE_OUTPUT
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import com.qfs.apres.event.MIDIEvent
import kotlin.concurrent.thread

open class MidiController(var context: Context, var auto_connect: Boolean = true) {
    var midi_manager: MidiManager = this.context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    var receiver = object: MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            val msg_list = msg!!.toMutableList()
            msg_list.removeFirst()
            val event = event_from_bytes(msg_list, 0x90.toByte()) ?: return
            broadcast_event(event)
        }
    }

    var virtual_devices: MutableList<VirtualMidiDevice> = mutableListOf()
    var connected_input_ports = mutableListOf<MidiInputPort>()
    private val mapped_input_ports = HashMap<Int, MutableList<MidiInputPort>>()
    private val mapped_output_ports = HashMap<Int, MutableList<MidiOutputPort>>()

    init {
        var that = this
        this.midi_manager.registerDeviceCallback(object: MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                if (that.auto_connect) {
                    if (device_info.inputPortCount > 0) {
                        that.open_output_device(device_info)
                    }
                    if (device_info.outputPortCount > 0) {
                        that.open_input_device(device_info)
                    }
                }
            }
            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                that.close_device(device_info)
            }
        }, null)
        if (this.auto_connect) {
            this.open_connected_devices()
        }
    }

    open fun onDeviceAdded(device_info: MidiDeviceInfo) { }
    open fun onDeviceRemoved(device_info: MidiDeviceInfo) { }

    fun open_connected_devices() {
        for (device_info in this.poll_output_devices()) {
            this.open_output_device(device_info)
        }
        for (device_info in this.poll_input_devices()) {
            this.open_input_device(device_info)
        }
    }

    fun connect_virtual_device(device: VirtualMidiDevice) {
        this.virtual_devices.add(device)
        device.setMidiController(this)
    }

    fun disconnect_virtual_device(device: VirtualMidiDevice) {
        val index = this.virtual_devices.indexOf(device)
        if (index >= 0) {
            this.virtual_devices.removeAt(index)
        }
    }

    fun broadcast_event(event: MIDIEvent) {
        // Rebroadcast to listening devices
        for (device in this.virtual_devices) {
            thread {
                device.receiveMessage(event)
            }
        }

        for (input_port in this.connected_input_ports) {
            input_port.send(event.as_bytes(), 0, 1)
        }
    }

    fun receiveMessage(event: MIDIEvent, source: VirtualMidiDevice) {
        // Rebroadcast to listening devices
        for (device in this.virtual_devices) {
            if (device == source) {
                continue
            }
            thread {
                device.receiveMessage(event)
            }
        }

        for (input_port in this.connected_input_ports) {
            val event_bytes = event.as_bytes()
            input_port.send(event_bytes, 0, event_bytes.size)
        }
    }

    @SuppressLint("NewApi")
    fun poll_output_devices(): List<MidiDeviceInfo> {
        val devices_info =  this.midi_manager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        val output_devices = mutableListOf<MidiDeviceInfo>()
        for (device_info in devices_info) {
            var device_name = device_info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (device_info.inputPortCount > 0) {
                output_devices.add(device_info)
            }
        }
        return output_devices
    }

    @SuppressLint("NewApi")
    fun poll_input_devices(): List<MidiDeviceInfo> {
        val devices_info =  this.midi_manager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        val input_devices = mutableListOf<MidiDeviceInfo>()
        for (device_info in devices_info) {
            var device_name = device_info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (device_info.outputPortCount > 0) {
                input_devices.add(device_info)
            }
        }
        return input_devices
    }

    // NOTE: output device has input port
    fun open_output_device(device_info: MidiDeviceInfo, port: Int? = null) {
        var port_number = port ?: (device_info.ports.filter { it.type == TYPE_INPUT }).first().portNumber

        this.midi_manager.openDevice(device_info, {
            val input_port = it.openInputPort(port_number) // TODO: check open ports?
            this.connected_input_ports.add(input_port)
            if (!this.mapped_input_ports.containsKey(device_info.id)) {
                this.mapped_input_ports[device_info.id] = mutableListOf()
            }
            this.mapped_input_ports[device_info.id]!!.add(input_port)
        }, null)
    }

    // NOTE: input device has output port
    fun open_input_device(device_info: MidiDeviceInfo, port: Int? = null) {
        var port_number = port ?: (device_info.ports.filter { it.type == TYPE_OUTPUT }).first().portNumber

        this.midi_manager.openDevice(device_info, {
            val output_port = it.openOutputPort(port_number)
            output_port.connect(this.receiver)
            if (!this.mapped_output_ports.containsKey(device_info.id)) {
                this.mapped_output_ports[device_info.id] = mutableListOf()
            }
            this.mapped_output_ports[device_info.id]!!.add(output_port)
        }, null)
    }

    fun output_devices_connected(): Boolean {
        return this.connected_input_ports.isNotEmpty()
    }

    fun close_device(device_info: MidiDeviceInfo) {
        if (this.mapped_input_ports.containsKey(device_info.id)) {
            this.mapped_input_ports[device_info.id]!!.forEach {
                it.close()
            }
            this.mapped_input_ports.remove(device_info.id)
        }
        if (this.mapped_output_ports.containsKey(device_info.id)) {
            this.mapped_output_ports[device_info.id]!!.forEach {
                it.close()
            }
            this.mapped_output_ports.remove(device_info.id)
        }
    }
}