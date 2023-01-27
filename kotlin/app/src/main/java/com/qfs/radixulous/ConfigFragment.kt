package com.qfs.radixulous

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.qfs.radixulous.databinding.FragmentConfigBinding
import kotlinx.android.synthetic.main.channel_ctrl.view.*
import kotlinx.android.synthetic.main.fragment_load.view.*
import java.io.File

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
        main.getOpusManager().get_working_dir()?.let { name: String ->
            main.set_title_text(
                "${name.substring(name.lastIndexOf("/") + 1)}"
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResult("RETURNED", bundleOf())
        var main = this.getMain()

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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}