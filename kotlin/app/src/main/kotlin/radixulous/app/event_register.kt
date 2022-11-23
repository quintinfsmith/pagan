package radixulous.app
import radixulous.app.opusmanager.OpusEvent

class EventRegister {
    var RADIX = 12
    var register: OpusEvent? = null
    var channel: Int = 0

    fun add_digit(value: Int) {
        var current_register = this.register
        if (current_register == null) {
            this.register = OpusEvent(
                value,
                this.RADIX,
                channel,
                false
            )
        } else if (current_register.relative) {
            this.register = OpusEvent(
                current_register.note * value,
                current_register.radix,
                current_register.channel,
                true
            )
        } else {
            this.register = OpusEvent(
                (current_register.note * current_register.radix) + value,
                current_register.radix,
                current_register.channel,
                false
            )
        }
    }

    fun is_ready(): Boolean {
        var register = this.register
        return register != null && (register.relative || register.note >= this.RADIX)
    }

    fun clear() {
        this.register = null
    }

    fun fetch(): OpusEvent? {
        var output = this.register
        this.register = null
        return output
    }

    fun relative_add_entry() {
        this.register = OpusEvent(1, this.RADIX, this.channel, true)
    }

    fun relative_subtract_entry() {
        this.register = OpusEvent(-1, this.RADIX, this.channel, true)
    }

    fun relative_downshift_entry() {
        this.register = OpusEvent(-1 * this.RADIX, this.RADIX, this.channel, true)
    }

    fun relative_upshift_entry() {
        this.register = OpusEvent(this.RADIX, this.RADIX, this.channel, true)
    }

    fun set_channel(channel: Int) {
        this.channel = channel
    }
}
