package com.qfs.radixulous
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import com.qfs.radixulous.opusmanager.OpusManagerBase as OpusManager

class ProjectManager(data_dir: String) {
    val projects_dir = "$data_dir/projects/"
    val projects_list_file_path = "$data_dir/projects.json"
    private fun get_title(path: String): String? {
        val project_list_file = File(this.projects_list_file_path)

        if (project_list_file.isFile) {
            val content = project_list_file.readText(Charsets.UTF_8)
            val json_project_list: MutableList<LoadFragment.ProjectDirPair> = Json.decodeFromString(content)

            json_project_list.forEachIndexed { _, pair ->
                if (pair.filename == path) {
                    return pair.title
                }
            }
        }
        return null
    }

    fun set_title(title: String, opus_manager: OpusManager) {
        val path = opus_manager.path!!
        val filename = path.substring(path.lastIndexOf("/") + 1)
        this.set_title(title, filename)
    }

    private fun set_title(title: String, filename: String) {
        val project_list_file = File(this.projects_list_file_path)

        var current_exists = false
        val project_list = if (project_list_file.isFile) {
            val content = project_list_file.readText(Charsets.UTF_8)
            val json_project_list: MutableList<LoadFragment.ProjectDirPair> = Json.decodeFromString(content)

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
                LoadFragment.ProjectDirPair(
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
            val json_project_list: MutableList<LoadFragment.ProjectDirPair> = Json.decodeFromString(content)
            val output: MutableList<LoadFragment.ProjectDirPair> = mutableListOf()
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

        val file = File(path)
        if (file.isFile) {
            file.delete()
        }
    }

    fun copy(opus_manager: OpusManager): String {
        val path = opus_manager.path!!
        val filename = path.substring(path.lastIndexOf("/") + 1)
        val old_title = this.get_title(filename)

        val new_title = "$old_title (Copy)"
        val new_path = this.get_new_path()

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