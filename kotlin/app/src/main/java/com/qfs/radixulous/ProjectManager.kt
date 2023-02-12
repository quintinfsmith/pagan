package com.qfs.radixulous
import android.util.Xml
import android.widget.Toast
import androidx.navigation.findNavController
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import com.qfs.radixulous.opusmanager.OpusManagerBase as OpusManager

class ProjectManager(var projects_dir: String) {
    val projects_list_file_path = "/data/data/com.qfs.radixulous/projects.json"
    private fun get_title(path: String): String? {
        val project_list_file = File(this.projects_list_file_path)

        if (project_list_file.isFile) {
            val content = project_list_file.readText(Charsets.UTF_8)
            val json_project_list: MutableList<ProjectDirPair> = Json.decodeFromString(content)

            json_project_list.forEachIndexed { _, pair ->
                if (pair.filename == path) {
                    return pair.title
                }
            }
        }
        return null
    }

    // I don't think there's ever a need for this, i'll leave it commented just in case
    //private fun get_filename(title: String): String? {
    //    val project_list_file = File(this.projects_list_file_path)
    //    if (project_list_file.isFile) {
    //        val content = project_list_file.readText(Charsets.UTF_8)
    //        val json_project_list: MutableList<ProjectDirPair> = Json.decodeFromString(content)

    //        json_project_list.forEachIndexed { _, pair ->
    //            if (pair.title == title) {
    //                return pair.filename
    //            }
    //        }
    //    }
    //    return null
    //}

    private fun set_title(title: String, filename: String) {
        val project_list_file = File(this.projects_list_file_path)

        var current_exists = false
        val project_list = if (project_list_file.isFile) {
            val content = project_list_file.readText(Charsets.UTF_8)
            val json_project_list: MutableList<ProjectDirPair> = Json.decodeFromString(content)

            json_project_list.forEachIndexed { _, pair ->
                if (pair.filename == filename) {
                    current_exists = true
                    pair.title = title
                }
            }

            json_project_list
        } else {
            mutableListOf()
        }

        if (! current_exists) {
            project_list.add(
                ProjectDirPair(
                    title = title,
                    filename = filename
                )
            )
        }

        project_list_file.writeText( Json.encodeToString( project_list ) )
    }

    private fun remove_from_ledger(filename: String) {
        val project_list_file = File(this.projects_list_file_path)

        val project_list = if (project_list_file.isFile) {
            val content = project_list_file.readText(Charsets.UTF_8)
            val json_project_list: MutableList<ProjectDirPair> = Json.decodeFromString(content)
            var output: MutableList<ProjectDirPair> = mutableListOf()
            json_project_list.forEachIndexed { _, pair ->
                if (pair.filename != filename) {
                    output.add(pair)
                }
            }

            output
        } else {
            mutableListOf()
        }

        project_list_file.writeText( Json.encodeToString( project_list ) )

    }

    fun delete(opus_manager: OpusManager) {
        val path = opus_manager.path!!
        val filename = path.substring(path.lastIndexOf("/") + 1)

        this.remove_from_ledger(filename)

        var file = File(path)
        if (file.isFile) {
            file.delete()
        }
    }

    fun rename(opus_manager: OpusManager, new_title: String) {
        val path = opus_manager.path!!
        val filename = path.substring(path.lastIndexOf("/") + 1)
        this.set_title(new_title, filename)
    }

    fun copy(opus_manager: OpusManager): String {
        val path = opus_manager.path!!
        val filename = path.substring(path.lastIndexOf("/") + 1)
        var old_title = this.get_title(filename)

        var new_title = "$old_title (Copy)"
        var new_path = this.get_new_path()

        this.set_title(new_title, new_path.substring(path.lastIndexOf("/") + 1))
        opus_manager.path = new_path
        opus_manager.save()
        return new_title
    }

    fun save(title: String, opus_manager: OpusManager) {
        // Saving opus_manager first ensures projects path exists
        opus_manager.save()
        val path = opus_manager.path!!
        val filename = path.substring(path.lastIndexOf("/") + 1)
        this.set_title(title, filename)
    }

    fun get_new_path(): String {
        var i = 0
        while (File("${this.projects_dir}/opus_$i.json").isFile) {
            i += 1
        }
        return "${this.projects_dir}/opus_$i.json"
    }
}