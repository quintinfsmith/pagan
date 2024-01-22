package com.qfs.pagan
import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.qfs.pagan.opusmanager.HistoryCache

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
    var backup_undo_stack: HistoryCache? = null
    var coarse_x: Int = 0
    var fine_x: Int = 0
    var coarse_y: Int = 0
    var fine_y: Int = 0

    fun clear() {
        this.coarse_y = 0
        this.fine_y = 0
        this.coarse_x = 0
        this.fine_x = 0
        this.backup_fragment_intent = null
    }
}