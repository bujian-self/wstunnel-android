package cn.bujian.wstunnel

import android.content.Context

/**
 * Global single-instance manager for the wstunnel child process.
 *
 * One instance is shared by all profiles (Q8b): [ensureStarted] restarts the process
 * only when the effective config changed; an empty server means "disabled" and the
 * tunnel falls back to a plain SOCKS5 upstream.
 */
object Wstunnel {
    private var proc: WstunnelProcess? = null
    private var config: String? = null

    @JvmStatic
    @Synchronized
    fun ensureStarted(
        context: Context,
        server: String,
        sni: String,
        port: Int,
        pool: Int,
        user: String,
        pass: String
    ): Boolean {
        val key = "$server|$sni|$port|$pool|$user|$pass"
        if (config == key && proc?.isAlive() == true) return true
        stopInternal()

        return try {
            val bin = NativeBin.ensure(context)
            val p = WstunnelProcess(bin)
            p.start(server, sni, port, pool, user, pass)
            if (!WstunnelProcess.waitForPort(port)) {
                LogBuffer.append("[wstunnel] port $port not listening")
                p.stop()
                false
            } else {
                proc = p
                config = key
                true
            }
        } catch (t: Throwable) {
            LogBuffer.append("[wstunnel] start failed: ${t.message ?: t.javaClass.simpleName}")
            false
        }
    }

    @JvmStatic
    @Synchronized
    fun stop() {
        stopInternal()
    }

    @JvmStatic
    @Synchronized
    fun isRunning(): Boolean = proc?.isAlive() == true

    private fun stopInternal() {
        proc?.stop()
        proc = null
        config = null
    }
}
