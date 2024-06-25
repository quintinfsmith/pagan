
import com.qfs.json.Parser
import org.junit.Test

class SerializationUnitTest {
    @Test
    fun test_json() {
        val test_string = """{
            "key0" = 0
        }"""
        val ob = Parser.parse(test_string)

    }
}

