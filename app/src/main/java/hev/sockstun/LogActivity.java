/*
 ============================================================================
  Name        : LogActivity.java
  Description : Live view of the wstunnel / VPN service log
 ============================================================================
 */

package hev.sockstun;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LogActivity extends AppCompatActivity {
	private final Handler handler = new Handler(Looper.getMainLooper());
	private TextView textview_log;
	private ScrollView scrollview_log;
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			String tail = cn.bujian.wstunnel.LogBuffer.tail(800);
			if (!tail.isEmpty()) {
				textview_log.setText(tail);
				scrollview_log.post(() -> scrollview_log.fullScroll(View.FOCUS_DOWN));
			}
			handler.postDelayed(this, 1000);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);
		if (getSupportActionBar() != null)
		  getSupportActionBar().setTitle(R.string.log_title);
		cn.bujian.wstunnel.LogBuffer.init(getFilesDir());
		textview_log = (TextView) findViewById(R.id.textview_log);
		scrollview_log = (ScrollView) findViewById(R.id.scrollview_log);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.log, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_copy) {
			ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			cm.setPrimaryClip(ClipData.newPlainText("wstunnel log", textview_log.getText()));
			Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		handler.post(refresh);
	}

	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(refresh);
	}
}
