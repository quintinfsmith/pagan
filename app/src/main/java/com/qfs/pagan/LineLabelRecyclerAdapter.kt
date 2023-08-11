package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.qfs.pagan.InterfaceLayer as OpusManager

class LineLabelRecyclerAdapter(editor_table: EditorTable): RecyclerView.Adapter<LineLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    var dragging_position: Pair<Int, Int>? = null
    private var recycler: LineLabelRecyclerView
    var label_count = 0


    init {
        this.recycler = editor_table.line_label_recycler
        this.recycler.adapter = this
        this.recycler.layoutManager = TestLayoutManager(this.recycler.context, VERTICAL)
        this.recycler.itemAnimator = null

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    that.notifyItemRangeChanged(start - count, that.itemCount)
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.notifyItemRangeChanged(start - count, that.itemCount)
                }
            }
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineLabelViewHolder {


        return LineLabelViewHolder(parent.context)
    }

    override fun onViewAttachedToWindow(holder: LineLabelViewHolder) {

    }

    override fun onBindViewHolder(holder: LineLabelViewHolder, position: Int) {
        val opus_manager = this.get_opus_manager()
        val label_view = LineLabelView(holder)
        val position = label_view.get_position()
        val (channel, line_offset) = opus_manager.get_std_offset(position)
        val label = this.get_label_text(channel, line_offset)

        label_view.set_text(label)



    }

    private fun get_label_text(channel: Int, line_offset: Int): String {
        val opus_manager = this.get_opus_manager()
        return if (!opus_manager.is_percussion(channel)) {
            "$channel::$line_offset"
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            "!$instrument"
        }
    }

    override fun getItemCount(): Int {
        return this.label_count
    }

    fun scrollToLine(y: Int) {
        val current_y = this.recycler.computeVerticalScrollOffset()
        this.recycler.scrollBy(0, y - current_y)
    }

    private fun interact_lineLabel(view: LineLabelView) {
        //val rvTable = this.activity.findViewById<RecyclerView>(R.id.rvTable)
        //val adapter = rvTable.adapter as BeatColumnAdapter

        //if (adapter.linking_beat != null) {
        //    try {
        //        if (adapter.linking_beat_b == null) {
        //            this.opus_manager.link_row(
        //                view.channel,
        //                view.line_offset,
        //                adapter.linking_beat!!
        //            )
        //        } else {

        //            this.opus_manager.link_beat_range_horizontally(
        //                view.channel,
        //                view.line_offset,
        //                adapter.linking_beat!!,
        //                adapter.linking_beat_b!!
        //            )
        //        }
        //    } catch (e: Exception) {
        //        if (e is LinksLayer.MixedLinkException) {
        //            this.activity.feedback_msg("Can't Link percussion with non-percussion")
        //        } else {
        //            throw e
        //        }
        //    }
        //    adapter.cancel_linking()
        //}
        //this.opus_manager.cursor_select_row(
        //    view.channel,
        //    view.line_offset
        //)
    }

    fun set_dragging_line(channel: Int, line_offset:Int) {
        this.dragging_position = Pair(channel, line_offset)
    }
    fun is_dragging(): Boolean {
        return this.dragging_position != null
    }
    fun stop_dragging() {
        this.dragging_position = null
    }

    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }
    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun add_label(index: Int) {
        this.label_count += 1
        this.notifyItemInserted(index)
    }
    fun remove_label(index: Int) {
        this.label_count -= 1
        this.notifyItemRemoved(index)
    }
    fun clear() {
        var count = this.label_count
        this.label_count = 0
        this.notifyItemRangeRemoved(0, count)
    }
}
