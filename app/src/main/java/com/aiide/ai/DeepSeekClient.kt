package com.aiide.ai

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class DeepSeekClient(private val config: AIConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json".toMediaType()

    data class ChatMessage(val role: String, val content: String)
    data class FileOperation(val path: String, val operation: String, val content: String = "", val lineNumber: Int = 0, val oldContent: String = "")
    data class ChatResponse(val content: String, val fileOperations: List<FileOperation> = emptyList(), val terminalCommands: List<String> = emptyList())

    fun chat(messages: List<ChatMessage>, onStream: (String) -> Unit): ChatResponse {
        val jsonBody = JSONObject().apply {
            put("model", config.model)
            put("max_tokens", config.max_tokens)
            put("temperature", config.temperature)
            put("stream", true)
            val msgs = JSONArray()
            msgs.put(JSONObject().apply { put("role", "system"); put("content", config.system_prompt) })
            for (m in messages) msgs.put(JSONObject().apply { put("role", m.role); put("content", m.content) })
            put("messages", msgs)
        }
        val request = Request.Builder()
            .url("${config.base_url}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.api_key}")
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()
        val fullResponse = StringBuilder()
        val fileOps = mutableListOf<FileOperation>()
        val termCmds = mutableListOf<String>()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                onStream("\n**错误**: API请求失败 [${response.code}]\n${response.body?.string() ?: ""}")
                return ChatResponse("")
            }
            val body = response.body?.source() ?: return ChatResponse("")
            while (!body.exhausted()) {
                val line = body.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ")
                    if (data == "[DONE]") break
                    try {
                        val chunk = JSONObject(data)
                        val choices = chunk.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val content = choices.getJSONObject(0).optJSONObject("delta")?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) { fullResponse.append(content); onStream(content) }
                        }
                    } catch (_: Exception) {}
                }
            }
            response.close()
        } catch (e: Exception) {
            onStream("\n**错误**: ${e.message}")
            return ChatResponse("")
        }
        parseCommands(fullResponse.toString(), fileOps, termCmds)
        return ChatResponse(fullResponse.toString(), fileOps, termCmds)
    }

    private fun parseCommands(text: String, fileOps: MutableList<FileOperation>, termCmds: MutableList<String>) {
        val fileOpRegex = Regex("【文件操作】\\s*路径:\\s*([^\\n]+)\\s*操作:\\s*(\\w+)\\s*(?:行号:\\s*(\\d+))?\\s*(?:旧内容:\\s*([\\s\\S]*?))?内容:\\s*```\\s*([\\s\\S]*?)```", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        for (m in fileOpRegex.findAll(text)) {
            fileOps.add(FileOperation(
                path = m.groupValues[1].trim(),
                operation = when (m.groupValues[2].trim().lowercase()) { "write","覆盖"->"write"; "read","读取"->"read"; "delete","删除"->"delete"; "append","追加"->"append"; "insert","插入"->"insert"; "replace","替换"->"replace"; else->"write" },
                content = m.groupValues[5].trim(),
                lineNumber = m.groupValues[3].trim().toIntOrNull() ?: 0,
                oldContent = m.groupValues[4].trim()
            ))
        }
        Regex("【终端命令】\\s*命令:\\s*([^\\n]+)").findAll(text).forEach { termCmds.add(it.groupValues[1].trim()) }
    }

    fun executeFileOperation(op: FileOperation, projectDir: String): String = try {
        val file = if (op.path.startsWith("/")) File(op.path) else File(projectDir, op.path)
        when (op.operation) {
            "read" -> if (file.exists()) "✅ 读取: ${file.absolutePath}\n\n${file.readText()}" else "❌ 文件不存在"
            "write" -> { file.parentFile?.mkdirs(); file.writeText(op.content); "✅ 已写入 (${op.content.length}字符)" }
            "append" -> { file.parentFile?.mkdirs(); file.appendText("\n${op.content}"); "✅ 已追加" }
            "delete" -> if (file.delete()) "✅ 已删除" else "❌ 删除失败"
            "insert" -> { val lines = file.readLines().toMutableList(); val idx = (op.lineNumber-1).coerceIn(0,lines.size); lines.add(idx, op.content); file.writeText(lines.joinToString("\n")); "✅ 在第${op.lineNumber}行插入" }
            "replace" -> { val text = file.readText(); if (op.oldContent.isNotEmpty()) file.writeText(text.replace(op.oldContent, op.content)); "✅ 已替换" }
            else -> "❌ 未知操作：${op.operation}"
        }
    } catch (e: Exception) { "❌ 操作失败: ${e.message}" }
}
