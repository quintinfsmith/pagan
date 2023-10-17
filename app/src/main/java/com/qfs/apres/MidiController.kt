package com.qfs.apres

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceInfo.PortInfo.TYPE_INPUT
import android.media.midi.MidiDeviceInfo.PortInfo.TYPE_OUTPUT
import android.media.midi.MidiDeviceStatus
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiManager.TRANSPORT_MIDI_BYTE_STREAM
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
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

    var virtual_input_devices: MutableList<VirtualMidiDevice> = mutableListOf()
    var virtual_output_devices: MutableList<VirtualMidiDevice> = mutableListOf()
    var connected_input_ports = mutableListOf<MidiInputPort>()
    private val mapped_input_ports = HashMap<Int, MutableList<MidiInputPort>>()
    private val mapped_output_ports = HashMap<Int, MutableList<MidiOutputPort>>()

    init {
        val midi_manager_callback = object: MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                if (this@MidiController.auto_connect) {
                    if (device_info.inputPortCount > 0) {
                        this@MidiController.open_output_device(device_info)
                    }
                    if (device_info.outputPortCount > 0) {
                        this@MidiController.open_input_device(device_info)
                    }
                }
                this@MidiController.onDeviceAdded(device_info)
            }
            override fun onDeviceRemoved(device_info: MidiDeviceInfo) {
                this@MidiController.close_device(device_info)
                this@MidiController.onDeviceRemoved(device_info)
            }
            override fun onDeviceStatusChanged(status: MidiDeviceStatus) {
            }
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            this.midi_manager.registerDeviceCallback( TRANSPORT_MIDI_BYTE_STREAM, { }, midi_manager_callback)
        } else {
            @Suppress("DEPRECATION")
            this.midi_manager.registerDeviceCallback(midi_manager_callback, null)
        }

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

    fun connect_virtual_input_device(device: VirtualMidiDevice) {
        this.virtual_input_devices.add(device)
        device.setMidiController(this)
    }

    fun disconnect_virtual_input_device(device: VirtualMidiDevice) {
        val index = this.virtual_input_devices.indexOf(device)
        if (index >= 0) {
            this.virtual_input_devices.removeAt(index)
        }
    }

    fun connect_virtual_output_device(device: VirtualMidiDevice) {
        this.virtual_output_devices.add(device)
        device.setMidiController(this)
    }

    fun disconnect_virtual_output_device(device: VirtualMidiDevice) {
        val index = this.virtual_output_devices.indexOf(device)
        if (index >= 0) {
            this.virtual_output_devices.removeAt(index)
        }
    }

    fun broadcast_event(event: MIDIEvent) {
        // Rebroadcast to listening devices
        for (device in this.virtual_output_devices) {
            thread {
                device.receiveMessage(event)
            }
        }

        for (input_port in this.connected_input_ports) {
            var bytes = event.as_bytes()
            input_port.send(bytes, 0, bytes.size)
        }
    }

    fun poll_output_devices(): List<MidiDeviceInfo> {
        val devices_info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.midi_manager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            this.midi_manager.devices.toList()
        }
        val output_devices = mutableListOf<MidiDeviceInfo>()
        for (device_info in devices_info) {
            var device_name = device_info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (device_info.inputPortCount > 0) {
                output_devices.add(device_info)
            }
        }
        return output_devices
    }

    fun poll_input_devices(): List<MidiDeviceInfo> {
        val devices_info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.midi_manager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            this.midi_manager.devices.toList()
        }
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
                if (this.connected_input_ports.contains(it)) {
                    this.connected_input_ports.remove(it)
                }
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