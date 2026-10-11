package cn.bujian.wstunnel

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/** Manages the wstunnel client child process and pumps its stdout into [LogBuffer]. */
class WstunnelProcess(private val binary: File) {
    private var process: Process? = null

    @Synchronized
    fun start(serverUrl: String, sni: String, socksPort: Int, poolSize: Int = 4, username: String = "", password: String = "") {
        if (process?.isAlive == true) return
        val effectivePool = poolSize.coerceIn(1, 64)
        val cmd = buildList {
            add(binary.absolutePath)
            add("client")
            add("-L")
            add("socks5://127.0.0.1:$socksPort")
            add("--log-lvl")
            add("info")
            add("--connection-min-idle")
            add(effectivePool.toString())
            if (sni.isNotBlank()) {
                add("--tls-sni-override")
                add(sni)
            }
            if (username.isNotBlank()) {
                add("--http-upgrade-credentials")
                add(if (password.isNotBlank()) "$username:$password" else username)
            }
            add(serverUrl)
        }
        LogBuffer.append("[wstunnel] starting: ${cmd.joinToString(" ")}")
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        process = p
        Thread {
            try {
                BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        LogBuffer.append("[wstunnel] ${line!!.replace(ANSI_ESCAPE, "")}")
                    }
                }
            } catch (_: Exception) {
                // stream closed during stop
            }
            val code = try {
                p.waitFor()
            } catch (_: InterruptedException) {
                -1
            }
            LogBuffer.append("[wstunnel] exited code=$code")
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun isAlive(): Boolean = process?.isAlive == true

    @Synchronized
    fun stop() {
        val p = process ?: return
        p.destroy()
        if (!p.waitFor(2, TimeUnit.SECONDS)) {
            p.destroyForcibly()
        }
        process = null
        LogBuffer.append("[wstunnel] stopped")
    }

    companion object {
        private val ANSI_ESCAPE = Regex("\u001B\\[[;\\d]*m")

        /** Polls the local socks5 port until it accepts a TCP connection. */
        fun waitForPort(port: Int, timeoutMs: Long = 15_000): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 300) }
                    return true
                } catch (_: Exception) {
                    Thread.sleep(200)
                }
            }
            return false
        }
    }
}
