package dev.binh.lensbubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Shows the captured screenshot so the user can circle the area to search.
 * The bounding box of the stroke is cropped and sent to Lens; a quick tap sends the whole screen.
 */
public class SelectActivity extends Activity {

    static final String EXTRA_FILE = "file";

    private static final String TAG = "LensBubble";
    private static final float PAD_DP = 16;
    private static final float MIN_SELECTION_DP = 48;

    private File shotFile;
    private Bitmap shot;
    private boolean sending;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Prefs.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra(EXTRA_FILE);
        shotFile = name == null ? null : new File(ShotProvider.shotDir(this), name);
        shot = shotFile == null ? null : BitmapFactory.decodeFile(shotFile.getAbsolutePath());
        if (shot == null) {
            Toast.makeText(this, R.string.error_save, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getWindow().setDecorFitsSystemWindows(false);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(new DrawView(), new FrameLayout.LayoutParams(-1, -1));

        TextView hint = new TextView(this);
        hint.setText(R.string.select_hint);
        hint.setTextColor(Color.WHITE);
        hint.setBackgroundColor(0x99000000);
        int pad = dp(10);
        hint.setPadding(pad * 2, pad, pad * 2, pad);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(-2, -2,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        hintParams.topMargin = dp(48);
        root.addView(hint, hintParams);

        ImageButton settings = new ImageButton(this);
        settings.setImageResource(R.drawable.ic_settings);
        settings.setColorFilter(Color.WHITE);
        settings.setContentDescription(getString(R.string.settings_label));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0x99000000);
        settings.setBackground(circle);
        settings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        });
        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.TOP | Gravity.END);
        settingsParams.topMargin = dp(40);
        settingsParams.setMarginEnd(dp(16));
        root.addView(settings, settingsParams);
        setContentView(root);

        root.getWindowInsetsController().hide(WindowInsets.Type.systemBars());
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void send(RectF region) {
        if (sending) return;
        sending = true;
        boolean whole = region == null;
        new Thread(() -> {
            Uri uri;
            if (whole) {
                uri = ShotProvider.uriFor(shotFile);
            } else {
                File out = new File(shotFile.getParentFile(), "shot-" + System.currentTimeMillis() + ".jpg");
                Bitmap crop = Bitmap.createBitmap(shot, Math.round(region.left), Math.round(region.top),
                        Math.max(1, Math.round(region.width())), Math.max(1, Math.round(region.height())));
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    crop.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                } catch (IOException e) {
                    Log.e(TAG, "crop save failed", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.error_save, Toast.LENGTH_SHORT).show();
                        sending = false;
                    });
                    return;
                } finally {
                    crop.recycle();
                }
                uri = ShotProvider.uriFor(out);
            }
            Uri target = uri;
            runOnUiThread(() -> {
                LensShare.open(this, target);
                finish();
                overridePendingTransition(0, 0);
            });
        }).start();
    }

    private final class DrawView extends View {
        private final Matrix toView = new Matrix();
        private final Matrix toBitmap = new Matrix();
        private final Path stroke = new Path();
        private final RectF bounds = new RectF();
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

        DrawView() {
            super(SelectActivity.this);
            for (Paint p : new Paint[]{outline, line}) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);
            }
            outline.setColor(0xCC000000);
            outline.setStrokeWidth(dp(7));
            line.setColor(Color.WHITE);
            line.setStrokeWidth(dp(4));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            float scale = Math.min((float) w / shot.getWidth(), (float) h / shot.getHeight());
            float dx = (w - shot.getWidth() * scale) / 2f;
            float dy = (h - shot.getHeight() * scale) / 2f;
            toView.setScale(scale, scale);
            toView.postTranslate(dx, dy);
            toView.invert(toBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(shot, toView, null);
            if (!stroke.isEmpty()) {
                canvas.drawPath(stroke, outline);
                canvas.drawPath(stroke, line);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    stroke.reset();
                    stroke.moveTo(x, y);
                    bounds.set(x, y, x, y);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < e.getHistorySize(); i++) {
                        extend(e.getHistoricalX(i), e.getHistoricalY(i));
                    }
                    extend(x, y);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    extend(x, y);
                    invalidate();
                    send(selection());
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    stroke.reset();
                    invalidate();
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }

        private void extend(float x, float y) {
            stroke.lineTo(x, y);
            bounds.union(x, y);
        }

        /** Bounding box of the stroke in bitmap pixels, or null for "whole screen". */
        private RectF selection() {
            float min = dp(MIN_SELECTION_DP);
            if (bounds.width() < min && bounds.height() < min) return null;
            RectF box = new RectF(bounds);
            float pad = dp(PAD_DP);
            box.inset(-pad, -pad);
            toBitmap.mapRect(box);
            box.intersect(0, 0, shot.getWidth(), shot.getHeight());
            return box.isEmpty() ? null : box;
        }
    }
}
