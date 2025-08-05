
import com.qfs.json.InvalidJSON
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONObject
import com.qfs.json.JSONParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationUnitTest {
    val string_rep = """{
    "key0": 0,
    "key1": [
        [
            0,
            1,
            {
                "key0": "blargh",
                "key1": false,
                "key2": true,
                "key3": 0.54,
                "key4": "🔥 \ud83d\udd25 \\ud83d\\udd25"
            }
        ],
        null,
        1.4,
        2,
        3
    ]
}"""

    @Test
    fun test_json() {
        val ob = JSONParser.parse<JSONObject>(this.string_rep)
        assertTrue( ob is JSONHashMap )
        assertEquals(
            0,
            (ob as JSONHashMap).get_int("key0")
        )
    }

    @Test
    fun test_indent() {
        val ob = JSONParser.parse<JSONObject>(this.string_rep)
        assertEquals(
            this.string_rep,
            ob!!.to_string(4)
        )
    }
    @Test
    fun test_missing_colon_hashmap() {
        val test_string = """{ "key0" "0" }"""
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }

    @Test
    fun test_missing_comma_hashmap() {
        val test_string = """{ "key0": "0" "key1": "1" }"""
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }

    @Test
    fun test_missing_comma_list() {
        val test_string = """[ "key0" "0" ]"""
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }

    @Test
    fun test_unterminated_list() {
        val test_string = "[0,1,2"
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }

    @Test
    fun test_unterminated_hashmap() {
        val test_string = """{ "key": "value" """
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }
    @Test
    fun test_no_value_hashmap() {
        val test_string = """{ "key" }"""
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }

        val test_string_b = """{ "key": }"""
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string_b)
        }
    }

    @Test
    fun test_unterminated_string() {
        val test_string = """{ "key": "value } """
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }

    @Test
    fun test_unterminated_string_2() {
        val test_string = "\"test"
        assertThrows(InvalidJSON::class.java) {
            JSONParser.parse<JSONObject>(test_string)
        }
    }
}

