package com.qfs.radixulous
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import com.qfs.radixulous.opusmanager.OpusManagerBase as OpusManager

class ProjectManager(data_dir: String) {
    val projects_dir = "$data_dir/projects/"
    val projects_list_file_path = "$data_dir/projects.json"

    fun delete(opus_manager: OpusManager) {
        val path = opus_manager.path!!

        val file = File(path)
        if (file.isFile) {
            file.delete()
        }
    }

    fun copy(opus_manager: OpusManager) {
        val old_title = opus_manager.project_name
        val new_title = "$old_title (Copy)"
        val new_path = this.get_new_path()

        opus_manager.path = new_path
        opus_manager.project_name = new_title
        opus_manager.save()
    }

    fun save(opus_manager: OpusManager) {
        opus_manager.save()
    }

    fun get_new_path(): String {
        var i = 0
        while (File("${this.projects_dir}/opus_$i.json").isFile) {
            i += 1
        }
        return "${this.projects_dir}/opus_$i.json"
    }

    fun has_projects_saved(): Boolean {
        val directory = File(this.projects_dir)
        if (!directory.isDirectory) {
            return false
        }
        return directory.listFiles().isNotEmpty()
    }
}