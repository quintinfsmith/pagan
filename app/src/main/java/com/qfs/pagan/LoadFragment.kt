package com.qfs.pagan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.databinding.FragmentLoadBinding
import com.qfs.pagan.opusmanager.LoadedJSONData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class LoadFragment : PaganFragment() {
    class MKDirFailedException(dir: String): Exception("Failed to create directory $dir")

    // Boiler Plate //
    private var _binding: FragmentLoadBinding? = null
    private val binding get() = _binding!!
    //////////////////

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadBinding.inflate(inflater, container, false)
        this.get_main().lockDrawer()
        this.get_main().update_menu_options()
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        this.get_main().update_menu_options()
        this.get_main().set_title_text(getString(R.string.load_fragment_label))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult("RETURNED", bundleOf())
        super.onViewCreated(view, savedInstanceState)

        val loadprojectAdapter = ProjectToLoadAdapter(this)

        val rvProjectList: RecyclerView = view.findViewById(R.id.rvProjectList)
        rvProjectList.adapter = loadprojectAdapter
        rvProjectList.layoutManager = LinearLayoutManager(view.context)

        val main = this.get_main()
        val project_manager = main.project_manager

        val directory = File(project_manager.projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw MKDirFailedException(project_manager.projects_dir)
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
        setFragmentResult(IntentFragmentToken.Load.name, bundleOf(Pair("PATH", path)))

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