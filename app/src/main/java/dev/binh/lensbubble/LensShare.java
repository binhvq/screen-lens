package dev.binh.lensbubble;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/** Hands a screenshot file to Google Lens. */
final class LensShare {

    private static final String TAG = "LensBubble";
    private static final String GOOGLE_APP = "com.google.android.googlequicksearchbox";
    private static final String LENS_SHARE_ACTIVITY =
            "com.google.android.apps.search.lens.LensShareEntryPointActivity";
    private static final String LENS_APP = "com.google.ar.lens";

    private LensShare() {
    }

    static void open(Context context, Uri uri) {
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION;
        ClipData clip = ClipData.newRawUri("", uri);
        Intent[] candidates = {
                new Intent(Intent.ACTION_SEND)
                        .setType("image/jpeg")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .setClassName(GOOGLE_APP, LENS_SHARE_ACTIVITY),
                new Intent(Intent.ACTION_SEND)
                        .setType("image/jpeg")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .setPackage(LENS_APP),
        };
        for (Intent intent : candidates) {
            intent.addFlags(flags).setClipData(clip);
            try {
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException | SecurityException e) {
                Log.w(TAG, "Lens entry unavailable: " + intent, e);
            }
        }
        Toast.makeText(context, Prefs.text(context, R.string.error_no_lens), Toast.LENGTH_SHORT).show();
    }
}
