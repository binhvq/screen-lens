package dev.binh.lensbubble;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Takes a screenshot on request and hands it to Google Lens. */
public class LensBubbleService extends AccessibilityService {

    private static final String TAG = "LensBubble";
    // Lets the sidebar panel and the trampoline activity leave the screen first.
    private static final long SETTLE_BEFORE_CAPTURE_MS = 400;

    private static volatile LensBubbleService instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean capturing;

    static LensBubbleService get() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        io.shutdown();
        super.onDestroy();
    }

    void requestCapture() {
        if (capturing) return;
        capturing = true;
        main.postDelayed(() -> takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        Bitmap bitmap;
                        try (HardwareBuffer buffer = result.getHardwareBuffer()) {
                            Bitmap hw = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
                            bitmap = hw == null ? null : hw.copy(Bitmap.Config.ARGB_8888, false);
                        }
                        if (bitmap == null) {
                            fail(text(R.string.error_save));
                            return;
                        }
                        io.execute(() -> saveAndShare(bitmap));
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        Log.w(TAG, "takeScreenshot failed: " + errorCode);
                        fail(text(R.string.error_capture, errorCode));
                    }
                }), SETTLE_BEFORE_CAPTURE_MS);
    }

    private void saveAndShare(Bitmap bitmap) {
        File dir = ShotProvider.shotDir(this);
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
        dir.mkdirs();
        File file = new File(dir, "shot-" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
        } catch (IOException e) {
            Log.e(TAG, "save failed", e);
            main.post(() -> fail(text(R.string.error_save)));
            return;
        } finally {
            bitmap.recycle();
        }
        main.post(() -> {
            startActivity(new Intent(this, SelectActivity.class)
                    .putExtra(SelectActivity.EXTRA_FILE, file.getName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            capturing = false;
        });
    }

    private String text(int id, Object... args) {
        return Prefs.text(this, id, args);
    }

    private void fail(String message) {
        capturing = false;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
