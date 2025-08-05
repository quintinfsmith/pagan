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
                '\\' -> '\\'
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
                else -> {
                    if (input_char.code > 127) {
                        String.format("\\u%04x", input_char.code)
                    } else {
                        input_char.toString()
                    }
                }
            }
        }

        fun unescape(string: String): JSONString {
            val new_strings = mutableListOf<Char>()
            var skip = 0
            val chars = string.toList()
            println("ECAPING $string")
            for (i in 0 until chars.size) {
                if (skip > 0) {
                    skip -= 1
                    continue
                }

                val char = chars[i]
                if (char == '\\') {
                    when (chars[i + 1]) {
                        'u' -> {
                            skip = 4
                            val t = Char(
                                (chars[i + 2].digitToInt(16) shl 12) +
                                (chars[i + 3].digitToInt(16) shl 8) +
                                (chars[i + 4].digitToInt(16) shl 4) +
                                chars[i + 5].digitToInt(16)
                            )
                            print(t)
                            t
                        }
                        else -> {
                            skip = 1
                            try {
                                new_strings.add(
                                    JSONString.unescape_char(chars[i + 1])
                                )
                            } catch (_: InvalidEscapeChar) {
                                new_strings.add('\\')
                                new_strings.add(chars[i+1])
                            }
                        }
                    }
                } else {
                    new_strings.add(char)
                }
            }
            return JSONString(new_strings.joinToString(""))
        }
    }
    init {
        println("String: $value")
    }

    override fun to_string(indent: Int?): String {
        val escaped_string = List<String>(this.value.length) { i ->
            JSONString.escape_char(this.value.get(i))
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