package com.qfs.pagan
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.setFragmentResultListener
import com.qfs.pagan.databinding.FragmentLicenseBinding

class FragmentLicense: FragmentPagan<FragmentLicenseBinding>() {
    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentLicenseBinding {
        return FragmentLicenseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvLicenseText = view.findViewById<TextView>(R.id.tvLicenseText)

        setFragmentResultListener("LICENSE") { _, bundle: Bundle? ->
            if (bundle == null) {
                return@setFragmentResultListener
            }

            bundle.getString("TEXT")?.let { text: String ->
                tvLicenseText.text = text
            }
            bundle.getString("TITLE")?.let { text: String ->
                (this.activity as MainActivity).set_title_text(text)
            }
        }
    }
}
