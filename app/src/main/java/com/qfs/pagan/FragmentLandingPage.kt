package com.qfs.pagan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.qfs.pagan.databinding.FragmentLandingBinding
import kotlin.concurrent.thread
import java.io.File

class FragmentLandingPage : FragmentPagan<FragmentLandingBinding>() {
    override fun inflate( inflater: LayoutInflater, container: ViewGroup?): FragmentLandingBinding {
        return FragmentLandingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn_mostRecent = view.findViewById<View>(R.id.btnMostRecent)
        val btn_newProject = view.findViewById<View>(R.id.btnFrontNew)
        val btn_loadProject = view.findViewById<View>(R.id.btnFrontLoad)
        val btn_importMidi = view.findViewById<View>(R.id.btnFrontImport)
        val btn_settings = view.findViewById<View>(R.id.btnFrontSettings)
        val linkSource = view.findViewById<TextView>(R.id.linkSource)
        val btn_linkLicense = view.findViewById<TextView>(R.id.linkLicense)

        btn_settings.setOnClickListener {
            this.get_main().navigate(R.id.SettingsFragment)
        }

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
            this.get_main().navigate(R.id.LicenseFragment)
        }

        btn_newProject.setOnClickListener {
            this.setFragmentResult(IntentFragmentToken.New.name, bundleOf())
            this.get_main().navigate(R.id.EditorFragment)
        }

        val bkp_json_path = "${this.get_main().applicationInfo.dataDir}/.bkp.json"
        if (File(bkp_json_path).exists()) {
            btn_mostRecent.setOnClickListener {
                this.setFragmentResult(IntentFragmentToken.MostRecent.name, bundleOf())
                this.get_main().navigate(R.id.EditorFragment)
            }
        } else {
            btn_mostRecent.visibility = View.GONE
        }

        if (this.get_main().has_projects_saved()) {
            //  KLUDGE Lockout prevents accidentally double clicking. need a better general solution,
            // but right now i  think this is the only place this is a problem
            var lockout = false
            btn_loadProject.setOnClickListener {
                if (lockout) {
                    return@setOnClickListener
                }
                lockout = true
                this.get_main().dialog_load_project()
                thread {
                    Thread.sleep(1000)
                    lockout = false
                }
            }

            btn_loadProject.setOnLongClickListener {
                this.get_main().select_project_file()
                true
            }
        } else {
            btn_loadProject.setOnClickListener {
                this.get_main().select_project_file()
            }
        }

        btn_importMidi.setOnClickListener {
            this.get_main().select_midi_file()
        }

        linkSource.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_git))
            startActivity(intent)
        }

        val main = this.get_main()
        if (main.is_soundfont_available()) {
            this.binding.root.findViewById<LinearLayout>(R.id.llSFWarningLanding).visibility = View.GONE
        }  else {
            this.binding.root.findViewById<TextView>(R.id.tvFluidUrlLanding).setOnClickListener {
                val url = getString(R.string.url_fluid)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.get_main().set_title_text("${getString(R.string.app_name)} ${getString(R.string.app_version)}")
    }
}
