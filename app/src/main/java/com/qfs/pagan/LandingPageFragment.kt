package com.qfs.pagan

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.qfs.pagan.databinding.FragmentLandingBinding
import kotlin.concurrent.thread

class LandingPageFragment : PaganFragment() {
    // Boiler Plate //
    private var _binding: FragmentLandingBinding? = null
    private val binding get() = _binding!!
    //////////////////

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        thread {
            this.get_main().update_menu_options()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btn_newProject = view.findViewById<View>(R.id.btnFrontNew)
        val btn_loadProject = view.findViewById<View>(R.id.btnFrontLoad)
        val btn_importMidi = view.findViewById<View>(R.id.btnFrontImport)
        val btn_importProject = view.findViewById<View>(R.id.btnFrontImportProject)
        val linkSource = view.findViewById<View>(R.id.linkSource)
        val btn_linkLicense = view.findViewById<View>(R.id.linkLicense)
        val btn_linkSFLicense = view.findViewById<View>(R.id.linkSFLicense)

        btn_linkLicense.setOnClickListener {
            val stream = this.activity!!.assets.open("LICENSE")
            val bytes = ByteArray(stream.available())
            stream.read(bytes)
            stream.close()
            val text_body = bytes.toString(charset = Charsets.UTF_8)
            this.setFragmentResult(
                "LICENSE",
                bundleOf(
                    Pair("TEXT", text_body),
                    Pair("TITLE", "GPLv3")
                )
            )
            this.get_main().navTo("license")
        }

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

        var main = this.get_main()
        if (main.has_soundfont()) {
            this.binding.root.findViewById<LinearLayout>(R.id.llSFWarningLanding).visibility = View.GONE
        }  else {
            this.binding.root.findViewById<TextView>(R.id.tvFluidUrlLanding).setOnClickListener {
                val url = getString(R.string.url_fluid)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }

        if (!main.has_fluid_soundfont()) {
            btn_linkSFLicense.visibility = View.GONE
        } else {
            btn_linkSFLicense.setOnClickListener {
                val stream = this.activity!!.assets.open("SFLicense.txt")
                val bytes = ByteArray(stream.available())
                stream.read(bytes)
                stream.close()
                val text_body = bytes.toString(charset = Charsets.UTF_8)
                this.setFragmentResult(
                    "LICENSE",
                    bundleOf(
                        Pair("TEXT", text_body),
                        Pair("TITLE", "FluidR3_GM License")
                    )
                )
                this.get_main().navTo("license")
            }

        }

    }
    override fun onStart() {
        super.onStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
