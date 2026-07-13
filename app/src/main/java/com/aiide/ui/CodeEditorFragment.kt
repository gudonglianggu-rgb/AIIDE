package com.aiide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.io.File

class CodeEditorFragment : Fragment() {

    private lateinit var etFileName: EditText
    private lateinit var etCode: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnOpen: MaterialButton

    private var currentProjectPath: String = ""
    private var currentFilePath: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_code_editor, container, false)

        etFileName = view.findViewById(R.id.et_file_name)
        etCode = view.findViewById(R.id.et_code_content)
        btnSave = view.findViewById(R.id.btn_save_file)
        btnOpen = view.findViewById(R.id.btn_open_file)

        btnSave.setOnClickListener {
            saveCurrentFile()
        }

        btnOpen.setOnClickListener {
            openFileFromPath()
        }

        return view
    }

    fun onProjectPathChanged(path: String) {
        currentProjectPath = path
    }

    private fun saveCurrentFile() {
        val fileName = etFileName.text.toString().trim()
        val content = etCode.text.toString()

        if (fileName.isEmpty()) {
            Toast.makeText(context, "请输入文件名", Toast.LENGTH_SHORT).show()
            return
        }

        val file = if (fileName.startsWith("/")) {
            File(fileName)
        } else {
            if (currentProjectPath.isEmpty()) {
                Toast.makeText(context, "请先打开项目", Toast.LENGTH_SHORT).show()
                return
            }
            File(currentProjectPath, fileName)
        }

        try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            currentFilePath = file.absolutePath
            Toast.makeText(context, "已保存: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileFromPath() {
        val path = etFileName.text.toString().trim()
        if (path.isEmpty()) {
            Toast.makeText(context, "请输入文件路径", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            etCode.setText(file.readText())
            etFileName.setText(file.absolutePath)
            currentFilePath = file.absolutePath
            Toast.makeText(context, "已打开: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getCurrentCode(): String = etCode.text.toString()
    fun setCode(content: String) {
        etCode.setText(content)
    }
    fun getCurrentFilePath(): String = currentFilePath

    companion object {
        val TAG: String = CodeEditorFragment::class.java.simpleName
    }
}
