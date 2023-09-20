package com.qfs.pagan

import org.junit.Test
import org.junit.Assert.*
import org.simpleframework.xml.Root
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.Text
import org.simpleframework.xml.core.Persister
import java.io.File

class MuseScoreTest {
    @Test
    fun test_musescore_loading() {
        val file = File("score.mscx")

        val serializer: Serializer = Persister()
        val dataFetch = serializer.read(MuseScore::class.java, file.readText())
        println(dataFetch.program_version.text)
        println(dataFetch.score.meta_tag_list)
    }

    @Test
    fun test_class() {

    }
    @Root(strict = false, name="TestA")
    class TestClass {
        @field:Attribute(name="name", required=false)
        var name: String? = null
        @field:Text
        var text: String = ""
    }

}