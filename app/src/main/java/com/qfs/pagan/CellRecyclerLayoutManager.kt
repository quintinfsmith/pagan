package com.qfs.pagan

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class CellRecyclerLayoutManager(context: Context, var recycler: CellRecycler): LinearLayoutManager(context, VERTICAL, false) { }