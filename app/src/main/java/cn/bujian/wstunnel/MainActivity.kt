package cn.bujian.wstunnel

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var serverInput: EditText
    private lateinit var sniInput: EditText
    private lateinit var portInput: EditText
    private lateinit var dnsInput: EditText
    private lateinit var poolInput: EditText
    private lateinit var userInput: EditText
    private lateinit var passInput: EditText
    private lateinit var toggleButton: Button
    private lateinit var logView: TextView
    private lateinit var configTab: Button
    private lateinit var logTab: Button
    private lateinit var configPage: View
    private lateinit var logPage: View
    private val handler = Handler(Looper.getMainLooper())
    private val logTicker = object : Runnable {
        override fun run() {
            logView.text = LogBuffer.tail()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)

        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }

        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        outer.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val sys = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                v.setPadding(pad + sys.left, pad + sys.top, pad + sys.right, pad + sys.bottom)
            } else {
                v.setPadding(
                    pad + insets.systemWindowInsetLeft,
                    pad + insets.systemWindowInsetTop,
                    pad + insets.systemWindowInsetRight,
                    pad + insets.systemWindowInsetBottom
                )
            }
            insets
        }

        fun tabButton(text: String): Button = Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
        }

        configTab = tabButton("配置")
        logTab = tabButton("日志")
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabRow.addView(configTab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tabRow.addView(logTab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        outer.addView(tabRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        fun label(text: String): TextView = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, (8 * density).toInt(), 0, (2 * density).toInt())
        }

        fun input(hint: String, value: String = ""): EditText = EditText(this).apply {
            this.hint = hint
            setText(value)
            isSingleLine = true
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
        }

        form.addView(label("服务器地址"))
        serverInput = input("https://host:port", Prefs.serverUrl)
        form.addView(serverInput)

        form.addView(label("SNI 伪装域名（可选）"))
        sniInput = input("留空则使用服务器地址的主机名", Prefs.sni)
        form.addView(sniInput)

        form.addView(label("本地 SOCKS5 端口"))
        portInput = input("8888", Prefs.socksPort.toString())
        form.addView(portInput)

        form.addView(label("DNS 服务器"))
        dnsInput = input("198.18.0.2", Prefs.dns)
        form.addView(dnsInput)

        form.addView(label("连接池大小（1-64）"))
        poolInput = input("4", Prefs.poolSize.toString())
        form.addView(poolInput)

        form.addView(label("用户名（可选，留空则不认证）"))
        userInput = input("user", Prefs.username)
        form.addView(userInput)

        form.addView(label("密码"))
        passInput = EditText(this).apply {
            hint = "password"
            setText(Prefs.password)
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        form.addView(passInput)

        toggleButton = Button(this).apply {
            text = "启动"
            setOnClickListener { onToggle() }
        }
        val saveButton = Button(this).apply {
            text = "保存"
            setOnClickListener {
                Prefs.serverUrl = serverInput.text.toString().trim()
                Prefs.sni = sniInput.text.toString().trim()
                Prefs.socksPort = portInput.text.toString().trim().toIntOrNull() ?: 8888
                Prefs.dns = dnsInput.text.toString().trim().ifBlank { "198.18.0.2" }
                Prefs.poolSize = poolInput.text.toString().trim().toIntOrNull()?.coerceIn(1, 64) ?: 4
                Prefs.username = userInput.text.toString().trim()
                Prefs.password = passInput.text.toString()
                Toast.makeText(this@MainActivity, "已保存", Toast.LENGTH_SHORT).show()
            }
        }
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(saveButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(toggleButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = pad })
        form.addView(btnRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = pad })

        configPage = ScrollView(this).apply { addView(form) }
        outer.addView(configPage, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        logView = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        logPage = ScrollView(this).apply { addView(logView) }
        outer.addView(logPage, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        configTab.setOnClickListener { showPage(true) }
        logTab.setOnClickListener { showPage(false) }
        showPage(true)

        setContentView(outer)

        maybeRequestBatteryOptimization()
        maybeRequestNotificationPermission()
    }

    private fun showPage(config: Boolean) {
        configPage.visibility = if (config) View.VISIBLE else View.GONE
        logPage.visibility = if (config) View.GONE else View.VISIBLE
        configTab.alpha = if (config) 1.0f else 0.5f
        logTab.alpha = if (config) 0.5f else 1.0f
    }

    override fun onResume() {
        super.onResume()
        handler.post(logTicker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(logTicker)
    }

    private fun onToggle() {
        if (TunnelServiceRunner.isRunning) {
            val intent = Intent(this, TunnelService::class.java).setAction(TunnelService.ACTION_STOP)
            startService(intent)
            TunnelServiceRunner.isRunning = false
            toggleButton.text = "启动"
            return
        }

        val serverUrl = serverInput.text.toString().trim()
        if (!serverUrl.contains("://")) {
            Toast.makeText(this, "服务器地址需包含协议，如 https://host:port", Toast.LENGTH_LONG).show()
            return
        }
        val sni = sniInput.text.toString().trim()
        val port = portInput.text.toString().trim().toIntOrNull() ?: 8888
        val dns = dnsInput.text.toString().trim().ifBlank { "198.18.0.2" }
        val pool = poolInput.text.toString().trim().toIntOrNull()?.coerceIn(1, 64) ?: 4
        val username = userInput.text.toString().trim()
        val password = passInput.text.toString()

        Prefs.serverUrl = serverUrl
        Prefs.sni = sni
        Prefs.socksPort = port
        Prefs.dns = dns
        Prefs.poolSize = pool
        Prefs.username = username
        Prefs.password = password

        prepareAndStart(serverUrl, sni, port, dns, pool, username, password)
    }

    private fun prepareAndStart(serverUrl: String, sni: String, port: Int, dns: String, pool: Int, username: String, password: String) {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            startActivityForResult(prepare, REQUEST_VPN)
            pendingStart = PendingStart(serverUrl, sni, port, dns, pool, username, password)
        } else {
            dispatchStart(serverUrl, sni, port, dns, pool, username, password)
        }
    }

    private data class PendingStart(
        val url: String,
        val sni: String,
        val port: Int,
        val dns: String,
        val pool: Int,
        val username: String,
        val password: String
    )

    private var pendingStart: PendingStart? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            pendingStart?.let { dispatchStart(it.url, it.sni, it.port, it.dns, it.pool, it.username, it.password) }
        } else if (requestCode == REQUEST_VPN) {
            Toast.makeText(this, "未授权 VPN，无法启动", Toast.LENGTH_LONG).show()
        }
        pendingStart = null
    }

    private fun dispatchStart(serverUrl: String, sni: String, port: Int, dns: String, pool: Int, username: String, password: String) {
        val intent = Intent(this, TunnelService::class.java).apply {
            action = TunnelService.ACTION_START
            putExtra(TunnelService.EXTRA_SERVER_URL, serverUrl)
            putExtra(TunnelService.EXTRA_SNI, sni)
            putExtra(TunnelService.EXTRA_SOCKS_PORT, port)
            putExtra(TunnelService.EXTRA_DNS, dns)
            putExtra(TunnelService.EXTRA_POOL_SIZE, pool)
            putExtra(TunnelService.EXTRA_USERNAME, username)
            putExtra(TunnelService.EXTRA_PASSWORD, password)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        TunnelServiceRunner.isRunning = true
        toggleButton.text = "停止"
    }

    private fun maybeRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT < 23) return
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        AlertDialog.Builder(this)
            .setTitle("电池优化")
            .setMessage("为保持后台稳定，请允许 wstunnel 忽略电池优化，否则系统可能杀掉隧道进程。")
            .setPositiveButton("去设置") { _, _ ->
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                    )
                }.onFailure {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .setNegativeButton("以后再说", null)
            .show()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), REQUEST_NOTIF)
        }
    }

    companion object {
        private const val REQUEST_VPN = 1001
        private const val REQUEST_NOTIF = 1002
    }
}

/** UI-side running flag; the service itself is the source of truth for process state. */
object TunnelServiceRunner {
    var isRunning = false
}
