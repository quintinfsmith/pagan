package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuControlLeafB(context: Context, attrs: AttributeSet? = null): ContextMenuView(R.layout.contextmenu_line_ctl_leaf_b, context, attrs) {
    lateinit var button_erase: ButtonIcon
    lateinit var label: PaganTextView
    override fun init_properties() {
        super.init_properties()
        this.button_erase = this.findViewById(R.id.btnEraseSelection)
        this.label = this.findViewById(R.id.tvLinkLabel)
    }

    override fun setup_interactions() {
        this.button_erase.setOnClickListener {
            if (it.isEnabled) {
                this.get_opus_manager().unset()
            }
        }
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        this.button_erase.isEnabled = opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range
    }
}