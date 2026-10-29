package dev.binh.lensbubble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/** Language choice and accessibility-service status. */
public class SettingsActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Prefs.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings_title);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(buildContent());
    }

    private View buildContent() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        root.addView(heading(R.string.settings_title, 24));

        root.addView(heading(R.string.language_title, 16));
        RadioGroup group = new RadioGroup(this);
        String current = Prefs.language(this);
        addOption(group, R.string.language_system, Prefs.LANG_SYSTEM, current);
        addOption(group, R.string.language_vi, Prefs.LANG_VI, current);
        addOption(group, R.string.language_en, Prefs.LANG_EN, current);
        group.setOnCheckedChangeListener((g, checkedId) -> {
            RadioButton chosen = g.findViewById(checkedId);
            String lang = (String) chosen.getTag();
            if (!lang.equals(Prefs.language(this))) {
                Prefs.setLanguage(this, lang);
                recreate();
            }
        });
        root.addView(group);

        root.addView(heading(R.string.a11y_title, 16));
        TextView status = new TextView(this);
        status.setText(LensBubbleService.get() != null ? R.string.a11y_on : R.string.a11y_off);
        status.setPadding(0, 0, 0, dp(8));
        root.addView(status);
        Button open = new Button(this);
        open.setText(R.string.a11y_open);
        open.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(open);
        return root;
    }

    private TextView heading(int textRes, float sp) {
        TextView view = new TextView(this);
        view.setText(textRes);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(16), 0, dp(8));
        return view;
    }

    private void addOption(RadioGroup group, int labelRes, String lang, String current) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(labelRes);
        button.setTag(lang);
        button.setChecked(lang.equals(current));
        group.addView(button);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
