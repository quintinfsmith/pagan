package com.qfs.pagan.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.qfs.pagan.PaganActivity
import com.qfs.pagan.R
import com.qfs.pagan.databinding.ActivityAboutBinding
import androidx.core.net.toUri

class ActivityAbout: PaganActivity() {
    private lateinit var _binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this._binding = ActivityAboutBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)

        val toolbar = this._binding.toolbar
        toolbar.background = null

        val text_view_license = this.findViewById<TextView>(R.id.tvLicenseText)
        val stream = this.assets.open("LICENSE")

        this._binding.toolbar.title = "${this.getString(R.string.app_name)} ${this.getString(R.string.app_version)}"

        val bytes = ByteArray(stream.available())
        stream.read(bytes)
        stream.close()

        val text_body = bytes.toString(charset = Charsets.UTF_8)
        text_view_license.text = text_body

        this.findViewById<View>(R.id.linkManual).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = getString(R.string.url_manual).toUri()
            startActivity(intent)
        }

        this.findViewById<View>(R.id.linkSource).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = getString(R.string.url_git).toUri()
            startActivity(intent)
        }

        this.findViewById<View>(R.id.linkIssues).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = getString(R.string.url_issues).toUri()
            startActivity(intent)
        }

        this.findViewById<View>(R.id.linkSuggestions).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = "mailto:".toUri()
            intent.putExtra(Intent.EXTRA_EMAIL, this.getString(R.string.support_email))
            startActivity(intent);
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}