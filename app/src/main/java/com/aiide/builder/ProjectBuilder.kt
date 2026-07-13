package com.aiide.builder

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import java.io.File

class ProjectBuilder(private val context: Context, private val projectDir: String) {
    private lateinit var outputView: TextView
    private lateinit var buildBtn: Button
    private var isBuilding = false

    fun buildView(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(16,16,16,16)
        }
        layout.addView(TextView(context).apply { text = "🔧 项目打包工具"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt()) })

        val btnRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        buildBtn = Button(context).apply { text = "▶ 打包项目"; setOnClickListener { buildProject() } }
        btnRow.addView(buildBtn)
        btnRow.addView(Button(context).apply { text = "🧹 清理"; setOnClickListener { cleanBuild() } })
        btnRow.addView(Button(context).apply { text = "📲 安装APK"; setOnClickListener { installApk() } })
        layout.addView(btnRow)

        layout.addView(TextView(context).apply { text = "构建输出:"; setTextColor(0xFFAAAAAA.toId()); setPadding(0,8,0,4) })

        val scroll = ScrollView(context).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        outputView = TextView(context).apply {
            textSize = 11f; typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(0xFF00FF00.toInt()); setBackgroundColor(0xFF1A1A1A.toInt()); setPadding(8,8,8,8)
        }
        scroll.addView(outputView)
        layout.addView(scroll)

        layout.addView(TextView(context).apply {
            text = "\n内置打包工具:\n• Python PyInstaller\n• Node.js pkg\n• Android Gradle\n• ZIP压缩\n• Shell tar/gzip"
            textSize = 13f; setTextColor(0xFF888888.toInt())
        })
        return layout
    }

    private fun log(msg: String) { outputView.append("$msg\n") }

    private fun buildProject() {
        if (isBuilding) return; isBuilding = true; buildBtn.isEnabled = false; outputView.text = ""
        log("🚀 开始构建项目...")
        Thread {
            try {
                val project = File(projectDir)
                val hasGradle = project.walkTopDown().any { it.name == "build.gradle.kts" || it.name == "build.gradle" }
                val hasPy = project.walkTopDown().any { it.extension == "py" && it.name != "__init__.py" }
                val hasPkgJson = File(project, "package.json").exists()
                when {
                    hasGradle -> log("📱 Android Gradle项目\n⚠️ 需要Android SDK\n💡 使用命令: gradle assembleDebug")
                    hasPkgJson -> { log("⬡ Node.js项目"); runCmd(arrayOf("npm","pack"), project) }
                    hasPy -> { log("🐍 Python项目"); val main = project.walkTopDown().firstOrNull { it.name == "main.py" || it.name == "app.py" }; if (main != null) runCmd(arrayOf("python3","-m","zipapp",main.name,"-o","${project.name}.pyz"), project) else log("⚠️ 未找到main.py/app.py") }
                    else -> { log("📦 创建ZIP归档..."); runCmd(arrayOf("zip","-r","${project.name}.zip","."), project) }
                }
                log("✅ 构建完成")
            } catch (e: Exception) { log("❌ 构建失败: ${e.message}") }
            finally { isBuilding = false; Handler(Looper.getMainLooper()).post { buildBtn.isEnabled = true } }
        }.start()
    }

    private fun runCmd(cmd: Array<String>, dir: File) {
        try {
            val p = ProcessBuilder(*cmd).directory(dir).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().forEachLine { log(it) }
            p.waitFor()
        } catch (e: Exception) { log("❌ 执行失败: ${e.message}") }
    }

    private fun cleanBuild() {
        outputView.text = ""; log("🧹 清理构建缓存...")
        for (d in listOf("build","dist","__pycache__",".gradle","node_modules")) {
            val f = File(projectDir, d)
            if (f.exists()) { f.deleteRecursively(); log("  删除: $d") }
        }
        log("✅ 清理完成")
    }

    private fun installApk() {
        val apks = File(projectDir).walkTopDown().filter { it.extension == "apk" }.toList()
        if (apks.isEmpty()) { log("⚠️ 未找到APK"); return }
        log("📲 安装: ${apks.last().name}")
        try { ProcessBuilder("pm","install","-r",apks.last().absolutePath).redirectErrorStream(true).start().inputStream.bufferedReader().forEachLine { log(it) } }
        catch (e: Exception) { log("❌ 安装失败: ${e.message}") }
    }
}
