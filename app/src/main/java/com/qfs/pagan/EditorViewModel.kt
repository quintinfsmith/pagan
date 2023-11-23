package com.qfs.pagan

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.qfs.pagan.opusmanager.HistoryLayer

class EditorViewModel: ViewModel() {
    /*
     backup_fragment_intent exists BECAUSE i need to run imports on a thread
     so the user can have visual feedback BUT that fucks up if the orientation is
     changed during import or file select. This can be used backup the bundle
     so if that happens, it can be checked onResume
     */
    var backup_fragment_intent: Pair<IntentFragmentToken, Bundle?>? = null

    /*
        Otherwise the history stack gets forgotten when changing orientation, putting the app in the
        background or navigating back to the editor from the load menu
     */
    var backup_undo_stack: HistoryLayer.HistoryCache? = null
}