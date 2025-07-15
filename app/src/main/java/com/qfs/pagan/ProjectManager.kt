package com.qfs.pagan
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONParser
import com.qfs.pagan.jsoninterfaces.OpusManagerJSONInterface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.Document
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.qfs.pagan.opusmanager.OpusLayerBase as OpusManager
class ProjectManager(val context: Context, var uri: Uri?) {
    class MKDirFailedException(dir: String): Exception("Failed to create directory $dir")
    class InvalidDirectory(path: Uri): Exception("Real Directory Required ($path)")
    class PathNotSetException(): Exception("Projects path has not been set.")
    private val _cache_path = "${context.applicationContext.dataDir}/project_list.json"
    private val bkp_path = "${this.context.applicationInfo.dataDir}/.bkp.json"
    private val bkp_path_path = "${this.context.applicationInfo.dataDir}/.bkp_path"

    init {
        this.recache_project_list()
    }

    /**
     * Move files from [old_uri] to the ProjectManger's current [uri],
     * return the new uri associated with given [active_project_uri] if it was moved
     **/
    fun move_old_projects_directory(old_uri: Uri, active_project_uri: Uri? = null): Uri? {
        val old_directory = DocumentFile.fromTreeUri(this.context, old_uri) ?: return null
        if (!old_directory.isDirectory || old_uri == this.uri) {
            return null
        }

        var output: Uri? = null
        for (project in old_directory.listFiles()) {
            if (!project.isFile) {
                continue
            }
            // Use new path instead of copying file name to avoid collisions
            val new_uri = this.get_new_file_uri() ?: continue
            val output_stream = this.context.contentResolver.openOutputStream(new_uri, "wt")
            val input_stream = this.context.contentResolver.openInputStream(project.uri)

            if (project.uri == active_project_uri) {
                output = new_uri
            }

            val buffer = ByteArray(4096)
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

            project.delete()
        }

        this.recache_project_list()
        return output
    }

    fun change_project_path(new_uri: Uri, active_project_uri: Uri? = null): Uri? {
        val new_directory = DocumentFile.fromTreeUri(this.context, new_uri)
        if (new_directory == null || !new_directory.isDirectory) {
            throw InvalidDirectory(new_uri)
        }

        this.ucheck_update_move_project_files(active_project_uri)

        val old_uri = this.uri
        this.uri = new_uri
        val output = if (old_uri != null) {
            this.move_old_projects_directory(old_uri, active_project_uri)
        } else {
            null
        }

        this.recache_project_list()
        return output
    }

    fun contains(uri: Uri): Boolean {
        if (this.uri == null) {
            return false
        }

        val parent_segments = this.uri!!.pathSegments.last().split("/")
        val child_segments = uri.pathSegments.last().split("/")

        val output = uri.authority == this.uri?.authority && child_segments.size > parent_segments.size  && child_segments.subList(0, parent_segments.size) == parent_segments
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

        opus_manager.project_name = new_title
    }

    fun save(opus_manager: OpusManager, uri: Uri?): Uri {
        val active_project_uri = uri ?: this.get_new_file_uri() ?: throw Exception("Failed To create new file")
        // Untrack then track in order to update the project title in the cache
        active_project_uri.let {
            val content = opus_manager.to_json()

            this.context.contentResolver.openOutputStream(it, "wt")?.let { output_stream ->
                output_stream.write(content.to_string().toByteArray(Charsets.UTF_8))
                output_stream.flush()
                output_stream.close()
            }

            this._untrack_uri(it)
            this._track_path(it)
        }

        return active_project_uri
    }
    fun get_new_file_uri(): Uri? {
        if (this.uri == null) {
            throw PathNotSetException()
        }
        val working_directory = DocumentFile.fromTreeUri(this.context, this.uri!!)!!
        var i = 0
        while (working_directory.findFile("opus_$i.json") != null) {
            i += 1
        }

        return working_directory.createFile("application/json", "opus_$i.json")?.uri
    }

    fun has_projects_saved(): Boolean {
        if (this.uri == null) {
            return false
        }

        val working_directory = DocumentFile.fromTreeUri(this.context, this.uri!!) ?: return false
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

    fun has_external_storage_projects(): Boolean {
        // V1.7.7, moved project storage out of ExternalFilesDir
        val external_path = this.context.getExternalFilesDir(null) ?: return false
        val old_directory = File("$external_path/projects")
        return old_directory.isDirectory && old_directory.listFiles()?.isNotEmpty() ?: false
    }

    fun ucheck_recache_external_storage_projects(): Boolean {
        // V1.7.7, moved project storage out of ExternalFilesDir
        val external_path = this.context.getExternalFilesDir(null) ?: return false
        val old_directory = File("$external_path/projects")
        if (!old_directory.isDirectory || old_directory.listFiles()?.isEmpty() ?: false) {
            return false
        }

        val working_directory = DocumentFile.fromFile(old_directory)
        val json = Json {
            ignoreUnknownKeys = true
        }

        val project_list = mutableListOf<Pair<String, String>>()
        for (json_file in working_directory.listFiles()) {

            val project_name = try {
                this.get_file_project_name(json_file.uri) ?: this.generate_file_project_name()
            } catch (e: Exception) {
                continue
            }
            project_list.add(Pair(json_file.uri.toString(), project_name))
        }

        project_list.sortBy { it.second }
        val json_string = json.encodeToString(project_list)
        val file = File(this._cache_path)
        file.writeText(json_string)
        return true
    }

    fun recache_project_list() {
        val file = File(this._cache_path)
        if (this.ucheck_recache_external_storage_projects()) {
            return
        }

        if (!this.has_projects_saved()) {
            if (file.exists()) {
                file.delete()
            }
            return
        }

        val working_directory = DocumentFile.fromTreeUri(this.context, this.uri!!) ?: return
        val json = Json {
            ignoreUnknownKeys = true
        }

        val project_list = mutableListOf<Pair<String, String>>()
        for (json_file in working_directory.listFiles()) {
            val project_name = try {
                this.get_file_project_name(json_file.uri) ?: this.generate_file_project_name()
            } catch (e: Exception) {
                continue
            }
            project_list.add(Pair(json_file.uri.toString(), project_name))
        }

        project_list.sortBy { it.second }
        val json_string = json.encodeToString(project_list)
        file.writeText(json_string)
    }

    fun get_file_project_name(uri: Uri): String? {
        val input_stream = this.context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(input_stream))
        val content = reader.readText()

        reader.close()

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

    // v1.7.7: Using custom projects directories rather than forcing external directory
    fun ucheck_update_move_project_files(active_uri: Uri? = null): Uri? {
        val bkp_path_file = File(this.bkp_path_path)
        val bkp_uri = if (!bkp_path_file.exists()) {
            null
        } else {
            bkp_path_file.readText().toUri()
        }


        var output: Uri? = null
        this.context.getExternalFilesDir(null)?.let {
            val old_directory = File("$it/projects")
            if (!old_directory.isDirectory) {
                return null
            }

            for (project in old_directory.listFiles()) {
                // Use new path instead of copying file name to avoid collisions
                val new_uri = this.get_new_file_uri() ?: continue
                val output_stream = this.context.contentResolver.openOutputStream(new_uri, "wt")
                val input_stream = project.inputStream()

                // Necessary for when user goes into settings, changes project directory, then leaves the app for a while
                // and the context is lost. When the project is loaded from bkp, the reloaded active project would OTHERWISE be incorrect
                when (project.toUri()) {
                    active_uri -> { output = new_uri }
                    bkp_uri -> { bkp_path_file.writeText(new_uri.toString()) }
                    else -> { }
                }

                val buffer = ByteArray(4096)
                while (true) {
                    val read_size = input_stream.read(buffer)
                    if (read_size == -1) {
                        break
                    }
                    output_stream?.write(buffer, 0, read_size)
                }

                input_stream.close()
                output_stream?.flush()
                output_stream?.close()
            }

            this.recache_project_list()
            old_directory.deleteRecursively()
        }

        return output
    }


    fun save_to_backup(opus_manager: OpusLayerInterface, uri: Uri?) {
        val path_file = File(this.bkp_path_path)
        if (uri == null) {
            if (path_file.exists()) {
                path_file.delete()
            }
        } else {
            path_file.writeText(uri.toString())
        }

        File(this.bkp_path).writeText(opus_manager.to_json().to_string())
    }

    fun read_backup(): Pair<Uri?, ByteArray> {
        val path_file = File(this.bkp_path_path)
        return Pair(
            if (path_file.exists()) {
                val uri = path_file.readText().toUri()
                if (DocumentFile.fromSingleUri(this.context, uri)?.exists() == true) {
                    uri
                } else {
                    null
                }
            } else {
                null
           },
            FileInputStream(this.bkp_path).readBytes()
        )
    }

    fun delete_backup() {
        File(this.bkp_path).let { file ->
            if (file.exists()) {
                file.delete()
            }
        }

        File(this.bkp_path_path).let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun has_backup_saved(): Boolean {
        return File(this.bkp_path).exists()
    }
}
