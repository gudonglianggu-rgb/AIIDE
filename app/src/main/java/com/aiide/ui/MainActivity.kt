package com.aiide.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.aiide.R
import com.aiide.ai.DeepSeekClient
import com.aiide.builder.ProjectBuilder
import com.aiide.filemanager.FileBrowser
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioGroup
import java.io.File

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var drawerLayout: DrawerLayout
    lateinit var tabLayout: TabLayout
    lateinit var viewPager: ViewPager2
    lateinit var deepSeekClient: DeepSeekClient
    lateinit var projectBuilder: ProjectBuilder
    lateinit var fileBrowser: FileBrowser

    private var currentProjectPath: String = ""

    private val fragments = listOf(
        CodeEditorFragment(),
        AIChatFragment(),
        ProjectBuilderFragment(),
        FileBrowserFragment()
    )

    private val tabTitles = listOf("编辑器", "AI 助手", "打包", "文件")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化组件
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "AIIDE"

        // 初始化 DeepSeek Client
        deepSeekClient = DeepSeekClient(this)
        projectBuilder = ProjectBuilder(this)
        fileBrowser = FileBrowser(this, filesDir.absolutePath)

        // Toggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.nav_open, R.string.nav_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // NavView
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        // Tab + ViewPager
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        val adapter = MainPagerAdapter(supportFragmentManager, lifecycle, fragments, tabTitles)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // 默认加载示例项目
        loadDefaultProject()
    }

    private fun loadDefaultProject() {
        val defaultPath = "${filesDir.absolutePath}/projects"
        val dir = File(defaultPath)
        if (!dir.exists()) dir.mkdirs()
        currentProjectPath = defaultPath
        // 通知各 Fragment
        broadcastProjectPath(currentProjectPath)
    }

    fun broadcastProjectPath(path: String) {
        currentProjectPath = path
        fragments.forEach { fragment ->
            when (fragment) {
                is CodeEditorFragment -> fragment.onProjectPathChanged(path)
                is AIChatFragment -> fragment.onProjectPathChanged(path)
                is ProjectBuilderFragment -> fragment.onProjectPathChanged(path)
                is FileBrowserFragment -> fragment.onProjectPathChanged(path)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_new_project -> showNewProjectDialog()
            R.id.nav_open_project -> showOpenProjectDialog()
            R.id.nav_ai_config -> showAIConfigDialog()
            R.id.nav_terminal -> showTerminalDialog()
            R.id.nav_settings -> showSettingsDialog()
            R.id.nav_about -> showAboutDialog()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showNewProjectDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_project, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.et_project_name)
        val pathInput = dialogView.findViewById<EditText>(R.id.et_project_path)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.type_group)
        // dialog_new_project.xml 使用 RadioGroup + RadioButton，所以直接按选中 Button 取文字
        pathInput.setText(currentProjectPath)

        MaterialAlertDialogBuilder(this)
            .setTitle("新建项目")
            .setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                val name = nameInput.text.toString().trim()
                val basePath = pathInput.text.toString().trim()
                val selectedId = radioGroup.checkedRadioButtonId
                val type = if (selectedId != -1) {
                    dialogView.findViewById<android.widget.RadioButton>(selectedId)?.text?.toString() ?: "空项目"
                } else "空项目"

                if (name.isEmpty()) {
                    Toast.makeText(this, "请输入项目名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val projectDir = File(basePath, name)
                if (projectDir.exists()) {
                    Toast.makeText(this, "项目已存在", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                projectDir.mkdirs()
                currentProjectPath = projectDir.absolutePath
                broadcastProjectPath(currentProjectPath)
                Toast.makeText(this, "项目 $name 创建成功（$type）", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showOpenProjectDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("打开项目")
            .setMessage("当前项目路径:\n$currentProjectPath\n\n请在文件浏览器中导航到目标目录，然后点击路径即可。")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showAIConfigDialog() {
        val config = deepSeekClient.config
        val items = arrayOf(
            "API Key: ${config.api_key.take(8)}...",
            "Base URL: ${config.base_url}",
            "Model: ${config.model}"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("AI 配置")
            .setItems(items) { _, _ -> }
            .setPositiveButton("编辑配置") { _, _ ->
                // 打开配置文件供修改
                Toast.makeText(this, "请修改 ai_config.json 文件", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showTerminalDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("终端")
            .setMessage("终端功能通过 AI 聊天面板中的【终端命令】格式触发。\n\n例如：\n【终端命令】命令: ls -la\n【终端命令】命令: gradle assembleDebug")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showSettingsDialog() {
        Toast.makeText(this, "设置功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("关于 AIIDE")
            .setMessage("AIIDE v1.0\n\n基于 AndroidIDE 架构改造\n内置 DeepSeek AI 大模型\n支持 AI 直接读写文件、执行命令\n包名: com.aiide")
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}