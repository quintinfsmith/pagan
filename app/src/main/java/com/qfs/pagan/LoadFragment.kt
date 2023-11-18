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
import com.qfs.pagan.opusmanager.LoadedJSONData0
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class LoadFragment : PaganFragment<FragmentLoadBinding>() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.get_main().drawer_lock()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentLoadBinding {
        return FragmentLoadBinding.inflate(inflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        this.get_main().set_title_text(getString(R.string.load_fragment_label))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult("RETURNED", bundleOf())
        super.onViewCreated(view, savedInstanceState)

        val rvProjectList: RecyclerView = view.findViewById(R.id.rvProjectList)
        rvProjectList.adapter = ProjectToLoadAdapter(this)
        rvProjectList.layoutManager = LinearLayoutManager(view.context)

        val main = this.get_main()

        for (json_file in main.get_project_directory().listFiles()!!) {
            val content = json_file.readText(Charsets.UTF_8)
            val json_obj: LoadedJSONData = try {
                Json.decodeFromString(content)
            } catch (e: Exception) {
                val old_data = Json.decodeFromString<LoadedJSONData0>(content)
                main.get_opus_manager().convert_old_fmt(old_data)
            }
            (rvProjectList.adapter as ProjectToLoadAdapter).addProject(Pair(json_obj.name, json_file.path))
        }

    }

    fun load_project(path: String) {
        setFragmentResult(IntentFragmentToken.Load.name, bundleOf(Pair("PATH", path)))
        this.get_main().navigate(R.id.EditorFragment)
    }

}