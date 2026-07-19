
import com.qfs.json.InvalidJSON
import com.qfs.json.InvalidJSONObject
import com.qfs.json.JSONBoolean
import com.qfs.json.JSONCompliant
import com.qfs.json.JSONFloat
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONObject
import com.qfs.json.JSONParser
import com.qfs.json.JSONString
import com.qfs.json.NonNullableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test
    fun test_compliant() {
        class Test(var a: Int): JSONCompliant {
            override fun to_json(): JSONInteger {
                return JSONInteger(this.a)
            }
        }

        val jmap = JSONHashMap("a" to Test(1))

        assertEquals(
            Test(1).to_json(),
            jmap["a"]
        )
    }

    @Test
    fun test_hashmap() {
        val jmap = JSONHashMap()
        assert(jmap.isEmpty())
        assert(!jmap.isNotEmpty())
        jmap["a"] = JSONInteger(1)
        assertEquals(1, jmap.get_int("a"))
        jmap["b"] = JSONFloat(1F)
        assertEquals(1F, jmap.get_float("b"))
        jmap["c"] = JSONString("Bums")
        assertEquals("Bums", jmap.get_string("c"))
        jmap["d"] = JSONList()
        jmap["e"] = null
        assertEquals(null, jmap.get_stringn("e"))
        assertThrows(NonNullableException::class.java) {
            jmap.get_string("e")
        }
        assertEquals(null, jmap.get_intn("e"))
        assertThrows(NonNullableException::class.java) {
            jmap.get_int("e")
        }
        assertEquals(null, jmap.get_booleann("e"))
        assertThrows(NonNullableException::class.java) {
            jmap.get_boolean("e")
        }
        assertEquals(null, jmap.get_floatn("e"))
        assertThrows(NonNullableException::class.java) {
            jmap.get_float("e")
        }
        assertEquals(null, jmap.get_listn("e"))
        assertThrows(NonNullableException::class.java) {
            jmap.get_list("e")
        }
        assertEquals(null, jmap.get_hashmapn("e"))
        assertThrows(NonNullableException::class.java) {
            jmap.get_hashmap("e")
        }
        jmap["f"] = JSONBoolean(true)
        assert(jmap.get_boolean("f"))


        class Test()
        assertThrows(InvalidJSONObject::class.java) {
            JSONHashMap("a" to Test())
        }

        assert(jmap.isNotEmpty())
        assert(!jmap.isEmpty())

        val copy = jmap.copy()
        assertEquals(jmap, copy)
        copy["x"] = null
        assertNotEquals(jmap, copy)
        copy.remove("x")
        copy["a"] = null
        assertNotEquals(jmap, copy)
        copy["a"] = jmap["a"]
        assertNotEquals(copy, JSONList(null))
    }

    @Test
    fun test_list() {
        val jmap = JSONList()
        assert(jmap.isEmpty())
        assert(!jmap.isNotEmpty())
        jmap.add(1)
        jmap.add(1F)
        jmap.add("Bums")
        jmap.add(JSONList())
        jmap.add(true)

        assert(!jmap.isEmpty())
        assert(jmap.isNotEmpty())

        assertEquals(5, jmap.size)
        assertEquals(1, jmap.get_int(0))
        assertEquals(1F, jmap.get_float(1))
        assertEquals("Bums", jmap.get_string(2))
        assert(jmap.get_boolean(4))

        val dup = JSONList(*Array(jmap.size) { null })
        for (i in 0 until jmap.size) {
            dup[i] = jmap[i]
            assertEquals(jmap[i], dup[i])
        }



        //assertEquals(1, jmap.get_int("a"))
        //assertEquals(1F, jmap.get_float("b"))
        //assertEquals("Bums", jmap.get_string("c"))
        //assertEquals(null, jmap.get_stringn("e"))
        //assertThrows(NonNullableException::class.java) {
        //    jmap.get_string("e")
        //}
        //assertEquals(null, jmap.get_intn("e"))
        //assertThrows(NonNullableException::class.java) {
        //    jmap.get_int("e")
        //}
        //assertEquals(null, jmap.get_booleann("e"))
        //assertThrows(NonNullableException::class.java) {
        //    jmap.get_boolean("e")
        //}
        //assertEquals(null, jmap.get_floatn("e"))
        //assertThrows(NonNullableException::class.java) {
        //    jmap.get_float("e")
        //}
        //assertEquals(null, jmap.get_listn("e"))
        //assertThrows(NonNullableException::class.java) {
        //    jmap.get_list("e")
        //}
        //assertEquals(null, jmap.get_hashmapn("e"))
        //assertThrows(NonNullableException::class.java) {
        //    jmap.get_hashmap("e")
        //}
        //assert(jmap.get_boolean("f"))

        // class Test()
        // assertThrows(InvalidJSONObject::class.java) {
        //     JSONHashMap("a" to Test())
        // }

        // assert(jmap.isNotEmpty())
        // assert(!jmap.isEmpty())

        // val copy = jmap.copy()
        // assertEquals(jmap, copy)
        // copy["x"] = null
        // assertNotEquals(jmap, copy)
        // copy.remove("x")
        // copy["a"] = null
        // assertNotEquals(jmap, copy)
        // copy["a"] = jmap["a"]
        // assertNotEquals(copy, JSONList(null))
    }
}

