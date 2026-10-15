/*
 ============================================================================
 Name        : TProxyService.java
 Author      : hev <r@hev.cc>
 Copyright   : Copyright (c) 2024 xyz
 Description : TProxy Service
 ============================================================================
 */

package hev.sockstun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;

import androidx.core.app.NotificationCompat;

public class TProxyService extends VpnService {
	private static native boolean TProxyStartService(String config_path, int fd);
	private static native boolean TProxyStopService();
	private static native boolean TProxyIsRunning();
	private static native long[] TProxyGetStats();

	public static final String ACTION_CONNECT = "hev.sockstun.CONNECT";
	public static final String ACTION_DISCONNECT = "hev.sockstun.DISCONNECT";
	/* Broadcast to the main process: SharedPreferences is per-process,
	   so enable-state changes made here must be pushed explicitly. */
	public static final String ACTION_STATE = "cn.bujian.wstunnel.VPN_STATE";
	public static final String EXTRA_ENABLE = "enable";

	static {
		System.loadLibrary("hev-socks5-tunnel");
	}

	private volatile ParcelFileDescriptor tunFd = null;
	private volatile boolean stopping = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
			stopService();
			return START_NOT_STICKY;
		}
		startService();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onRevoke() {
		stopService();
		super.onRevoke();
	}

	public void startService() {
		if (tunFd != null)
		  return;
		stopping = false;
		/* waitForPort blocks up to 15s: never on the main thread. */
		new Thread(this::doStartService, "vpn-start").start();
	}

	private void doStartService() {
		Preferences prefs = new Preferences(this);
		cn.bujian.wstunnel.LogBuffer.init(getFilesDir());
		cn.bujian.wstunnel.LogBuffer.append("[vpn] start requested");

		/* The only upstream is wstunnel, driven by the active profile.
		   It must be listening on its local socks port BEFORE the tun exists,
		   so any failure leaves no half-established VPN behind. */
		String wsServer = prefs.getWstunnelServer().trim();
		if (wsServer.isEmpty()) {
			cn.bujian.wstunnel.LogBuffer.append("[vpn] server address empty, aborting start");
			failStart(prefs);
			return;
		}
		boolean wsOk = cn.bujian.wstunnel.Wstunnel.ensureStarted(
			getApplicationContext(), wsServer,
			prefs.getWstunnelSni().trim(), prefs.getWstunnelPort(),
			prefs.getWstunnelPool(), prefs.getWstunnelUser().trim(),
			prefs.getWstunnelPass());
		if (stopping) {
			cn.bujian.wstunnel.Wstunnel.stop();
			cn.bujian.wstunnel.LogBuffer.append("[vpn] start aborted by stop");
			return;
		}
		if (!wsOk) {
			cn.bujian.wstunnel.LogBuffer.append("[vpn] wstunnel failed, aborting start");
			failStart(prefs);
			return;
		}

		String socksAddr = "127.0.0.1";
		int socksPort = prefs.getWstunnelPort();

		String session = new String();
		VpnService.Builder builder = new VpnService.Builder();
		builder.setBlocking(false);
		builder.setMtu(prefs.getTunnelMtu());
		if (prefs.getIpv4()) {
			String addr = prefs.getTunnelIpv4Address();
			int prefix = prefs.getTunnelIpv4Prefix();
			String dns = prefs.getDnsIpv4();
			builder.addAddress(addr, prefix);
			builder.addRoute("0.0.0.0", 0);
			if (!prefs.getRemoteDns() && !dns.isEmpty())
			  builder.addDnsServer(dns);
			session += "IPv4";
		}
		if (prefs.getIpv6()) {
			String addr = prefs.getTunnelIpv6Address();
			int prefix = prefs.getTunnelIpv6Prefix();
			String dns = prefs.getDnsIpv6();
			builder.addAddress(addr, prefix);
			builder.addRoute("::", 0);
			if (!prefs.getRemoteDns() && !dns.isEmpty())
			  builder.addDnsServer(dns);
			if (!session.isEmpty())
			  session += " + ";
			session += "IPv6";
		}
		if (prefs.getRemoteDns()) {
			builder.addDnsServer(prefs.getMappedDns());
		}
		boolean disallowSelf = true;
		if (prefs.getGlobal()) {
			session += "/Global";
		} else {
			for (String appName : prefs.getApps()) {
				try {
					builder.addAllowedApplication(appName);
					disallowSelf = false;
				} catch (NameNotFoundException e) {
				}
			}
			session += "/per-App";
		}
		if (disallowSelf) {
			String selfName = getApplicationContext().getPackageName();
			try {
				builder.addDisallowedApplication(selfName);
			} catch (NameNotFoundException e) {
			}
		}
		builder.setSession(session);
		tunFd = builder.establish();
		if (tunFd == null) {
			cn.bujian.wstunnel.LogBuffer.append("[vpn] tun establish failed, aborting start");
			failStart(prefs);
			return;
		}
		if (stopping) {
			try {
				tunFd.close();
			} catch (IOException e) {
			}
			tunFd = null;
			cn.bujian.wstunnel.Wstunnel.stop();
			cn.bujian.wstunnel.LogBuffer.append("[vpn] start aborted by stop");
			return;
		}

		/* TProxy */
		File tproxy_file = new File(getCacheDir(), "tproxy.conf");
		try {
			tproxy_file.createNewFile();
			FileOutputStream fos = new FileOutputStream(tproxy_file, false);

		String tproxy_conf = "misc:\n" +
			"  task-stack-size: " + prefs.getTaskStackSize() + "\n" +
			"  log-file: '" + new File(getFilesDir(), "wstunnel.log").getAbsolutePath() + "'\n" +
			"  log-level: info\n" +
			"tunnel:\n" +
				"  mtu: " + prefs.getTunnelMtu() + "\n" +
				"  icmp: 'reply'\n" +
				"socks5:\n" +
				"  port: " + socksPort + "\n" +
				"  address: '" + socksAddr + "'\n" +
				"  udp: '" + (prefs.getUdpInTcp() ? "tcp" : "udp") + "'\n";

			if (prefs.getRemoteDns()) {
				tproxy_conf += "mapdns:\n" +
					"  address: " + prefs.getMappedDns() + "\n" +
					"  port: 53\n" +
					"  network: 240.0.0.0\n" +
					"  netmask: 240.0.0.0\n" +
					"  cache-size: 10000\n";
			}

			fos.write(tproxy_conf.getBytes());
			fos.close();
		} catch (IOException e) {
			/* Leave nothing half-started: drop the tun, roll back enable. */
			try {
				tunFd.close();
			} catch (IOException e2) {
			}
			tunFd = null;
			cn.bujian.wstunnel.LogBuffer.append("[vpn] tproxy.conf write failed, aborting start");
			failStart(prefs);
			return;
		}
		TProxyStartService(tproxy_file.getAbsolutePath(), tunFd.getFd());
		cn.bujian.wstunnel.LogBuffer.append("[vpn] tproxy started (upstream " +
			socksAddr + ":" + socksPort + ", via wstunnel)");
		prefs.setEnable(true);
		QSTileService.requestUpdate(this);
		broadcastState(true);

		String channelName = "socks5";
		initNotificationChannel(channelName);
		createNotification(channelName);
	}

	/* Full rollback on any start failure: nothing half-established stays behind. */
	private void failStart(Preferences prefs) {
		prefs.setEnable(false);
		QSTileService.requestUpdate(this);
		broadcastState(false);
		stopSelf();
	}

	private void broadcastState(boolean enable) {
		Intent i = new Intent(ACTION_STATE);
		i.setPackage(getPackageName());
		i.putExtra(EXTRA_ENABLE, enable);
		sendBroadcast(i);
	}

	public void stopService() {
		stopping = true;
		new Thread(this::doStopService, "vpn-stop").start();
	}

	private void doStopService() {
		ParcelFileDescriptor fd = tunFd;
		if (fd != null) {
			new Preferences(this).setEnable(false);
			QSTileService.requestUpdate(this);
			broadcastState(false);

			stopForeground(true);

			/* TProxy */
			TProxyStopService();

			/* VPN */
			try {
				fd.close();
			} catch (IOException e) {
			}
			tunFd = null;
		} else {
			new Preferences(this).setEnable(false);
			QSTileService.requestUpdate(this);
			broadcastState(false);
		}

		/* wstunnel child process must die before this :native process exits.
		   Wstunnel.stop() waits for any in-flight ensureStarted (same lock),
		   so the child is always reaped before exit. */
		cn.bujian.wstunnel.Wstunnel.stop();
		cn.bujian.wstunnel.LogBuffer.append("[vpn] stopped");

		System.exit(0);
	}

	private void createNotification(String channelName) {
		Intent i = new Intent(this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
		NotificationCompat.Builder notification = new NotificationCompat.Builder(this, channelName);
		Notification notify = notification
				.setContentTitle(getString(R.string.app_name))
				.setSmallIcon(android.R.drawable.sym_def_app_icon)
				.setContentIntent(pi)
				.build();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			startForeground(1, notify);
		} else {
			startForeground(1, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
		}
	}

	// create NotificationChannel
	private void initNotificationChannel(String channelName) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.app_name);
			NotificationChannel channel = new NotificationChannel(channelName, name, NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
		}
	}
}
