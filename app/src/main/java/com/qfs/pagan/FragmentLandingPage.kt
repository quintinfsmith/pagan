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
import java.io.File
import kotlin.concurrent.thread

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

        btn_settings.setOnClickListener {
            this.get_activity().get_action_interface().open_settings()
        }

        btn_newProject.setOnClickListener {
            this.get_activity().get_action_interface().new_project()
        }

        val bkp_json_path = "${this.get_activity().applicationInfo.dataDir}/.bkp.json"
        if (File(bkp_json_path).exists()) {
            btn_mostRecent.setOnClickListener {
                this.setFragmentResult(IntentFragmentToken.MostRecent.name, bundleOf())
                this.get_activity().navigate(R.id.EditorFragment)
            }
        } else {
            btn_mostRecent.visibility = View.GONE
        }

        if (this.get_activity().has_projects_saved()) {
            //  KLUDGE Lockout prevents accidentally double clicking. need a better general solution,
            // but right now i  think this is the only place this is a problem
            var lockout = false
            btn_loadProject.setOnClickListener {
                if (lockout) {
                    return@setOnClickListener
                }
                lockout = true
                this.get_activity().dialog_load_project()
                thread {
                    Thread.sleep(1000)
                    lockout = false
                }
            }
            btn_loadProject.visibility = View.VISIBLE
        } else {
            btn_loadProject.visibility = View.GONE
        }

        btn_importMidi.setOnClickListener {
            this.get_activity().select_import_file()
        }

        val main = this.get_activity()
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
        this.get_activity().set_title_text("${getString(R.string.app_name)} ${getString(R.string.app_version)}")
    }
}
