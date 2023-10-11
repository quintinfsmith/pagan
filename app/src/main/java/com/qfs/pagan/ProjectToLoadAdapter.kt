package com.qfs.pagan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectToLoadAdapter(
    private val _load_fragment: LoadFragment
) : RecyclerView.Adapter<ProjectToLoadAdapter.ProjectToLoadViewHolder>() {
    class ProjectToLoadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val _projects: MutableList<Pair<String, String>> = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectToLoadViewHolder {
        return ProjectToLoadViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.loadmenu_item,
                parent,
                false
            )
        )
    }

    fun addProject(title_and_path: Pair<String, String>) {
        this._projects.add(title_and_path)
        notifyItemInserted(this._projects.size - 1)
    }

    override fun onBindViewHolder(holder: ProjectToLoadViewHolder, position: Int) {
        val (title, path) = this._projects[position]
        val tvProjectLabel: TextView = holder.itemView.findViewById(R.id.tvProjectLabel)
        tvProjectLabel.text = title

        tvProjectLabel.setOnClickListener {
            this._load_fragment.load_project(path)
        }
    }

    override fun getItemCount(): Int {
        return this._projects.size
    }
}

