package com.qfs.radixulous

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.radixulous.databinding.FragmentLoadBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.qfs.radixulous.opusmanager.LoadedJSONData
import java.io.File

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class LoadFragment : TempNameFragment() {
    @Serializable
    data class ProjectDirPair(var filename: String, var title: String)

    // Boiler Plate //
    private var _binding: FragmentLoadBinding? = null
    private val binding get() = _binding!!
    //////////////////

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoadBinding.inflate(inflater, container, false)
        this.get_main().lockDrawer()
        this.get_main().update_menu_options()
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        this.get_main().update_menu_options()
        this.get_main().set_title_text("Load Project")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult("RETURNED", bundleOf())
        super.onViewCreated(view, savedInstanceState)

        var loadprojectAdapter = ProjectToLoadAdapter(this)

        var rvProjectList: RecyclerView = view.findViewById(R.id.rvProjectList)
        rvProjectList.adapter = loadprojectAdapter
        rvProjectList.layoutManager = LinearLayoutManager(view.context)

        var main = this.get_main()
        var project_manager = main.project_manager

        val directory = File(project_manager.projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }

        for (json_file in directory.listFiles()!!) {
            val content = json_file.readText(Charsets.UTF_8)
            val json_obj: LoadedJSONData = Json.decodeFromString(content)
            loadprojectAdapter.addProject(
                Pair(
                    json_obj.name,
                    json_file.path
                )
            )
        }
    }

    fun load_project(path: String) {
        setFragmentResult("LOAD", bundleOf(Pair("PATH", path)))

        this.get_main().apply {
            loading_reticle()
            navTo("main")
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}