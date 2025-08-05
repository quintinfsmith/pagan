package com.qfs.json

data class JSONString(var value: String): JSONObject {
    class InvalidEscapeChar(char: Char): Exception("Invalid Escape Char $char")
    companion object {
        fun unescape_char(input_char: Char): Char {
            return when (input_char) {
                'n' -> '\n'
                'r' -> '\r'
                'b' -> '\b'
                'f' -> '\u000c'
                't' -> '\t'
                else -> throw InvalidEscapeChar(input_char)
            }
        }
        fun escape_char(input_char: Char): String {
            return when (input_char) {
                '\n' -> "\\n"
                '\r' -> "\\r"
                '\b' -> "\\b"
                '\u000c' -> "\\f"
                '\t' -> "\\t"
                else -> input_char.toString()
            }
        }

        fun unescape(string: String): JSONString {
            val new_strings = mutableListOf<Char>()
            var skip = 0
            val chars = string.toList()
            for (i in 0 until chars.size) {
                if (skip > 0) {
                    skip -= 1
                    continue
                }

                val char = chars[i]
                if (char == '\\') {
                    try {
                        new_strings.add(JSONString.unescape_char(chars[i + 1]))
                        skip = 1
                    } catch (_: InvalidEscapeChar) {
                        new_strings.add(char)
                    }
                } else {
                    new_strings.add(char)
                }
            }
            return JSONString(new_strings.joinToString(""))
        }
    }

    override fun to_string(indent: Int?): String {
        val escaped_string = List<String>(this.value.length) { i ->
            JSONString.escape_char(this.value[i])
        }.joinToString("")

        return "\"$escaped_string\""
    }


    override fun equals(other: Any?): Boolean {
        return (other is JSONString && other.value == this.value)
    }

    override fun toString(): String {
        return this.to_string()
    }

    override fun copy(): JSONObject {
        return JSONString(this.value)
    }

    override fun hashCode(): Int {
        return this.value.hashCode()
    }
}