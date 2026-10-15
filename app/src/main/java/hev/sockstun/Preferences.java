/*
 ============================================================================
 Name        : Perferences.java
 Author      : hev <r@hev.cc>
 Copyright   : Copyright (c) 2023 xyz
 Description : Perferences
 ============================================================================
 */

package hev.sockstun;

import java.util.Set;
import java.util.HashSet;
import android.content.Context;
import android.content.SharedPreferences;

public class Preferences
{
	public static final String PREFS_NAME = "SocksPrefs";
	public static final String DNS_IPV4 = "DnsIpv4";
	public static final String DNS_IPV6 = "DnsIpv6";
	public static final String IPV4 = "Ipv4";
	public static final String IPV6 = "Ipv6";
	public static final String GLOBAL = "Global";
	public static final String UDP_IN_TCP = "UdpInTcp";
	public static final String REMOTE_DNS = "RemoteDNS";
	public static final String APPS = "Apps";
	public static final String ENABLE = "Enable";
	public static final String NAME = "Name";
	public static final String PROFILE_COUNT = "ProfileCount";
	public static final String SELECTED = "Selected";

	/* wstunnel settings — the only upstream configuration, profile-scoped. */
	public static final String WS_SERVER = "WstunnelServer";
	public static final String WS_SNI = "WstunnelSni";
	public static final String WS_PORT = "WstunnelPort";
	public static final String WS_POOL = "WstunnelPool";
	public static final String WS_USER = "WstunnelUser";
	public static final String WS_PASS = "WstunnelPass";

	public static final int MAX_PROFILES = 13;

	private SharedPreferences prefs;

	public Preferences(Context context) {
		prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
		migrate();
	}

	/* Profile-scoped key: all per-server settings live under "P<index>." */
	private static String key(int profile, String name) {
		return "P" + profile + "." + name;
	}

	private String key(String name) {
		return key(getSelected(), name);
	}

	/* One-time migration of legacy flat keys into profile 0. */
	private void migrate() {
		if (prefs.contains(PROFILE_COUNT))
		  return;

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(0, NAME), "Default");
		if (prefs.contains(DNS_IPV4))
		  editor.putString(key(0, DNS_IPV4), prefs.getString(DNS_IPV4, ""));
		if (prefs.contains(DNS_IPV6))
		  editor.putString(key(0, DNS_IPV6), prefs.getString(DNS_IPV6, ""));
		if (prefs.contains(IPV4))
		  editor.putBoolean(key(0, IPV4), prefs.getBoolean(IPV4, true));
		if (prefs.contains(IPV6))
		  editor.putBoolean(key(0, IPV6), prefs.getBoolean(IPV6, true));
		if (prefs.contains(GLOBAL))
		  editor.putBoolean(key(0, GLOBAL), prefs.getBoolean(GLOBAL, false));
		if (prefs.contains(UDP_IN_TCP))
		  editor.putBoolean(key(0, UDP_IN_TCP), prefs.getBoolean(UDP_IN_TCP, false));
		if (prefs.contains(REMOTE_DNS))
		  editor.putBoolean(key(0, REMOTE_DNS), prefs.getBoolean(REMOTE_DNS, true));
		if (prefs.contains(APPS))
		  editor.putStringSet(key(0, APPS), new HashSet<String>(prefs.getStringSet(APPS, new HashSet<String>())));
		editor.remove(DNS_IPV4);
		editor.remove(DNS_IPV6);
		editor.remove(IPV4);
		editor.remove(IPV6);
		editor.remove(GLOBAL);
		editor.remove(UDP_IN_TCP);
		editor.remove(REMOTE_DNS);
		editor.remove(APPS);
		editor.putInt(PROFILE_COUNT, 1);
		editor.putInt(SELECTED, 0);
		editor.commit();
	}

	/* Copy every per-profile key from profile src to profile dst.
	   Reads always see the committed state, so shifting profiles
	   in ascending order within one editor is safe. */
	private void copyProfile(SharedPreferences.Editor editor, int src, int dst) {
		editor.putString(key(dst, NAME), prefs.getString(key(src, NAME), "Default"));
		editor.putString(key(dst, DNS_IPV4), prefs.getString(key(src, DNS_IPV4), "8.8.8.8"));
		editor.putString(key(dst, DNS_IPV6), prefs.getString(key(src, DNS_IPV6), "2001:4860:4860::8888"));
		editor.putBoolean(key(dst, IPV4), prefs.getBoolean(key(src, IPV4), true));
		editor.putBoolean(key(dst, IPV6), prefs.getBoolean(key(src, IPV6), true));
		editor.putBoolean(key(dst, GLOBAL), prefs.getBoolean(key(src, GLOBAL), false));
		editor.putBoolean(key(dst, UDP_IN_TCP), prefs.getBoolean(key(src, UDP_IN_TCP), false));
		editor.putBoolean(key(dst, REMOTE_DNS), prefs.getBoolean(key(src, REMOTE_DNS), true));
		editor.putString(key(dst, WS_SERVER), prefs.getString(key(src, WS_SERVER), ""));
		editor.putString(key(dst, WS_SNI), prefs.getString(key(src, WS_SNI), ""));
		editor.putInt(key(dst, WS_PORT), prefs.getInt(key(src, WS_PORT), 8888));
		editor.putInt(key(dst, WS_POOL), prefs.getInt(key(src, WS_POOL), 4));
		editor.putString(key(dst, WS_USER), prefs.getString(key(src, WS_USER), ""));
		editor.putString(key(dst, WS_PASS), prefs.getString(key(src, WS_PASS), ""));
		editor.putStringSet(key(dst, APPS), new HashSet<String>(prefs.getStringSet(key(src, APPS), new HashSet<String>())));
	}

	private void removeProfile(SharedPreferences.Editor editor, int profile) {
		editor.remove(key(profile, NAME));
		editor.remove(key(profile, DNS_IPV4));
		editor.remove(key(profile, DNS_IPV6));
		editor.remove(key(profile, IPV4));
		editor.remove(key(profile, IPV6));
		editor.remove(key(profile, GLOBAL));
		editor.remove(key(profile, UDP_IN_TCP));
		editor.remove(key(profile, REMOTE_DNS));
		editor.remove(key(profile, APPS));
		editor.remove(key(profile, WS_SERVER));
		editor.remove(key(profile, WS_SNI));
		editor.remove(key(profile, WS_PORT));
		editor.remove(key(profile, WS_POOL));
		editor.remove(key(profile, WS_USER));
		editor.remove(key(profile, WS_PASS));
	}

	public int getProfileCount() {
		return prefs.getInt(PROFILE_COUNT, 1);
	}

	public int getSelected() {
		return prefs.getInt(SELECTED, 0);
	}

	public void setSelected(int index) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(SELECTED, index);
		editor.commit();
	}

	public String getProfileName() {
		return prefs.getString(key(NAME), "Default");
	}

	public void setProfileName(String name) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(NAME), name);
		editor.commit();
	}

	/* Create a new profile as a copy of the current one and select it. */
	public boolean addProfile(String name) {
		int count = getProfileCount();
		if (count >= MAX_PROFILES)
		  return false;

		SharedPreferences.Editor editor = prefs.edit();
		copyProfile(editor, getSelected(), count);
		editor.putString(key(count, NAME), name);
		editor.putInt(PROFILE_COUNT, count + 1);
		editor.putInt(SELECTED, count);
		editor.commit();
		return true;
	}

	/* Delete the current profile, shifting the following ones down. */
	public boolean deleteProfile() {
		int count = getProfileCount();
		if (count <= 1)
		  return false;

		int selected = getSelected();
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = selected + 1; i < count; i++)
		  copyProfile(editor, i, i - 1);
		removeProfile(editor, count - 1);
		editor.putInt(PROFILE_COUNT, count - 1);
		if (selected >= count - 1)
		  editor.putInt(SELECTED, count - 2);
		editor.commit();
		return true;
	}

	public String getDnsIpv4() {
		return prefs.getString(key(DNS_IPV4), "8.8.8.8");
	}

	public void setDnsIpv4(String addr) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(DNS_IPV4), addr);
		editor.commit();
	}

	public String getDnsIpv6() {
		return prefs.getString(key(DNS_IPV6), "2001:4860:4860::8888");
	}

	public void setDnsIpv6(String addr) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(DNS_IPV6), addr);
		editor.commit();
	}

	public String getMappedDns() {
		return "198.18.0.2";
	}

	public boolean getUdpInTcp() {
		return prefs.getBoolean(key(UDP_IN_TCP), false);
	}

	public void setUdpInTcp(boolean enable) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key(UDP_IN_TCP), enable);
		editor.commit();
	}

	public boolean getRemoteDns() {
		return prefs.getBoolean(key(REMOTE_DNS), false);
	}

	public void setRemoteDns(boolean enable) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key(REMOTE_DNS), enable);
		editor.commit();
	}

	public boolean getIpv4() {
		return prefs.getBoolean(key(IPV4), true);
	}

	public void setIpv4(boolean enable) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key(IPV4), enable);
		editor.commit();
	}

	public boolean getIpv6() {
		return prefs.getBoolean(key(IPV6), true);
	}

	public void setIpv6(boolean enable) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key(IPV6), enable);
		editor.commit();
	}

	public boolean getGlobal() {
		return prefs.getBoolean(key(GLOBAL), false);
	}

	public void setGlobal(boolean enable) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key(GLOBAL), enable);
		editor.commit();
	}

	public Set<String> getApps() {
		return prefs.getStringSet(key(APPS), new HashSet<String>());
	}

	public void setApps(Set<String> apps) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(key(APPS), apps);
		editor.commit();
	}

	public boolean getEnable() {
		return prefs.getBoolean(ENABLE, false);
	}

	public void setEnable(boolean enable) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(ENABLE, enable);
		editor.commit();
	}

	/* wstunnel getters/setters are profile-scoped via key(). */
	public String getWstunnelServer() {
		return prefs.getString(key(WS_SERVER), "");
	}

	public void setWstunnelServer(String server) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(WS_SERVER), server);
		editor.commit();
	}

	public String getWstunnelSni() {
		return prefs.getString(key(WS_SNI), "");
	}

	public void setWstunnelSni(String sni) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(WS_SNI), sni);
		editor.commit();
	}

	public int getWstunnelPort() {
		return prefs.getInt(key(WS_PORT), 8888);
	}

	public void setWstunnelPort(int port) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(key(WS_PORT), port);
		editor.commit();
	}

	public int getWstunnelPool() {
		return prefs.getInt(key(WS_POOL), 4);
	}

	public void setWstunnelPool(int pool) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(key(WS_POOL), pool);
		editor.commit();
	}

	public String getWstunnelUser() {
		return prefs.getString(key(WS_USER), "");
	}

	public void setWstunnelUser(String user) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(WS_USER), user);
		editor.commit();
	}

	public String getWstunnelPass() {
		return prefs.getString(key(WS_PASS), "");
	}

	public void setWstunnelPass(String pass) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key(WS_PASS), pass);
		editor.commit();
	}

	public void registerOnChange(SharedPreferences.OnSharedPreferenceChangeListener listener) {
		prefs.registerOnSharedPreferenceChangeListener(listener);
	}

	public void unregisterOnChange(SharedPreferences.OnSharedPreferenceChangeListener listener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
	}

	public int getTunnelMtu() {
		return 8500;
	}

	public String getTunnelIpv4Address() {
		return "198.18.0.1";
	}

	public int getTunnelIpv4Prefix() {
		return 32;
	}

	public String getTunnelIpv6Address() {
		return "fc00::1";
	}

	public int getTunnelIpv6Prefix() {
		return 128;
	}

	public int getTaskStackSize() {
		return 81920;
	}
}
