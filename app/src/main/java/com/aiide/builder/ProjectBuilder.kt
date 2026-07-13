package com.aiide.builder

import android.content.Context
import java.io.File

class ProjectBuilder(private val context: Context) {

    fun buildAndroid(projectPath: String): String {
        return try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) return "❌ 项目目录不存在: $projectPath"
            val gradlew = File(projectDir, "gradlew")
            if (!gradlew.exists()) return "⚠️ 缺少 gradlew，请生成 wrapper"
            val p = ProcessBuilder("./gradlew", "assembleDebug", "--no-daemon", "--quiet")
                .directory(projectDir).redirectErrorStream(true).start()
            val o = p.inputStream.bufferedReader().readText()
            if (p.waitFor() == 0) "✅ Android APK 打包成功!\n$o"
            else "❌ 编译失败:\n$o"
        } catch (e: Exception) { "❌ Android 打包异常: ${e.message}" }
    }

    fun buildNpm(projectPath: String): String {
        return try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) return "❌ 项目目录不存在: $projectPath"
            if (!File(projectDir, "package.json").exists()) return "⚠️ 不是 npm 项目"
            val s = mutableListOf("📦 npm 项目")
            val i = ProcessBuilder("npm", "install").directory(projectDir).redirectErrorStream(true).start()
            val io = i.inputStream.bufferedReader().readText()
            if (i.waitFor() != 0) return "❌ npm install 失败:\n$io"
            s.add("✅ 依赖安装完成")
            if (File(projectDir, "package.json").readText().contains("\"build\"")) {
                val b = ProcessBuilder("npm", "run", "build").directory(projectDir).redirectErrorStream(true).start()
                val bo = b.inputStream.bufferedReader().readText()
                s.add(if (b.waitFor() == 0) "✅ build 成功" else "❌ build 失败:\n$bo")
            }
            s.joinToString("\n")
        } catch (e: Exception) { "❌ npm 异常: ${e.message}" }
    }

    fun buildPython(projectPath: String): String {
        return try {
            val d = File(projectPath)
            if (!d.exists()) return "❌ 项目目录不存在: $projectPath"
            val s = mutableListOf("🐍 Python 项目")
            when {
                File(d, "setup.py").exists() -> {
                    val p = ProcessBuilder("python3", "setup.py", "sdist").directory(d).redirectErrorStream(true).start()
                    s.add(if (p.waitFor() == 0) "✅ sdist 构建成功" else "❌ 失败:\n${p.inputStream.bufferedReader().readText()}")
                }
                File(d, "pyproject.toml").exists() -> {
                    val p = ProcessBuilder("python3", "-m", "build", "--sdist").directory(d).redirectErrorStream(true).start()
                    s.add(if (p.waitFor() == 0) "✅ build 成功" else "❌ 失败:\n${p.inputStream.bufferedReader().readText()}")
                }
                else -> s.add("⚠️ 未检测到标准项目结构")
            }
            s.joinToString("\n")
        } catch (e: Exception) { "❌ Python 异常: ${e.message}" }
    }

    fun buildZip(projectPath: String): String {
        return try {
            val d = File(projectPath)
            if (!d.exists()) return "❌ 项目目录不存在: $projectPath"
            val zf = File(d.parentFile, "${d.name}.zip")
            val p = ProcessBuilder("zip", "-r", zf.absolutePath, ".",
                "-x", "*.git*", "-x", "node_modules/*", "-x", ".gradle/*")
                .directory(d).redirectErrorStream(true).start()
            val o = p.inputStream.bufferedReader().readText()
            if (p.waitFor() == 0) "✅ ZIP 打包成功!\n📦 ${zf.absolutePath}\n📏 ${String.format("%.1f", zf.length() / 1024.0 / 1024.0)} MB"
            else "❌ ZIP 失败:\n$o"
        } catch (e: Exception) { "❌ ZIP 异常: ${e.message}" }
    }
}
