package com.aiide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aiide.builder.ProjectBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectBuilderFragment : Fragment() {

    private lateinit var tvBuildLog: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnBuild: MaterialButton
    private lateinit var btnClean: MaterialButton
    private lateinit var projectBuilder: ProjectBuilder

    private var currentProjectPath: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_project_builder, container, false)

        tvBuildLog = view.findViewById(R.id.tv_build_log)
        chipGroup = view.findViewById(R.id.chip_build_type)
        btnBuild = view.findViewById(R.id.btn_start_build)
        btnClean = view.findViewById(R.id.btn_clean_build)

        projectBuilder = ProjectBuilder(requireContext())

        btnBuild.setOnClickListener {
            startBuild()
        }

        btnClean.setOnClickListener {
            tvBuildLog.text = ""
            Toast.makeText(context, "构建日志已清空", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    fun onProjectPathChanged(path: String) {
        currentProjectPath = path
    }

    private fun startBuild() {
        if (currentProjectPath.isEmpty()) {
            Toast.makeText(context, "请先打开项目", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedChipId = chipGroup.checkedChipId
        val buildType = if (selectedChipId != -1) {
            view?.findViewById<Chip>(selectedChipId)?.text?.toString() ?: "Android"
        } else "Android"

        appendLog("开始打包: $buildType")
        appendLog("项目路径: $currentProjectPath")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (buildType) {
                        "Android" -> projectBuilder.buildAndroid(currentProjectPath)
                        "npm" -> projectBuilder.buildNpm(currentProjectPath)
                        "Python" -> projectBuilder.buildPython(currentProjectPath)
                        "ZIP" -> projectBuilder.buildZip(currentProjectPath)
                        else -> "❌ 不支持的打包类型: $buildType"
                    }
                }
                appendLog(result)
            } catch (e: Exception) {
                appendLog("❌ 打包失败: ${e.message}")
            }
        }
    }

    private fun appendLog(message: String) {
        tvBuildLog.append(message + "\n")
        tvBuildLog.post {
            tvBuildLog.scrollTo(0, tvBuildLog.layout?.height ?: 0)
        }
    }
}
