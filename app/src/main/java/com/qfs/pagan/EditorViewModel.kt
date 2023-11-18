package com.qfs.pagan

import android.os.Bundle
import androidx.lifecycle.ViewModel

class EditorViewModel: ViewModel() {
    var coarse_x: Int? = null
    var coarse_y: Int? = null
    var fine_x: Int? = null
    var fine_y: Int? = null

    /*
     backup_fragment_intent exists BECAUSE i need to run imports on a thread
     so the user can have visual feedback BUT that fucks up if the orientation is
     changed during import or file select. This can be used backup the bundle
     so if that happens, it can be checked onResume
     */
    var backup_fragment_intent: Pair<IntentFragmentToken, Bundle?>? = null
}