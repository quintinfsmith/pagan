package com.qfs.pagan

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CellRecyclerLayoutManager(context: Context, var recycler: CellRecycler): LinearLayoutManager(context, VERTICAL, false) { }