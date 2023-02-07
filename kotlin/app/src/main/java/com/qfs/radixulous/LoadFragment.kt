package com.qfs.radixulous

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.databinding.FragmentLoadBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ProjectDirPair(var filename: String, var title: String)

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class LoadFragment : Fragment() {
    private var _binding: FragmentLoadBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoadBinding.inflate(inflater, container, false)
        return binding.root

    }
    private fun getMain(): MainActivity {
        return this.activity!! as MainActivity
    }

    override fun onStart() {
        super.onStart()
        this.getMain().update_menu_options()
        this.getMain().set_title_text("Load Project")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var loadprojectAdapter = ProjectToLoadAdapter(this)
        var rvProjectList: RecyclerView = view.findViewById(R.id.rvProjectList)

        rvProjectList.adapter = loadprojectAdapter
        rvProjectList.layoutManager = LinearLayoutManager(view.context)

        // TODO: Find way to use relative path
        var projects_dir = "/data/data/com.qfs.radixulous/projects"
        var projects_list_file_path = "/data/data/com.qfs.radixulous/projects.json"

        setFragmentResult("RETURNED", bundleOf())
        val directory = File(projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }

        var project_list_file = File(projects_list_file_path)
        if (project_list_file.isFile) {
            var content = project_list_file.readText(Charsets.UTF_8)
            var json_project_list: MutableList<ProjectDirPair> = Json.decodeFromString(content)

            json_project_list.sortBy { it.title }

            for (obj in json_project_list) {
                loadprojectAdapter.addProject(
                    Pair(
                        obj.title,
                        "$projects_dir/${obj.filename}"
                    )
                )
            }
        }
    }


    fun load_project(path: String, title: String) {
        setFragmentResult("LOAD", bundleOf(Pair("PATH", path)))
        this.getMain().set_current_project_title(title)
        this.getMain().navTo("main")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}