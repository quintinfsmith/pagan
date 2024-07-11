
import com.qfs.json.Parser
import com.qfs.json.InvalidJSON
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue

class SerializationUnitTest {
    @Test
    fun test_json() {
        val test_string = """{
            "key0": 0
        }"""
        val ob = Parser.parse(test_string)
        if (ob != null) {
            println("${ob.to_string()}")
        }
    }
    @Test
    fun test_missing_colon_hashmap() {
        val test_string = """{ "key0" "0" }"""
        assertThrows(InvalidJSON::class.java) {
            val ob = Parser.parse(test_string)
        }
    }

    @Test
    fun test_missing_comma_hashmap() {
        val test_string = """{ "key0": "0" "key1": "1" }"""
        assertThrows(InvalidJSON::class.java) {
            val ob = Parser.parse(test_string)
        }
    }

    @Test
    fun test_missing_comma_list() {
        val test_string = """[ "key0" "0" ]"""
        assertThrows(InvalidJSON::class.java) {
            val ob = Parser.parse(test_string)
        }
    }

    @Test
    fun test_unterminated_list() {
        val test_string = "[0,1,2"
        assertThrows(InvalidJSON::class.java) {
            val ob = Parser.parse(test_string)
        }
    }

    @Test
    fun test_unterminated_hashmap() {
        val test_string = """{ "key": "value" """
        assertThrows(InvalidJSON::class.java) {
            val ob = Parser.parse(test_string)
        }
    }

    @Test
    fun test_unterminated_string() {
        val test_string = """{ "key": "value } """
        assertThrows(InvalidJSON::class.java) {
            val ob = Parser.parse(test_string)
        }
    }
}

