package com.qfs.radixulous

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.qfs.radixulous.databinding.FragmentFrontBinding

class FrontFragment : Fragment() {
    private var _binding: FragmentFrontBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFrontBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var btn_newProject = view.findViewById<View>(R.id.btnFrontNew)
        var btn_loadProject = view.findViewById<View>(R.id.btnFrontLoad)
        var btn_importMidi = view.findViewById<View>(R.id.btnFrontImport)
        var linkSource = view.findViewById<View>(R.id.linkSource)

        btn_newProject.setOnClickListener {

            this.setFragmentResult("NEW", bundleOf())
            this.getMain().navTo("main")
        }
        btn_loadProject.setOnClickListener {
            this.getMain().navTo("load")
        }

        btn_importMidi.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this.getMain().import_midi_intent_launcher.launch(intent)
        }

        linkSource.setOnClickListener {
            val url = "https://burnsomni.net/git/radixulous"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun getMain(): MainActivity {
        return this.activity!! as MainActivity
    }

    override fun onStart() {
        super.onStart()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}