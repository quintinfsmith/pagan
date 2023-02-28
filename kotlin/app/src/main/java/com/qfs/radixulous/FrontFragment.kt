package com.qfs.radixulous

import android.os.Bundle
import android.view.*
import android.widget.Button
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
        var btn_newProject = view.findViewById<Button>(R.id.btnFrontNew)
        var btn_loadProject = view.findViewById<Button>(R.id.btnFrontLoad)

        btn_newProject.setOnClickListener {
            this.getMain().navTo("main")
        }
        btn_loadProject.setOnClickListener {
            this.getMain().navTo("load")
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