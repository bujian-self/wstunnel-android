/*
 ============================================================================
 Name        : MainActivity.java
 Author      : hev <r@hev.cc>
 Copyright   : Copyright (c) 2023 xyz
 Description : Main Activity
 ============================================================================
 */

package hev.sockstun;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.VpnService;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	private Preferences prefs;
	private TextInputLayout til_ws_address;
	private TextInputLayout til_ws_sni;
	private TextInputLayout til_ws_port;
	private TextInputLayout til_ws_user;
	private TextInputLayout til_ws_pass;
	private TextInputLayout til_ws_pool;
	private TextInputLayout til_dns_ipv4;
	private TextInputLayout til_dns_ipv6;
	private EditText edittext_ws_address;
	private EditText edittext_ws_sni;
	private EditText edittext_ws_port;
	private EditText edittext_ws_user;
	private EditText edittext_ws_pass;
	private EditText edittext_ws_pool;
	private EditText edittext_dns_ipv4;
	private EditText edittext_dns_ipv6;
	private CheckBox checkbox_udp_in_tcp;
	private CheckBox checkbox_remote_dns;
	private CheckBox checkbox_global;
	private CheckBox checkbox_ipv4;
	private CheckBox checkbox_ipv6;
	private Button button_apps;
	private Button button_save;
	private Button button_control;
	private Button button_profile_prev;
	private Button button_profile_next;
	private TextView textview_profile_name;

	/* Launch-in-progress UI: enable is already true (fields locked) while the
	   service brings up wstunnel first and sockstun after; the button shows a
	   loading state until the service broadcasts success or failure. */
	private boolean launching = false;
	private final Handler launchHandler = new Handler(Looper.getMainLooper());
	private final Runnable launchTimeout = new Runnable() {
		@Override
		public void run() {
			if (!launching)
			  return;
			launching = false;
			updateControlState();
			Toast.makeText(getApplicationContext(), R.string.start_failed, Toast.LENGTH_LONG).show();
		}
	};

	/* The VPN service runs in :native; SharedPreferences is per-process, so
	   enable-state changes made there (start success, failure rollback, stop)
	   arrive here via broadcast and are re-applied to this process's prefs. */
	private final BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean enable = intent.getBooleanExtra(TProxyService.EXTRA_ENABLE, prefs.getEnable());
			prefs.setEnable(enable);
			if (launching) {
				launching = false;
				launchHandler.removeCallbacks(launchTimeout);
				if (!enable)
				  Toast.makeText(context, R.string.start_failed, Toast.LENGTH_LONG).show();
			}
			updateControlState();
		}
	};

	/* Refresh the control state when the tunnel is toggled elsewhere
	   (e.g. from the Quick Settings tile) while this screen is visible. */
	private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
		new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
				if (Preferences.ENABLE.equals(key))
				  updateControlState();
			}
		};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DynamicColors.applyToActivityIfAvailable(this);

		prefs = new Preferences(this);
		cn.bujian.wstunnel.LogBuffer.init(getFilesDir());
		setContentView(R.layout.main);

		til_ws_address = (TextInputLayout) findViewById(R.id.til_ws_address);
		til_ws_sni = (TextInputLayout) findViewById(R.id.til_ws_sni);
		til_ws_port = (TextInputLayout) findViewById(R.id.til_ws_port);
		til_ws_user = (TextInputLayout) findViewById(R.id.til_ws_user);
		til_ws_pass = (TextInputLayout) findViewById(R.id.til_ws_pass);
		til_ws_pool = (TextInputLayout) findViewById(R.id.til_ws_pool);
		til_dns_ipv4 = (TextInputLayout) findViewById(R.id.til_dns_ipv4);
		til_dns_ipv6 = (TextInputLayout) findViewById(R.id.til_dns_ipv6);
		edittext_ws_address = (EditText) findViewById(R.id.ws_address);
		edittext_ws_sni = (EditText) findViewById(R.id.ws_sni);
		edittext_ws_port = (EditText) findViewById(R.id.ws_port);
		edittext_ws_user = (EditText) findViewById(R.id.ws_user);
		edittext_ws_pass = (EditText) findViewById(R.id.ws_pass);
		edittext_ws_pool = (EditText) findViewById(R.id.ws_pool);
		edittext_dns_ipv4 = (EditText) findViewById(R.id.dns_ipv4);
		edittext_dns_ipv6 = (EditText) findViewById(R.id.dns_ipv6);
		checkbox_ipv4 = (CheckBox) findViewById(R.id.ipv4);
		checkbox_ipv6 = (CheckBox) findViewById(R.id.ipv6);
		checkbox_global = (CheckBox) findViewById(R.id.global);
		checkbox_udp_in_tcp = (CheckBox) findViewById(R.id.udp_in_tcp);
		checkbox_remote_dns = (CheckBox) findViewById(R.id.remote_dns);
		button_apps = (Button) findViewById(R.id.apps);
		button_save = (Button) findViewById(R.id.save);
		button_control = (Button) findViewById(R.id.control);
		button_profile_prev = (Button) findViewById(R.id.profile_prev);
		button_profile_next = (Button) findViewById(R.id.profile_next);
		textview_profile_name = (TextView) findViewById(R.id.profile_name);

		button_profile_prev.setOnClickListener(this);
		button_profile_next.setOnClickListener(this);
		textview_profile_name.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (prefs.getEnable())
				  return false;
				showProfileMenu();
				return true;
			}
		});
		checkbox_udp_in_tcp.setOnClickListener(this);
		checkbox_remote_dns.setOnClickListener(this);
		checkbox_global.setOnClickListener(this);
		button_apps.setOnClickListener(this);
		button_save.setOnClickListener(this);
		button_control.setOnClickListener(this);
		updateUI();

		/* Request VPN permission */
		Intent intent = VpnService.prepare(MainActivity.this);
		if (intent != null)
		  startActivityForResult(intent, 0);
		else
		  onActivityResult(0, RESULT_OK, null);
	}

	@Override
	protected void onStart() {
		super.onStart();
		prefs.registerOnChange(prefsListener);
		registerReceiver(vpnStateReceiver, new IntentFilter(TProxyService.ACTION_STATE),
			Context.RECEIVER_NOT_EXPORTED);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateControlState();
	}

	@Override
	protected void onStop() {
		super.onStop();
		prefs.unregisterOnChange(prefsListener);
		unregisterReceiver(vpnStateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		launchHandler.removeCallbacks(launchTimeout);
	}

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		if ((result == RESULT_OK) && prefs.getEnable()) {
			Intent intent = new Intent(this, TProxyService.class);
			startService(intent.setAction(TProxyService.ACTION_CONNECT));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_log) {
			startActivity(new Intent(this, LogActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View view) {
		if (view == checkbox_global || view == checkbox_remote_dns) {
			savePrefs();
			updateUI();
		} else if (view == button_apps) {
			startActivity(new Intent(this, AppListActivity.class));
		} else if (view == button_save) {
			if (!savePrefs()) {
				Toast.makeText(this, R.string.addr_required, Toast.LENGTH_SHORT).show();
				return;
			}
			Toast.makeText(getApplicationContext(), R.string.saved, Toast.LENGTH_SHORT).show();
		} else if (view == button_control) {
			boolean isEnable = prefs.getEnable();
			if (!isEnable) {
				if (!savePrefs()) {
					Toast.makeText(this, R.string.addr_required, Toast.LENGTH_SHORT).show();
					return;
				}
				prefs.setEnable(true);
				launching = true;
				launchHandler.removeCallbacks(launchTimeout);
				launchHandler.postDelayed(launchTimeout, 30_000);
			} else {
				prefs.setEnable(false);
				launching = false;
				launchHandler.removeCallbacks(launchTimeout);
			}
			updateUI();
			Intent intent = new Intent(this, TProxyService.class);
			if (isEnable)
			  startService(intent.setAction(TProxyService.ACTION_DISCONNECT));
			else
			  startService(intent.setAction(TProxyService.ACTION_CONNECT));
			QSTileService.requestUpdate(this);
		} else if (view == button_profile_prev) {
			switchProfile(-1);
		} else if (view == button_profile_next) {
			switchProfile(1);
		}
	}

	private void switchProfile(int direction) {
		if (prefs.getEnable())
		  return;
		int count = prefs.getProfileCount();
		prefs.setSelected((prefs.getSelected() + direction + count) % count);
		updateUI();
	}

	private void showProfileMenu() {
		String[] items = {
			getString(R.string.profile_rename),
			getString(R.string.profile_add),
			getString(R.string.profile_delete),
		};
		new AlertDialog.Builder(this)
			.setTitle(prefs.getProfileName())
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						showProfileNameDialog(R.string.profile_rename, false);
						break;
					case 1:
						addProfileFlow();
						break;
					case 2:
						deleteProfile();
						break;
					}
				}
			})
			.show();
	}

	/* Adding a profile requires a complete current one: persist the page
	   first, then clone into the new profile via the naming dialog. */
	private void addProfileFlow() {
		if (prefs.getProfileCount() >= Preferences.MAX_PROFILES) {
			Toast.makeText(this, R.string.profile_limit, Toast.LENGTH_SHORT).show();
			return;
		}
		if (edittext_ws_address.getText().toString().trim().isEmpty()) {
			Toast.makeText(this, R.string.profile_incomplete, Toast.LENGTH_SHORT).show();
			return;
		}
		if (!savePrefs()) {
			Toast.makeText(this, R.string.addr_required, Toast.LENGTH_SHORT).show();
			return;
		}
		showProfileNameDialog(R.string.profile_add, true);
	}

	private void showProfileNameDialog(int titleId, final boolean add) {
		final EditText input = new EditText(this);
		input.setText(prefs.getProfileName());
		input.setHint(R.string.profile_name);
		input.setSingleLine(true);
		input.selectAll();

		FrameLayout container = new FrameLayout(this);
		int padding = (int) (16 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, 0, padding, 0);
		container.addView(input);

		new AlertDialog.Builder(this)
			.setTitle(titleId)
			.setView(container)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = input.getText().toString().trim();
					if (name.isEmpty())
					  return;
					if (add)
					  prefs.addProfile(name);
					else
					  prefs.setProfileName(name);
					updateUI();
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}

	private void deleteProfile() {
		if (prefs.getProfileCount() <= 1) {
			Toast.makeText(this, R.string.profile_last, Toast.LENGTH_SHORT).show();
			return;
		}
		new AlertDialog.Builder(this)
			.setMessage(getString(R.string.profile_delete_confirm, prefs.getProfileName()))
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					prefs.deleteProfile();
					updateUI();
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}

	private void updateUI() {
		edittext_ws_address.setText(prefs.getWstunnelServer());
		edittext_ws_sni.setText(prefs.getWstunnelSni());
		edittext_ws_port.setText(Integer.toString(prefs.getWstunnelPort()));
		edittext_ws_user.setText(prefs.getWstunnelUser());
		edittext_ws_pass.setText(prefs.getWstunnelPass());
		edittext_ws_pool.setText(Integer.toString(prefs.getWstunnelPool()));
		edittext_dns_ipv4.setText(prefs.getDnsIpv4());
		edittext_dns_ipv6.setText(prefs.getDnsIpv6());
		checkbox_ipv4.setChecked(prefs.getIpv4());
		checkbox_ipv6.setChecked(prefs.getIpv6());
		checkbox_global.setChecked(prefs.getGlobal());
		checkbox_udp_in_tcp.setChecked(prefs.getUdpInTcp());
		checkbox_remote_dns.setChecked(prefs.getRemoteDns());

		updateControlState();
	}

	private void updateControlState() {
		boolean editable = !prefs.getEnable();
		textview_profile_name.setText(prefs.getProfileName());
		button_profile_prev.setEnabled(editable);
		button_profile_next.setEnabled(editable);
		textview_profile_name.setEnabled(editable);
		til_ws_address.setEnabled(editable);
		til_ws_sni.setEnabled(editable);
		til_ws_port.setEnabled(editable);
		til_ws_user.setEnabled(editable);
		til_ws_pass.setEnabled(editable);
		til_ws_pool.setEnabled(editable);
		til_dns_ipv4.setEnabled(editable && !prefs.getRemoteDns());
		til_dns_ipv6.setEnabled(editable && !prefs.getRemoteDns());
		checkbox_udp_in_tcp.setEnabled(editable);
		checkbox_remote_dns.setEnabled(editable);
		checkbox_global.setEnabled(editable);
		checkbox_ipv4.setEnabled(editable);
		checkbox_ipv6.setEnabled(editable);
		button_apps.setEnabled(editable && !prefs.getGlobal());
		button_save.setEnabled(editable);

		if (launching) {
			button_control.setText(R.string.control_launching);
			button_control.setEnabled(false);
		} else {
			button_control.setEnabled(true);
			if (editable)
			  button_control.setText(R.string.control_enable);
			else
			  button_control.setText(R.string.control_disable);
		}
	}

	/* Returns false when the mandatory server address is missing — the page
	   must not be persisted (and the tunnel must not start) in that state. */
	private boolean savePrefs() {
		String addr = edittext_ws_address.getText().toString().trim();
		if (addr.isEmpty())
		  return false;

		prefs.setWstunnelServer(addr);
		prefs.setWstunnelSni(edittext_ws_sni.getText().toString().trim());
		try {
			prefs.setWstunnelPort(Integer.parseInt(edittext_ws_port.getText().toString().trim()));
		} catch (NumberFormatException e) {
		}
		try {
			int pool = Integer.parseInt(edittext_ws_pool.getText().toString().trim());
			prefs.setWstunnelPool(Math.max(1, Math.min(64, pool)));
		} catch (NumberFormatException e) {
		}
		prefs.setWstunnelUser(edittext_ws_user.getText().toString().trim());
		prefs.setWstunnelPass(edittext_ws_pass.getText().toString());
		prefs.setDnsIpv4(edittext_dns_ipv4.getText().toString());
		prefs.setDnsIpv6(edittext_dns_ipv6.getText().toString());
		if (!checkbox_ipv4.isChecked() && !checkbox_ipv6.isChecked())
		  checkbox_ipv4.setChecked(prefs.getIpv4());
		prefs.setIpv4(checkbox_ipv4.isChecked());
		prefs.setIpv6(checkbox_ipv6.isChecked());
		prefs.setGlobal(checkbox_global.isChecked());
		prefs.setUdpInTcp(checkbox_udp_in_tcp.isChecked());
		prefs.setRemoteDns(checkbox_remote_dns.isChecked());
		return true;
	}
}
