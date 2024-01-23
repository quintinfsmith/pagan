package com.qfs.pagan
import com.qfs.pagan.opusmanager.LoadedJSONData
import com.qfs.pagan.opusmanager.LoadedJSONData0
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import com.qfs.pagan.opusmanager.BaseLayer as OpusManager

class ProjectManager(data_dir: String) {
    class MKDirFailedException(dir: String): Exception("Failed to create directory $dir")
    val path = "$data_dir/projects/"
    val cache_path = "$data_dir/project_list.json"

    fun get_directory(): File {
        val directory = File(this.path)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw MKDirFailedException(this.path)
            }
        }
        return directory
    }

    fun delete(opus_manager: OpusManager) {
        val path = opus_manager.path!!

        val file = File(path)
        if (file.isFile) {
            file.delete()
        }
        this.untrack_path(opus_manager.path!!)
    }

    fun move_to_copy(opus_manager: OpusManager) {
        val old_title = opus_manager.project_name
        val new_title = "$old_title (Copy)"
        val new_path = this.get_new_path()

        opus_manager.path = new_path
        opus_manager.project_name = new_title
        opus_manager.save()
    }

    fun save(opus_manager: OpusManager) {
        this.get_directory()
        opus_manager.save()
        this.track_path(opus_manager.path!!)
    }

    fun get_new_path(): String {
        var i = 0
        while (File("${this.path}/opus_$i.json").isFile) {
            i += 1
        }
        return "${this.path}/opus_$i.json"
    }

    fun has_projects_saved(): Boolean {
        val directory = File(this.path)
        if (!directory.isDirectory) {
            return false
        }
        return directory.listFiles()?.isNotEmpty() ?: false
    }

    fun cache_project_list() {
        if (!has_projects_saved()) {
            return
        }

        val directory = File(this.path)

        val json = Json {
            ignoreUnknownKeys = true
        }

        val project_list = mutableListOf<Pair<String, String>>()
        for (json_file in directory.listFiles()!!) {
            val project_name = this.get_file_project_name(json_file) ?: continue
            project_list.add(Pair(json_file.path, project_name))
        }
        project_list.sortBy { it.second }

        val json_string = json.encodeToString(project_list)
        val file = File(this.cache_path)
        file.writeText(json_string)
    }

    private fun get_file_project_name(file: File): String? {
        val json = Json {
            ignoreUnknownKeys = true
        }

        val content = file.readText(Charsets.UTF_8)
        return try {
            val json_obj: LoadedJSONData = json.decodeFromString(content)
            json_obj.name
        } catch (e: Exception) {
            try {
                val json_obj: LoadedJSONData0 = json.decodeFromString<LoadedJSONData0>(content)
                json_obj.name
            } catch (e: Exception) {
                null
            }
        }
    }

    fun get_project_list(): List<Pair<String, String>> {
        val json = Json {
            ignoreUnknownKeys = true
        }

        if (!File(this.cache_path).exists()) {
            this.cache_project_list()
        }

        var string_content = File(this.cache_path).readText(Charsets.UTF_8)

        return try {
            json.decodeFromString(string_content)
        } catch (e: Exception) { // TODO: Figure out how to precisely catch json error (JsonDecodingException not found)
            // Corruption Protection: if the cache file is bad json, delete and rebuild
            File(this.cache_path).delete()
            this.cache_project_list()
            string_content = File(this.cache_path).readText(Charsets.UTF_8)

            json.decodeFromString(string_content)
        }
    }

    fun track_path(path: String) {
        var project_list = this.get_project_list().toMutableList()
        var is_tracking = false
        for ((check_path, _name) in project_list) {
            if (check_path == path) {
                is_tracking = true
                break
            }
        }

        if (is_tracking) {
            return
        }
        val project_name = this.get_file_project_name(File(path)) ?: return

        project_list.add(Pair(project_name, path))
        project_list.sortBy { it.second }

        val json = Json {
            ignoreUnknownKeys = true
        }

        val json_string = json.encodeToString(project_list)
        val file = File(this.cache_path)
        file.writeText(json_string)
    }

    fun untrack_path(path: String) {
        var project_list = this.get_project_list().toMutableList()
        var index_to_pop = 0
        for ((check_path, _name) in project_list) {
            if (check_path == path) {
                break
            }
            index_to_pop += 1
        }

        if (index_to_pop == project_list.size) {
            return
        }

        project_list.removeAt(index_to_pop)
        project_list.sortBy { it.second }

        val json = Json {
            ignoreUnknownKeys = true
        }

        val json_string = json.encodeToString(project_list)
        val file = File(this.cache_path)
        file.writeText(json_string)
    }

}