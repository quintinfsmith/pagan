package com.qfs.radixulous

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.chip.Chip
import com.qfs.radixulous.databinding.FragmentConfigBinding
import kotlinx.android.synthetic.main.channel_ctrl.view.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root

    }
    private fun getMain(): MainActivity {
        return this.activity!! as MainActivity
    }

    override fun onStart() {
        super.onStart()
        var main = this.getMain()
        main.update_menu_options()
        main.update_title_text()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResult("RETURNED", bundleOf())
        var main = this.getMain()
        var tvChangeProjectName: TextView = view.findViewById(R.id.tvChangeProjectName)
        tvChangeProjectName.setOnClickListener {
            this.change_name_dialog()
        }


        var etTempo: EditText = view.findViewById(R.id.etTempo)
        etTempo.setText(main.getOpusManager().tempo.toString())
        etTempo.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //TODO("Not yet implemented")
            }

            override fun afterTextChanged(editable: Editable?) {
                try {
                    main.getOpusManager().tempo = editable.toString().toFloat()
                } catch (exception: Exception) {

                }
            }
        })

        var opus_manager = this.getMain().getOpusManager()

        for (i in 0 until opus_manager.channel_lines.size) {
            val chipView = Chip((this.getMain().findViewById(R.id.cgEnabledChannels) as View).context)
            chipView.isCheckable = true
            if (i == 9) {
                chipView.text = "Drums"
            } else {
                chipView.text = "$i"
            }
            chipView.isChecked = opus_manager.channel_lines[i].isNotEmpty()

            // TODO: I suspect there is a better listener for this
            chipView.setOnClickListener {
                if (chipView.isChecked) {
                    if (opus_manager.channel_lines[i].isEmpty()) {
                        opus_manager.add_channel(i)
                        //this.tick()
                    }
                } else {
                    val line_count = opus_manager.channel_lines[i].size
                    if (opus_manager.line_count() > line_count) {
                        opus_manager.remove_channel(i)
                        //this.tick()
                    } else {
                        chipView.isChecked = true
                    }
                }
            }
            view.llB.cgEnabledChannels.addView(chipView)
        }

        for (i in 0 until opus_manager.channel_lines.size) {
            if (opus_manager.channel_lines[i].isEmpty()) {
                continue
            }
        }

        (view.findViewById(R.id.btnExportProject) as TextView).setOnClickListener {
            main.export_midi()
        }

        (view.findViewById(R.id.btnDeleteProject) as TextView).setOnClickListener {
            // TODO: Warning dialog
            main.delete_project()
            // TODO: Toast Feedback
        }
        (view.findViewById(R.id.btnCopyProject) as TextView).setOnClickListener {
            main.copy_project()
            // TODO: Toast Feedback
        }
    }

    private fun change_name_dialog() {
        var main = this.getMain()
        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.text_name_change, view as ViewGroup?, false)
        val input: EditText = viewInflated.findViewById(R.id.etProjectName)
        main.getOpusManager().get_working_dir()?.let {
            input.setText(
                it.substring(it.lastIndexOf("/") + 1)
                    .replace("&#47;", "/")
                    .replace("&#92;", "\\")
            )
        }

        AlertDialog.Builder(context).apply {
            setTitle("Change Project Name")
            setView(viewInflated)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                main.rename_project(input.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}