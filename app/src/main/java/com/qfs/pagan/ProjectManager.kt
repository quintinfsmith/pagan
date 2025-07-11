package com.qfs.pagan
import android.content.Context
import android.net.Uri
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import com.qfs.pagan.jsoninterfaces.OpusManagerJSONInterface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.qfs.pagan.opusmanager.OpusLayerBase as OpusManager

class ProjectManager(val context: Context) {
    class MKDirFailedException(dir: String): Exception("Failed to create directory $dir")
    private val data_dir = context.getExternalFilesDir(null)!!
    val path = "$data_dir/projects/"
    private val _cache_path = "$data_dir/project_list.json"

    fun contains(uri: Uri): Boolean {
        // clean uri
        val check_path = File(uri.toString()).path ?: return false

        val file_list = File(this.path).listFiles() ?: return false
        for (f in file_list) {
            if (f.path == check_path) {
                return true
            }
        }
        return false
    }

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

        this._untrack_path(opus_manager.path!!)
    }

    fun move_to_copy(opus_manager: OpusManager) {
        val old_title = opus_manager.project_name

        val new_title: String? = if (old_title == null) {
            null
        } else {
            "$old_title (Copy)"
        }

        val new_path = this.get_new_path()

        opus_manager.path = new_path
        opus_manager.project_name = new_title
    }

    fun save(opus_manager: OpusManager) {
        this.get_directory()

        opus_manager.save()

        // Untrack then track in order to update the project title in the cache
        this._untrack_path(opus_manager.path!!)
        this._track_path(opus_manager.path!!)
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

    private fun generate_file_project_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return this.context.getString(R.string.untitled_op, now.format(formatter))
    }

    private fun _cache_project_list() {
        if (!has_projects_saved()) {
            return
        }

        val directory = File(this.path)
        val json = Json {
            ignoreUnknownKeys = true
        }

        val project_list = mutableListOf<Pair<String, String>>()
        for (json_file in directory.listFiles()!!) {
            var project_name = try {
                this.get_file_project_name(json_file) ?: this.generate_file_project_name()
            } catch (e: Exception) {
                this.context.getString(R.string.corrupted_project)
            }
            project_list.add(Pair(json_file.path, project_name))
        }

        project_list.sortBy { it.second }

        val json_string = json.encodeToString(project_list)
        val file = File(this._cache_path)
        file.writeText(json_string)
    }

    private fun get_file_project_name(file: File): String? {
        val content = file.readText(Charsets.UTF_8)
        val json_obj = JSONParser.parse<JSONHashMap>(content) ?: return null

        val version = OpusManagerJSONInterface.detect_version(json_obj)
        return when (version) {
            0, 1, 2 -> json_obj.get_string("name")
            else -> {
                json_obj.get_hashmap("d").get_string("title", this.generate_file_project_name())
            }
        }
    }

    fun get_project_list(): List<Pair<String, String>> {
        val json = Json {
            ignoreUnknownKeys = true
        }

        if (!File(this._cache_path).exists()) {
            this._cache_project_list()
        }

        var string_content = File(this._cache_path).readText(Charsets.UTF_8)

        return try {
            json.decodeFromString(string_content)
        } catch (e: Exception) { // TODO: Figure out how to precisely catch json error (JsonDecodingException not found)
            // Corruption Protection: if the cache file is bad json, delete and rebuild
            File(this._cache_path).delete()
            this._cache_project_list()
            string_content = File(this._cache_path).readText(Charsets.UTF_8)

            json.decodeFromString(string_content)
        }
    }

    private fun _track_path(path: String) {
        val project_list = this.get_project_list().toMutableList()
        var is_tracking = false

        // use File object to clean up path
        val project_file = File(path)
        for ((check_path, _) in project_list) {
            if (check_path == project_file.path) {
                is_tracking = true
                break
            }
        }

        if (is_tracking) {
            return
        }

        val project_name = this.get_file_project_name(project_file) ?: this.generate_file_project_name()
        project_list.add(Pair(path, project_name))
        project_list.sortBy { it.second }

        val json = Json {
            ignoreUnknownKeys = true
        }

        val json_string = json.encodeToString(project_list)
        val file = File(this._cache_path)
        file.writeText(json_string)
    }

    private fun _untrack_path(path: String) {
        val project_list = this.get_project_list().toMutableList()
        var index_to_pop = 0
        for ((check_path, _) in project_list) {
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
        val file = File(this._cache_path)
        file.writeText(json_string)
    }

}
