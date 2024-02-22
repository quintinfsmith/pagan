package com.qfs.pagan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.qfs.pagan.ColorMap.Palette

abstract class FragmentPagan<T: ViewBinding>: Fragment() {
    // Boiler Plate //
    internal var _binding: T? = null
    internal val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        this._binding = this.inflate(inflater, container)
        return binding.root
    }

    abstract fun inflate(inflater: LayoutInflater, container: ViewGroup?): T

    internal fun get_main(): MainActivity {
        return this.activity!! as MainActivity
    }

    override fun onResume() {
        super.onResume()
        this.refresh_background()
        this.get_main().update_menu_options()
        this.get_main().update_title_text()
    }

    fun refresh_background() {
        val color_map = this.get_main().view_model.color_map
        this.view?.setBackgroundColor(color_map[Palette.Background])
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this._binding = null
    }

}