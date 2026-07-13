package com.aiide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aiide.R
import com.aiide.ai.DeepSeekClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIChatFragment : Fragment() {

    private lateinit var tvChatHistory: TextView
    private lateinit var etPrompt: EditText
    private lateinit var btnSend: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var deepSeekClient: DeepSeekClient

    private var currentProjectPath: String = ""
    private val chatHistory = StringBuilder()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ai_chat, container, false)

        tvChatHistory = view.findViewById(R.id.tv_chat_history)
        etPrompt = view.findViewById(R.id.et_prompt)
        btnSend = view.findViewById(R.id.btn_send)
        btnClear = view.findViewById(R.id.btn_clear)

        deepSeekClient = DeepSeekClient(requireContext())

        appendMessage("系统", "AIIDE AI 助手已就绪。请描述您想实现的功能，AI 将自动生成代码并写入文件。")

        btnSend.setOnClickListener {
            sendPrompt()
        }

        btnClear.setOnClickListener {
            chatHistory.clear()
            tvChatHistory.text = ""
            appendMessage("系统", "对话已清空")
        }

        return view
    }

    fun onProjectPathChanged(path: String) {
        currentProjectPath = path
    }

    private fun sendPrompt() {
        val prompt = etPrompt.text.toString().trim()
        if (prompt.isEmpty()) {
            Toast.makeText(context, "请输入提示词", Toast.LENGTH_SHORT).show()
            return
        }

        appendMessage("你", prompt)
        etPrompt.setText("")
        appendMessage("AI", "思考中...")

        scope.launch {
            try {
                val systemPrompt = buildString {
                    append("你是一个AI编程助手。用户正在使用AIIDE手机端编程应用。")
                    append("\n当前项目路径: $currentProjectPath")
                    append("\n\n你可以通过以下两种方式操作文件：")
                    append("\n1. 【文件操作】格式：")
                    append("\n【文件操作】路径: /path/to/file")
                    append("\n【文件操作】操作: write/append/delete/mkdir")
                    append("\n```")
                    append("\n文件内容")
                    append("\n```")
                    append("\n2. 【终端命令】格式：")
                    append("\n【终端命令】命令: ls -la")
                    append("\n\n请直接输出代码，不要解释，我会自动解析和执行。")
                }

                val messages = listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to prompt)
                )
                val response = withContext(Dispatchers.IO) {
                    val chatMessages = messages.map { DeepSeekClient.ChatMessage(role = it["role"] ?: "user", content = it["content"] ?: "") }
                    deepSeekClient.chat(chatMessages) { /* streaming 不需要实时显示 */ }
                }
                val lastIndex = chatHistory.lastIndexOf("思考中...")
                if (lastIndex != -1) {
                    chatHistory.replace(lastIndex, lastIndex + 5, "")
                }
                appendMessage("AI", response.content)
            } catch (e: Exception) {
                val lastIndex = chatHistory.lastIndexOf("思考中...")
                if (lastIndex != -1) {
                    chatHistory.replace(lastIndex, lastIndex + 5, "")
                }
                appendMessage("AI", "❌ 错误: ${e.message}")
            }
        }
    }

    private fun appendMessage(sender: String, message: String) {
        chatHistory.append("[$sender] $message\n\n")
        tvChatHistory.text = chatHistory.toString()
        // 滚动到底部
        tvChatHistory.post {
            tvChatHistory.scrollTo(0, tvChatHistory.layout?.height ?: 0)
        }
    }
}