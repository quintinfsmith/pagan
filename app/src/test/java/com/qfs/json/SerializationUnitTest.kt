import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.qfs.json.ParsedJSONObject

class SerializationUnitTest {
    @Test
    fun test_json() {
        val test_string = """{
            "key0" = 0
        }"""
        val ob = ParsedJSONObject(test_string)

    }
}

