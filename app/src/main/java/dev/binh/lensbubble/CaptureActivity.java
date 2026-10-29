package dev.binh.lensbubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/** Invisible launcher entry (e.g. from the ColorOS sidebar) that triggers a Lens capture. */
public class CaptureActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Prefs.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LensBubbleService service = LensBubbleService.get();
        if (service != null) {
            service.requestCapture();
        } else {
            Toast.makeText(this, R.string.status_off, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
