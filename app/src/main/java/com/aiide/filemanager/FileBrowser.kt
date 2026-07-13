package com.aiide.filemanager

import android.content.Context
import android.view.*
import android.widget.*
import java.io.File

class FileBrowser(private val context: Context, private var rootDir: String) {
    private var currentDir: File = File(rootDir)
    private var onFileSelected: ((File) -> Unit)? = null
    private lateinit var listView: ListView
    private lateinit var pathLabel: TextView

    fun setOnFileSelectedListener(listener: (File) -> Unit) { onFileSelected = listener }

    fun buildView(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF252526.toInt()); setPadding(8,8,8,8)
        }
        pathLabel = TextView(context).apply {
            text = currentDir.absolutePath; textSize = 12f; setTextColor(0xFFAAAAAA.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(pathLabel)
        topBar.addView(ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_rotate)
            setOnClickListener { refresh() }
        })
        layout.addView(topBar)
        listView = ListView(context)
        layout.addView(listView)
        refresh()
        return layout
    }

    fun refresh() {
        pathLabel.text = currentDir.absolutePath
        val items = mutableListOf<FileItem>()
        if (currentDir.parentFile != null) items.add(FileItem("..", true, currentDir.parentFile!!))
        val files = currentDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyArray()
        for (f in files) items.add(FileItem(f.name, f.isDirectory, f))
        listView.adapter = FileAdapter(context, items)
        listView.setOnItemClickListener { _, _, pos, _ ->
            val item = items[pos]
            if (item.isDir) { currentDir = item.file; refresh() }
            else onFileSelected?.invoke(item.file)
        }
    }

    fun setRoot(path: String) { rootDir = path; currentDir = File(path); refresh() }

    data class FileItem(val name: String, val isDir: Boolean, val file: File)
    private class FileAdapter(ctx: Context, private val items: List<FileItem>) : ArrayAdapter<FileItem>(ctx, android.R.layout.simple_list_item_1, items) {
        override fun getView(pos: Int, v: View?, parent: ViewGroup): View {
            val view = v ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
            val item = items[pos]
            view.findViewById<TextView>(android.R.id.text1).apply {
                text = "${if (item.isDir) "📁 " else getIcon(item.name)}${item.name}"
                textSize = 14f; setTextColor(if (item.isDir) 0xFF4FC3F7.toInt() else 0xFFDDDDDD.toInt())
            }
            return view
        }
        private fun getIcon(name: String) = when (name.substringAfterLast(".").lowercase()) {
            "kt","kts"->"🔷 "; "java"->"☕ "; "py"->"🐍 "; "js","ts"->"🟨 "; "html"->"🌐 "
            "css"->"🎨 "; "xml"->"📋 "; "json"->"📊 "; "md"->"📝 "; "txt"->"📄 "
            "sh","bash"->"⚡ "; "gradle"->"🛠️ "; "apk"->"📦 "; "zip"->"🗜️ "
            else -> "📄 "
        }
    }
}
