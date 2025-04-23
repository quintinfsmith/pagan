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
import com.qfs.apres.event.GeneralMIDIEvent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class MidiController(var context: Context, var auto_connect: Boolean = true) {

    var midi_manager: MidiManager? = try {
        this.context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    } catch (e: java.lang.NullPointerException) {
        null
    }
    var receiver = object: MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            val msg_list = msg!!.toMutableList()
            msg_list.removeAt(0)
            val event = StandardMidiFileInterface.event_from_bytes(msg_list, 0x90.toByte()) ?: return
            if (! this@MidiController.block_physical_devices) {
                broadcast_event(event)
            }
        }
    }

    var virtual_input_devices: MutableList<VirtualMidiInputDevice> = mutableListOf()
    var virtual_output_devices: MutableList<VirtualMidiOutputDevice> = mutableListOf()
    var connected_input_ports = mutableListOf<MidiInputPort>()
    private val mapped_input_ports = HashMap<Int, MutableList<MidiInputPort>>()
    private val mapped_output_ports = HashMap<Int, MutableList<MidiOutputPort>>()
    private val virtual_output_mutex = Mutex()
    var block_physical_devices = false

    init {
        val midi_manager_callback = object: MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device_info: MidiDeviceInfo) {
                if (this@MidiController.auto_connect) {
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
        if (this.midi_manager != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                this.midi_manager!!.registerDeviceCallback(
                    TRANSPORT_MIDI_BYTE_STREAM,
                    { runnable: Runnable ->
                        runnable.run()
                    },
                    midi_manager_callback
                )
            } else {
                @Suppress("DEPRECATION")
                this.midi_manager!!.registerDeviceCallback(midi_manager_callback, null)
            }
        }

        if (this.auto_connect) {
            this.open_connected_devices()
        }
    }

    open fun onDeviceAdded(device_info: MidiDeviceInfo) { }
    open fun onDeviceRemoved(device_info: MidiDeviceInfo) { }

    fun open_output_devices() {
        for (device_info in this.poll_output_devices()) {
            this.open_output_device(device_info)
        }
    }

    fun close_output_devices() {
        for (connected_input_port in this.connected_input_ports) {
            try {
                connected_input_port.close()
            } catch (e: IllegalArgumentException) {
                // Pass
            }
        }
        this.connected_input_ports.clear()
    }

    fun open_connected_devices() {
        this.open_output_devices()

        for (device_info in this.poll_input_devices()) {
            this.open_input_device(device_info)
        }
    }

    fun close_connected_devices() {
        this.close_output_devices()
    }

    fun connect_virtual_input_device(device: VirtualMidiInputDevice) {
        this.virtual_input_devices.add(device)
        device.set_midi_controller(this)
    }

    fun disconnect_virtual_input_device(device: VirtualMidiInputDevice) {
        val index = this.virtual_input_devices.indexOf(device)
        if (index >= 0) {
            this.virtual_input_devices.removeAt(index)
        }
        device.unset_midi_controller()
    }

    fun connect_virtual_output_device(device: VirtualMidiOutputDevice) {
        runBlocking {
            this@MidiController.virtual_output_mutex.withLock{
                this@MidiController.virtual_output_devices.add(device)
            }
        }
    }

    fun disconnect_virtual_output_device(device: VirtualMidiOutputDevice) {
        runBlocking {
            this@MidiController.virtual_output_mutex.withLock {
                val index = this@MidiController.virtual_output_devices.indexOf(device)
                if (index >= 0) {
                    this@MidiController.virtual_output_devices.removeAt(index)
                }
            }
        }
    }

    fun broadcast_event(event: GeneralMIDIEvent) {
        // Rebroadcast to listening devices
        runBlocking {
            this@MidiController.virtual_output_mutex.withLock {
                for (device in this@MidiController.virtual_output_devices) {
                    device.receiveMessage(event)
                }
            }
        }

        if (! this.block_physical_devices) {
            for (input_port in this.connected_input_ports) {
                val bytes = event.as_bytes()
                try {
                    input_port.send(bytes, 0, bytes.size)
                } catch (e: java.io.IOException) {
                    continue
                }
            }
        }
    }

    fun poll_output_devices(): List<MidiDeviceInfo> {
        if (this.midi_manager == null) {
            return listOf()
        }

        val devices_info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.midi_manager!!.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            this.midi_manager!!.devices.toList()
        }

        val output_devices = mutableListOf<MidiDeviceInfo>()
        for (device_info in devices_info) {
            if (device_info.inputPortCount > 0) {
                output_devices.add(device_info)
            }
        }
        return output_devices
    }

    fun poll_input_devices(): List<MidiDeviceInfo> {
        if (this.midi_manager == null) {
            return listOf()
        }

        val devices_info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.midi_manager!!.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            this.midi_manager!!.devices.toList()
        }
        val input_devices = mutableListOf<MidiDeviceInfo>()
        for (device_info in devices_info) {
            if (device_info.outputPortCount > 0) {
                input_devices.add(device_info)
            }
        }
        return input_devices
    }

    // NOTE: output device has input port
    fun open_output_device(device_info: MidiDeviceInfo, port: Int? = null) {
        if (this.midi_manager == null) {
            return
        }

        val port_number = port ?: (device_info.ports.filter { it.type == TYPE_INPUT }).first().portNumber

        this.midi_manager!!.openDevice(device_info, {
            val input_port = it.openInputPort(port_number) ?: return@openDevice // TODO: check open ports?
            this.connected_input_ports.add(input_port)
            if (!this.mapped_input_ports.containsKey(device_info.id)) {
                this.mapped_input_ports[device_info.id] = mutableListOf()
            }
            this.mapped_input_ports[device_info.id]!!.add(input_port)
        }, null)
    }

    // NOTE: input device has output port
    fun open_input_device(device_info: MidiDeviceInfo, port: Int? = null) {
        if (this.midi_manager == null) {
            return
        }

        val port_number = port ?: (device_info.ports.filter { it.type == TYPE_OUTPUT }).first().portNumber

        this.midi_manager!!.openDevice(device_info, {
            val output_port = it.openOutputPort(port_number)
            output_port.connect(this.receiver)

            if (!this.mapped_output_ports.containsKey(device_info.id)) {
                this.mapped_output_ports[device_info.id] = mutableListOf()
            }
            this.mapped_output_ports[device_info.id]!!.add(output_port)
        }, null)
    }

    fun output_devices_connected(): Boolean {
        return this.poll_output_devices().isNotEmpty()
    }

    fun close_device(device_info: MidiDeviceInfo) {
        if (this.mapped_input_ports.containsKey(device_info.id)) {
            for (input_port in this.mapped_input_ports[device_info.id]!!) {
                if (this.connected_input_ports.contains(input_port)) {
                    this.connected_input_ports.remove(input_port)
                }
                input_port.close()
            }
            this.mapped_input_ports.remove(device_info.id)
        }
        if (this.mapped_output_ports.containsKey(device_info.id)) {
            for (output_port in this.mapped_output_ports[device_info.id]!!) {
                output_port.close()
            }
            this.mapped_output_ports.remove(device_info.id)
        }
    }

    fun is_connected(output_device: VirtualMidiOutputDevice): Boolean {
        return this.virtual_output_devices.contains(output_device)
    }
}
