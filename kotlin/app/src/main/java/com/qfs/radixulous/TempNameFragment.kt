package com.qfs.radixulous

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.qfs.radixulous.databinding.FragmentMainBinding
import androidx.viewbinding.ViewBinding

open class TempNameFragment: Fragment() {
    internal fun get_main(): MainActivity {
        return this.activity!! as MainActivity
    }
}