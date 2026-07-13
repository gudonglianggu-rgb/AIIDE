package com.aiide.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AIConfig(
    val api_key: String = "",
    val base_url: String = "https://api.deepseek.com",
    val model: String = "deepseek-chat",
    val system_prompt: String = "你是一个手机AI编程助手，可以直接修改用户代码文件。\n\n格式说明：\n【文件操作】\n路径: /完整路径\n操作: write|read|delete|append|insert|replace\n内容: ```\n代码内容\n```\n\n【终端命令】\n命令: shell命令",
    val max_tokens: Int = 4096,
    val temperature: Float = 0.7f
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        fun load(configDir: String): AIConfig {
            val file = File(configDir, "ai_config.json")
            return if (file.exists()) {
                try { json.decodeFromString(file.readText()) }
                catch (e: Exception) { AIConfig() }
            } else {
                val config = AIConfig()
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(config))
                config
            }
        }
        fun save(configDir: String, config: AIConfig) {
            val file = File(configDir, "ai_config.json")
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(config))
        }
    }
}
