package com.qfs.pagan
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.qfs.pagan.databinding.FragmentLicenseBinding

class FragmentLicense: FragmentPagan<FragmentLicenseBinding>() {
    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentLicenseBinding {
        return FragmentLicenseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvLicenseText = view.findViewById<TextView>(R.id.tvLicenseText)
        val stream = this.requireActivity().assets.open("LICENSE")
        val bytes = ByteArray(stream.available())
        stream.read(bytes)
        stream.close()
        val text_body = bytes.toString(charset = Charsets.UTF_8)
        tvLicenseText.text = text_body

        val linkManual = view.findViewById<View>(R.id.linkManual)
        linkManual.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_manual))
            startActivity(intent)
        }

        val linkSource = view.findViewById<View>(R.id.linkSource)
        linkSource.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_git))
            startActivity(intent)
        }

        val linkIssues = view.findViewById<View>(R.id.linkIssues)
        linkIssues.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.url_issues))
            startActivity(intent)
        }

    }
}
