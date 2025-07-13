package com.qfs.pagan
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import com.qfs.pagan.jsoninterfaces.OpusManagerJSONInterface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.qfs.pagan.opusmanager.OpusLayerBase as OpusManager
class ProjectManager(val context: Context, var path: Uri?) {
    class MKDirFailedException(dir: String): Exception("Failed to create directory $dir")
    class InvalidDirectory(path: Uri): Exception("Real Directory Required ($path)")
    class PathNotSetException(): Exception("Projects path has not been set.")
    private val _cache_path = "${context.applicationContext.dataDir}/project_list.json"
    var active_project: DocumentFile? = null

    init {
        this.recache_project_list()
    }
    fun move_old_projects_directory(old_path: Uri) {
        val old_directory = DocumentFile.fromTreeUri(this.context, old_path) ?: return
        if (!old_directory.isDirectory) {
            return
        }

        for (project in old_directory.listFiles()) {
            if (!project.isFile) {
                continue
            }
            // Use new path instead of copying file name to avoid collisions
            println("${project.uri}")
            val new_file = this.get_new_file() ?: continue
            val output_stream = this.context.contentResolver.openOutputStream(new_file.uri)
            val input_stream = this.context.contentResolver.openInputStream(project.uri)


            val buffer = ByteArray(1024)
            while (true) {
                val read_size = input_stream?.read(buffer) ?: break
                if (read_size == -1) {
                    break
                }
                output_stream?.write(buffer, 0, read_size)
            }

            input_stream?.close()
            output_stream?.flush()
            output_stream?.close()
        }
        this.recache_project_list()
    }

    fun set_new_project() {
        this.active_project = null
    }

    fun set_project(uri: Uri) {
        this.active_project = DocumentFile.fromSingleUri(this.context, uri)
    }

    fun change_project_path(new_uri: Uri) {
        val new_directory = DocumentFile.fromTreeUri(this.context, new_uri)
        if (new_directory == null || !new_directory.isDirectory) {
            throw InvalidDirectory(new_uri)
        }

        val old_path = this.path
        this.path = new_uri
        if (old_path != null) {
            this.move_old_projects_directory(old_path)
        }
    }

    fun contains(uri: Uri): Boolean {
        if (this.path == null) {
            return false
        }

        val output = uri.authority == this.path?.authority && uri.pathSegments.size > this.path!!.pathSegments.size  && uri.pathSegments.subList(0, this.path!!.pathSegments.size) == this.path!!.pathSegments
        return output
    }

    fun delete(uri: Uri) {
        val document_file = DocumentFile.fromSingleUri(this.context, uri) ?: return
        if (document_file.isFile) {
            document_file.delete()
        }

        this._untrack_uri(uri)
    }

    fun move_to_copy(opus_manager: OpusManager) {
        val old_title = opus_manager.project_name
        val new_title: String? = if (old_title == null) {
            null
        } else {
            "$old_title (Copy)"
        }

        this.active_project = null
        opus_manager.project_name = new_title
    }

    fun save(opus_manager: OpusManager) {
        if (this.active_project == null) {
            this.active_project = this.get_new_file()
        }

        // Untrack then track in order to update the project title in the cache
        this.active_project?.uri?.let {
            val content = opus_manager.to_json()
            val output_stream = this.context.contentResolver.openOutputStream(it)
            val stream_writer = OutputStreamWriter(output_stream)
            val writer = BufferedWriter(stream_writer)
            writer.write(content.to_string())

            writer.flush()
            writer.close()

            this._untrack_uri(it)
            this._track_path(it)
        }
    }
    fun get_new_file(): DocumentFile? {
        if (this.path == null) {
            throw PathNotSetException()
        }
        val working_directory = DocumentFile.fromTreeUri(this.context, this.path!!)!!
        var i = 0
        while (working_directory.findFile("opus_$i.json") != null) {
            i += 1
        }

        return working_directory.createFile("application/json", "opus_$i.json")
    }

    fun has_projects_saved(): Boolean {
        if (this.path == null) {
            return false
        }
        val working_directory = DocumentFile.fromTreeUri(this.context, this.path!!) ?: return false
        if (!working_directory.isDirectory) {
            return false
        }

        return working_directory.listFiles().isNotEmpty()
    }

    private fun generate_file_project_name(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return this.context.getString(R.string.untitled_op, now.format(formatter))
    }

    fun recache_project_list() {
        val file = File(this._cache_path)
        if (!this.has_projects_saved()) {
            if (file.exists()) {
                file.delete()
            }
            return
        }

        val working_directory = DocumentFile.fromTreeUri(this.context, this.path!!) ?: return
        val json = Json {
            ignoreUnknownKeys = true
        }

        val project_list = mutableListOf<Pair<String, String>>()
        for (json_file in working_directory.listFiles()) {
            val project_name = try {
                this.get_file_project_name(json_file.uri) ?: this.generate_file_project_name()
            } catch (e: Exception) {
                this.context.getString(R.string.corrupted_project)
            }
            project_list.add(Pair(json_file.uri.toString(), project_name))
        }

        project_list.sortBy { it.second }
        val json_string = json.encodeToString(project_list)
        file.writeText(json_string)
    }

    private fun get_file_project_name(uri: Uri): String? {
        val input_stream = this.context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText()

        reader.close()
        input_stream?.close()

        val json_obj = JSONParser.parse<JSONHashMap>(content) ?: return null
        val version = OpusManagerJSONInterface.detect_version(json_obj)
        return when (version) {
            0, 1, 2 -> json_obj.get_string("name")
            else -> {
                json_obj.get_hashmap("d").get_string("title", this.generate_file_project_name())
            }
        }
    }

    fun get_project_list(): List<Pair<Uri, String>> {
        val json = Json {
            ignoreUnknownKeys = true
        }

        if (!File(this._cache_path).exists()) {
            this.recache_project_list()
        }

        val file = File(this._cache_path)
        if (!file.exists()) {
            return listOf()
        }

        var string_content = file.readText(Charsets.UTF_8)

        // TODO: Convert to my json library
        val tmp_list: List<Pair<String, String>> = try {
            json.decodeFromString(string_content)
        } catch (_: Exception) {
            // Corruption Protection: if the cache file is bad json, delete and rebuild
            File(this._cache_path).delete()
            this.recache_project_list()
            string_content = File(this._cache_path).readText(Charsets.UTF_8)

            json.decodeFromString(string_content)
        }
        return List(tmp_list.size) { i: Int ->
            val uri = tmp_list[i].first.toUri()
            Pair(uri, tmp_list[i].second)
        }
    }

    private fun _track_path(uri: Uri) {
        val project_list = this.get_project_list().toMutableList()
        var is_tracking = false

        for ((check_path, _) in project_list) {
            if (check_path == uri) {
                is_tracking = true
                break
            }
        }

        if (is_tracking) {
            return
        }

        val project_name = this.get_file_project_name(uri) ?: return

        project_list.add(Pair(uri, project_name))
        project_list.sortBy { it.second }
        // Convert Uris to strings for storage
        val adj_project_list = List(project_list.size) { i: Int ->
             Pair(project_list[i].first.toString(), project_list[i].second)
        }

        val json = Json {
            ignoreUnknownKeys = true
        }

        val json_string = json.encodeToString(adj_project_list)
        val file = File(this._cache_path)
        file.writeText(json_string)
    }

    private fun _untrack_uri(uri: Uri) {
        val project_list = this.get_project_list().toMutableList()
        var index_to_pop = 0
        for ((check_path, _) in project_list) {
            if (check_path == uri) {
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

        // Convert Uris to strings for storage
        val adj_project_list = List(project_list.size) { i: Int ->
            Pair(project_list[i].first.toString(), project_list[i].second)
        }

        val json_string = json.encodeToString(adj_project_list)
        val file = File(this._cache_path)
        file.writeText(json_string)
    }

    fun get_active_project_name(): String? {
        return if (this.active_project == null) {
            null
        } else {
            this.get_file_project_name(this.active_project!!.uri) ?: this.context.getString(R.string.untitled_opus)
        }
    }
}
