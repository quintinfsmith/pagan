package com.qfs.pagan

import androidx.fragment.app.Fragment

open class PaganFragment: Fragment() {
    internal fun get_main(): MainActivity {
        return this.activity!! as MainActivity
    }
}