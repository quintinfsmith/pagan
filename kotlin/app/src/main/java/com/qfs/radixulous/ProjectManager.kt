package com.qfs.radixulous
import android.util.Xml
import androidx.navigation.findNavController
import java.io.File
import java.util.*
import com.qfs.radixulous.opusmanager.OpusManagerBase as OpusManager

class ProjectManager(var projects_dir: String) {
    fun delete(opus_manager: OpusManager) {
        var working_dir = opus_manager.get_working_dir()
        var working_file = File(working_dir)
        if (!working_file.isDirectory) {
            return
        }

        working_file.deleteRecursively()
    }
    fun rename(opus_manager: OpusManager, new_name: String) {
        var esc_name = new_name
            .replace("/", "&#47;")
            .replace("\\", "&#92;")
            .replace("\n", "")


        if (File("${this.projects_dir}/$esc_name").isDirectory) {
            throw Exception("Project '$esc_name' Exists")
        }

        var old_dir = opus_manager.get_working_dir()

        opus_manager.save( "${this.projects_dir}/$esc_name" )

        var working_file = File(old_dir)
        if (!working_file.isDirectory) {
            return
        }
        working_file.deleteRecursively()
    }

    fun copy(opus_manager: OpusManager) {
        var current_dir = opus_manager.get_working_dir()
        var i = 1
        while (File("$current_dir (Copy $i)").isDirectory) {
            i += 1
        }
        opus_manager.path = "$current_dir (Copy $i)"
        opus_manager.save()
    }


}