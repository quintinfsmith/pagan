package com.qfs.pagan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class FragmentPagan<T: ViewBinding>: Fragment() {
    // Boiler Plate //
    private var _binding: T? = null
    val binding get() = _binding!!

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

    internal fun get_activity(): MainActivity {
        return this.activity!! as MainActivity
    }

    override fun onResume() {
        super.onResume()
        this.get_activity().apply {
            update_menu_options()
            update_title_text()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        this._binding = null
    }

}