/*
 ============================================================================
  Name        : LicensesActivity.java
  Description : Third-party open source license notices
 ============================================================================
 */

package hev.sockstun;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LicensesActivity extends AppCompatActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.licenses);
		if (getSupportActionBar() != null)
		  getSupportActionBar().setTitle(R.string.licenses_title);
		TextView textview_licenses = (TextView) findViewById(R.id.textview_licenses);
		textview_licenses.setText(readLicenses());
	}

	/* License text ships as a raw resource so it is present in every APK build. */
	private String readLicenses() {
		try (InputStream in = getResources().openRawResource(R.raw.third_party_licenses)) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1)
			  out.write(buffer, 0, read);
			return out.toString(StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			return getString(R.string.licenses_unavailable);
		}
	}
}
