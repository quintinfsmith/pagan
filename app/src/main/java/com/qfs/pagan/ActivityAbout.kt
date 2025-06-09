package com.qfs.pagan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.qfs.pagan.databinding.ActivityAboutBinding

class ActivityAbout: PaganActivity() {
    private lateinit var _binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        this._binding = ActivityAboutBinding.inflate(this.layoutInflater)
        this.setContentView(this._binding.root)
        this.setSupportActionBar(this._binding.toolbar)
        this._binding.root.setBackgroundColor(resources.getColor(R.color.main_bg))

        val toolbar = this._binding.toolbar
        toolbar.background = null

        val tvLicenseText = this.findViewById<TextView>(R.id.tvLicenseText)
        val stream = this.assets.open("LICENSE")

        val bytes = ByteArray(stream.available())
        stream.read(bytes)
        stream.close()

        val text_body = bytes.toString(charset = Charsets.UTF_8)
        tvLicenseText.text = text_body

        val linkManual = this.findViewById<View>(R.id.linkManual)
        linkManual.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_manual))
            startActivity(intent)
        }

        val linkSource = this.findViewById<View>(R.id.linkSource)
        linkSource.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_git))
            startActivity(intent)
        }

        val linkIssues = this.findViewById<View>(R.id.linkIssues)
        linkIssues.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_issues))
            startActivity(intent)
        }
    }
}