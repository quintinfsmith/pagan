package com.qfs.pagan

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.PopupMenu
import com.qfs.pagan.databinding.FragmentSettingsBinding
import java.io.File

class SettingsFragment : PaganFragment() {
    // Boiler Plate //
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    //////////////////

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        this.get_main().lockDrawer()
        this.get_main().update_menu_options()
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        this.get_main().update_menu_options()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnChooseSoundFont = view.findViewById<Button>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.setOnClickListener {
            this.interact_btnChooseSoundFont(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun interact_btnChooseSoundFont(view: View) {
        val wrapper = ContextThemeWrapper(this.activity, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, view)

        val soundfont_dir = this.get_main().get_soundfont_directory()
        popupMenu.menu.add(0, 0, 0, "No Soundfont")
        var x = 1
        soundfont_dir.listFiles()?.forEachIndexed { i: Int, file: File ->
            popupMenu.menu.add(0, i + 1, i + 1, file.name)
            x += 1
        }

        popupMenu.menu.add(0, x, x, "Import...")

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                0 -> {

                }
                x -> {

                }
                else -> {

                }
            }
        }

        popupMenu.show()
    }

}