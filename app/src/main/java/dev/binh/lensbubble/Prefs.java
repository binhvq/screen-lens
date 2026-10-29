package dev.binh.lensbubble;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/** In-app language choice, applied by wrapping contexts so it works on every API level. */
final class Prefs {

    static final String LANG_SYSTEM = "system";
    static final String LANG_VI = "vi";
    static final String LANG_EN = "en";

    private static final String FILE = "settings";
    private static final String KEY_LANG = "lang";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String language(Context context) {
        return prefs(context).getString(KEY_LANG, LANG_SYSTEM);
    }

    static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANG, language).apply();
    }

    /** Returns a context whose resources use the chosen language. */
    static Context wrap(Context base) {
        String lang = language(base);
        if (LANG_SYSTEM.equals(lang)) return base;
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(new Locale(lang));
        return base.createConfigurationContext(config);
    }

    /** Resolves a string at call time so a language change applies without restarting a service. */
    static String text(Context context, int id, Object... args) {
        return wrap(context).getString(id, args);
    }
}
