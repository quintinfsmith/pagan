package com.qfs.apres.soundfont

import android.util.Log

data class Modulator(
    var sfModSrcOper: Int,
    var sfModDestOper: Int,
    var modAmount: Int,
    var sfModAmtSrcOper: Int,
    var sfModTransOper: Int
) {

    init {
        Log.d("AAA", "INIT MODULATOR: $sfModSrcOper")

        if (sfModDestOper and 0x0800 != 0) {
            Log.d("AAA", "DestOper Link: $sfModSrcOper")
        } else {
            Log.d("AAA", "DestOper: $sfModSrcOper")
        }

        Log.d("AAA", "Mod Amount: $modAmount")
        Log.d("AAA", "Source Mod Amount: $modAmount")
        Log.d("AAA", "Source Mod Amount: $sfModAmtSrcOper")
        Log.d("AAA", "Source Trans Oper: $sfModTransOper")
        Log.d("AAA", " ------")

    }
}
