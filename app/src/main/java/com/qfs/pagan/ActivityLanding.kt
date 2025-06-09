package com.qfs.pagan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.qfs.pagan.databinding.ActivityLandingBinding
import java.io.File

class ActivityLanding : PaganActivity() {
    private lateinit var _binding: ActivityLandingBinding

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        this._binding = ActivityLandingBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)
        this._binding.root.setBackgroundColor(resources.getColor(R.color.main_bg))

        val toolbar = this._binding.toolbar
        toolbar.background = null

        val btn_mostRecent = this.findViewById<View>(R.id.btnMostRecent)
        val btn_newProject = this.findViewById<View>(R.id.btnFrontNew)
        val btn_loadProject = this.findViewById<View>(R.id.btnFrontLoad)
        val btn_importMidi = this.findViewById<View>(R.id.btnFrontImport)
        val btn_settings = this.findViewById<View>(R.id.btnFrontSettings)
        val btn_about = this.findViewById<View>(R.id.btnFrontAbout)

        val bkp_json_path = "${this.applicationInfo.dataDir}/.bkp.json"
        if (File(bkp_json_path).exists()) {
            btn_mostRecent.setOnClickListener {
              //  this.setFragmentResult(IntentFragmentToken.MostRecent.name, bundleOf())
              //  this.get_activity().navigate(R.id.EditorFragment)
            }
        } else {
            btn_mostRecent.visibility = View.GONE
            val btn_index = (btn_mostRecent.parent as ViewGroup).indexOfChild(btn_mostRecent)
            // Show Space
            (btn_mostRecent.parent as ViewGroup).getChildAt(btn_index + 1)?.visibility = View.GONE
        }

        btn_settings.setOnClickListener {
            //this.get_activity().get_action_interface().open_settings()
            val intent = Intent(this, ActivitySettings::class.java)
            intent.putExtra("configuration", this.configuration.to_json().to_string())
            //this.settings_this_launcher.launch(intent)
        }


        btn_about.setOnClickListener {
            startActivity(
                Intent(this, ActivityAbout::class.java).apply {
                    putExtra("configuration", this@ActivityLanding.configuration.to_json().to_string())
                }
            )
        }

        btn_newProject.setOnClickListener {
            //this.get_action_interface().new_project()
        }


        //if (this.has_projects_saved()) {
        //    //  KLUDGE Lockout prevents accidentally double clicking. need a better general solution,
        //    // but right now i  think this is the only place this is a problem
        //    var lockout = false
        //    btn_loadProject.setOnClickListener {
        //        if (lockout) {
        //            return@setOnClickListener
        //        }
        //        lockout = true
        //        this.dialog_load_project()
        //        thread {
        //            Thread.sleep(1000)
        //            lockout = false
        //        }
        //    }
        //    btn_loadProject.visibility = View.VISIBLE
        //    this.findViewById<Space>(R.id.space_load).visibility = View.VISIBLE
        //} else {
        //    btn_loadProject.visibility = View.GONE
        //    this.findViewById<Space>(R.id.space_load).visibility = View.GONE
        //}

        btn_importMidi.setOnClickListener {
         //   this.select_import_file()
        }

        //val main = this.get_activity()
        // if (main.is_soundfont_available()) {
        //     this.binding.root.findViewById<LinearLayout>(R.id.llSFWarningLanding).visibility = View.INVISIBLE
        // }  else {
        //     this.binding.root.findViewById<TextView>(R.id.tvFluidUrlLanding).setOnClickListener {
        //         val url = getString(R.string.url_fluid)
        //         val intent = Intent(Intent.ACTION_VIEW)
        //         intent.data = Uri.parse(url)
        //         startActivity(intent)
        //     }
        // }
    }
}
