package com.qfs.radixulous

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.databinding.FragmentLoadBinding
import com.qfs.radixulous.opusmanager.LoadedJSONData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

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

        setFragmentResult("RETURNED", bundleOf())
        val directory = File(projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }
        var project_list: MutableList<Pair<String, String>> = mutableListOf()
        for (file_name in directory.list()!!) {
            if (!file_name.lowercase().endsWith("json")) {
                continue
            }

            val file = File("$projects_dir/$file_name")
            if (!file.isFile) {
                continue
            }

            var json_content = file.readText(Charsets.UTF_8)

            project_list.add(
                Pair(
                    Json.decodeFromString<LoadedJSONData>(json_content).project_name,
                    "$projects_dir/$file_name"
                )
            )
        }
        project_list.sortBy { it.first }
        for (name_and_path in project_list) {
            loadprojectAdapter.addProject(name_and_path)
        }
    }

    fun load_project(path: String) {
        Log.e("AAA", path)
        setFragmentResult("LOAD", bundleOf(Pair("PATH", path)))
        this.getMain().navTo("main")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}