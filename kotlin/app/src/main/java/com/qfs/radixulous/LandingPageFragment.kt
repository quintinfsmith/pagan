package com.qfs.radixulous

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.qfs.radixulous.databinding.FragmentLandingBinding

class LandingPageFragment : TempNameFragment() {
    // Boiler Plate //
    private var _binding: FragmentLandingBinding? = null
    private val binding get() = _binding!!
    //////////////////

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var btn_newProject = view.findViewById<View>(R.id.btnFrontNew)
        var btn_loadProject = view.findViewById<View>(R.id.btnFrontLoad)
        var btn_importMidi = view.findViewById<View>(R.id.btnFrontImport)
        var btn_importProject = view.findViewById<View>(R.id.btnFrontImportProject)
        var linkSource = view.findViewById<View>(R.id.linkSource)

        btn_importProject.setOnClickListener {
            val intent = Intent()
                .setType("application/json")
                .setAction(Intent.ACTION_GET_CONTENT)
            this.get_main().import_project_intent_launcher.launch(intent)
        }

        btn_newProject.setOnClickListener {
            this.setFragmentResult("NEW", bundleOf())
            this.get_main().navTo("main")
        }

        if (this.get_main().has_projects_saved()) {
            btn_loadProject.setOnClickListener {
                this.get_main().navTo("load")
            }
        } else {
            btn_loadProject.visibility = View.GONE
        }

        btn_importMidi.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this.get_main().import_midi_intent_launcher.launch(intent)
        }

        linkSource.setOnClickListener {
            val url = "https://burnsomni.net/git/radixulous"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}