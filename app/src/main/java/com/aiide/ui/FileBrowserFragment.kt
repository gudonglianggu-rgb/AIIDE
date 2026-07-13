package com.aiide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiide.filemanager.FileBrowser
import com.google.android.material.button.MaterialButton
import java.io.File

class FileBrowserFragment : Fragment() {

    private lateinit var tvCurrentPath: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnGoUp: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var fileBrowser: FileBrowser
    private var currentPath: String = "/"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_file_browser, container, false)

        tvCurrentPath = view.findViewById(R.id.tv_current_path)
        recyclerView = view.findViewById(R.id.rv_file_list)
        btnGoUp = view.findViewById(R.id.btn_go_up)
        btnRefresh = view.findViewById(R.id.btn_refresh)

        fileBrowser = FileBrowser(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(context)

        btnGoUp.setOnClickListener {
            navigateUp()
        }

        btnRefresh.setOnClickListener {
            loadDirectory(currentPath)
        }

        // 默认加载根目录
        loadDirectory("/")

        return view
    }

    fun onProjectPathChanged(path: String) {
        // 当项目路径改变时，自动切换到项目目录
        loadDirectory(path)
    }

    private fun loadDirectory(path: String) {
        currentPath = path
        tvCurrentPath.text = path

        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            Toast.makeText(context, "目录不存在", Toast.LENGTH_SHORT).show()
            return
        }

        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyArray()
        val adapter = FileListAdapter(files) { file ->
            if (file.isDirectory) {
                loadDirectory(file.absolutePath)
            } else {
                // 点击文件时通知编辑器
                Toast.makeText(context, "选中: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun navigateUp() {
        val parent = File(currentPath).parent
        if (parent != null) {
            loadDirectory(parent)
        } else {
            Toast.makeText(context, "已在根目录", Toast.LENGTH_SHORT).show()
        }
    }
}

class FileListAdapter(
    private val files: Array<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val icon = if (file.isDirectory) "📁 " else FileBrowser.getFileIcon(file.name)
        holder.textView.text = "$icon${file.name}"
        holder.itemView.setOnClickListener { onItemClick(file) }
    }

    override fun getItemCount(): Int = files.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }
}
