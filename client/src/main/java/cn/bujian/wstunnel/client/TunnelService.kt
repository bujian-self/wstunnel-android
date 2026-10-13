package cn.bujian.wstunnel.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * 前台 Service，仅托管 wstunnel client 子进程：
 *
 *   apps -> 127.0.0.1:socksPort -> wstunnel client -> wss server
 *
 * VPN/tun 层由独立的 sockstun APK 负责，其 SOCKS5 上游指向本服务的端口。
 */
class TunnelService : Service() {
    companion object {
        const val ACTION_START = "cn.bujian.wstunnel.client.action.START"
        const val ACTION_STOP = "cn.bujian.wstunnel.client.action.STOP"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_SNI = "sni"
        const val EXTRA_SOCKS_PORT = "socks_port"
        const val EXTRA_POOL_SIZE = "pool_size"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"

        private const val CHANNEL_ID = "wstunnel_client"
        private const val NOTIF_ID = 1
    }

    private var wstunnel: WstunnelProcess? = null
    private var starting = false

    override fun onBind(intent: Intent?): IBinder? = null

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
            NotificationChannel(CHANNEL_ID, "wstunnel", NotificationManager.IMPORTANCE_LOW)
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
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
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
            LogBuffer.append("[svc] wstunnel socks5 is up on 127.0.0.1:$socksPort")
        } catch (t: Throwable) {
            failStart("start error: ${t.message ?: t.javaClass.simpleName}")
        } finally {
            starting = false
        }
    }

    private fun failStart(msg: String) {
        LogBuffer.append("[svc] FAILED: $msg")
        stopTunnel()
        stopSelf()
    }

    private fun stopTunnel() {
        runCatching { wstunnel?.stop() }
        wstunnel = null
        LogBuffer.append("[svc] tunnel stopped")
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }
}
