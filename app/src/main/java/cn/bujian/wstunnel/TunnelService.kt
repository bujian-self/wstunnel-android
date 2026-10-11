package cn.bujian.wstunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * VpnService hosting the tunnel pipeline:
 *
 *   other apps -> TUN -> tun2socks (hev, in-process JNI) -> 127.0.0.1:socksPort
 *               -> wstunnel client (child process) -> wss server
 *
 * The whole app uid is excluded from the VPN via addDisallowedApplication(self):
 * wstunnel's upstream socket and tun2socks' loopback socket then bypass the TUN,
 * which prevents the traffic loop (see docs/adr/0001).
 */
class TunnelService : VpnService() {
    companion object {
        const val ACTION_START = "cn.bujian.wstunnel.action.START"
        const val ACTION_STOP = "cn.bujian.wstunnel.action.STOP"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_SNI = "sni"
        const val EXTRA_SOCKS_PORT = "socks_port"
        const val EXTRA_DNS = "dns"
        const val EXTRA_POOL_SIZE = "pool_size"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"

        private const val CHANNEL_ID = "wstunnel_tunnel"
        private const val NOTIF_ID = 1
        private const val TUN_ADDR = "198.18.0.1"
    }

    private var tunPfd: ParcelFileDescriptor? = null
    private var wstunnel: WstunnelProcess? = null
    private var starting = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL).orEmpty()
                startForegroundInternal(serverUrl)
                if (!starting && wstunnel?.isAlive() != true) {
                    starting = true
                    Thread { startTunnel(intent) }.start()
                }
            }
            ACTION_STOP -> {
                stopTunnel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundInternal(label: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tunnel", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("wstunnel 运行中")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun startTunnel(intent: Intent) {
        try {
            val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
                ?: return failStart("missing server url")
            val sni = intent.getStringExtra(EXTRA_SNI).orEmpty()
            val socksPort = intent.getIntExtra(EXTRA_SOCKS_PORT, 8888)
            val dns = intent.getStringExtra(EXTRA_DNS)?.trim().orEmpty().ifBlank { "198.18.0.2" }
            val poolSize = intent.getIntExtra(EXTRA_POOL_SIZE, 4)
            val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
            val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()

            val bin = NativeBin.ensure(this)
            val proc = WstunnelProcess(bin)
            wstunnel = proc
            proc.start(serverUrl, sni, socksPort, poolSize, username, password)
            if (!WstunnelProcess.waitForPort(socksPort)) {
                return failStart("wstunnel socks5 port $socksPort not listening")
            }
            LogBuffer.append("[vpn] wstunnel socks5 is up")

            val pfd = establishTun(dns) ?: return failStart("VPN permission denied or establish failed")
            tunPfd = pfd

            val cfg = File(filesDir, "hev.yml")
            cfg.writeText(
                """
                tunnel:
                  name: tun0
                  mtu: 1400
                  ipv4: $TUN_ADDR
                  icmp: 'reply'
                socks5:
                  address: 127.0.0.1
                  port: $socksPort
                  udp: 'udp'
                mapdns:
                  address: 198.18.0.2
                  port: 53
                  network: 100.64.0.0
                  netmask: 255.192.0.0
                  cache-size: 10000
                misc:
                  tcp-buffer-size: 262144
                  udp-recv-buffer-size: 1048576
                  max-session-count: 600
                  log-level: info
                """.trimIndent()
            )
            val ok = hev.htproxy.TProxyService.TProxyStartService(cfg.absolutePath, pfd.fd)
            LogBuffer.append("[vpn] tun2socks started=$ok")
            if (!ok) failStart("tun2socks (hev) failed to start")
        } catch (t: Throwable) {
            failStart("start error: ${t.message ?: t.javaClass.simpleName}")
        } finally {
            starting = false
        }
    }

    private fun establishTun(dns: String): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("wstunnel")
            .setMtu(1400)
            .addAddress(TUN_ADDR, 16)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(dns)
        // Exclude this app's uid from the VPN: wstunnel's upstream socket and
        // tun2socks' loopback socket bypass the TUN, preventing a traffic loop.
        builder.addDisallowedApplication(packageName)
        return builder.establish()
    }

    private fun failStart(msg: String) {
        LogBuffer.append("[vpn] FAILED: $msg")
        stopTunnel()
        stopSelf()
    }

    private fun stopTunnel() {
        runCatching { hev.htproxy.TProxyService.TProxyStopService() }
        runCatching { wstunnel?.stop() }
        runCatching { tunPfd?.close() }
        wstunnel = null
        tunPfd = null
        LogBuffer.append("[vpn] tunnel stopped")
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        LogBuffer.append("[vpn] revoked by system")
        stopTunnel()
        stopSelf()
    }
}
