package com.qfs.radixulous

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.qfs.radixulous.databinding.FragmentLoadBinding
import kotlinx.android.synthetic.main.fragment_load.view.*
import java.io.File

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class LoadFragment : Fragment() {

    private var _binding: FragmentLoadBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoadBinding.inflate(inflater, container, false)
        return binding.root

    }
    private fun getMain(): MainActivity {
        return this.activity!! as MainActivity
    }

    override fun onStart() {
        super.onStart()
        this.getMain().update_menu_options()
        this.getMain().set_title_text("Load Project")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Find way to use relative path
        var projects_dir = "/data/data/com.qfs.radixulous/projects"

        setFragmentResult("RETURNED", bundleOf())
        val directory = File(projects_dir)
        if (!directory.isDirectory) {
            if (! directory.mkdirs()) {
                throw Exception("Could not make directory")
            }
        }

        for (file_name in directory.list()!!) {
            val file = File("$projects_dir/$file_name")
            if (!file.isDirectory) {
                continue
            }

            // TODO: Check if directory is project directory
            val row = LayoutInflater.from(view.svProjectList.llProjectList.context).inflate(
                R.layout.loadmenu_item,
                view.svProjectList.llProjectList,
                false
            ) as ViewGroup

            (row.getChildAt(0) as TextView).text = file_name

            row.setOnClickListener {
                // TODO: Show loading reticule
                setFragmentResult("LOAD", bundleOf(Pair("PATH", "$projects_dir/$file_name")))
                findNavController().navigate(R.id.action_LoadFragment_to_MainFragment)
            }

            view.svProjectList.llProjectList.addView(row)
        }

        //binding.buttonLoad.setOnClickListener {
        //    findNavController().navigate(R.id.action_LoadFragment_to_FirstFragment)
        //}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}