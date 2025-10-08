package com.qfs.pagan

import androidx.lifecycle.ViewModel
import com.qfs.pagan.projectmanager.ProjectManager

class PaganViewModel: ViewModel() {
    internal lateinit var project_manager: ProjectManager
}