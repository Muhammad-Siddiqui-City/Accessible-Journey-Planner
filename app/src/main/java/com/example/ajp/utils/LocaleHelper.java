package com.example.ajp.utils;

// AI Generated
// Built with Claude
// Lovable.dev reference

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

/**
 * Shared utility class for LocaleHelper.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public final class LocaleHelper {

    private static final float LARGE_TEXT_SCALE = 1.25f;

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private static Locale localeFromLang(String lang) {
        if (lang == null) lang = SettingsPrefs.LANG_EN_GB;
        lang = lang.trim();
        String tag;
        if (SettingsPrefs.LANG_ES.equals(lang)) tag = "es";
        else if (SettingsPrefs.LANG_ZH.equals(lang)) tag = "zh";
        else if (SettingsPrefs.LANG_AR.equals(lang)) tag = "ar";
        else if (SettingsPrefs.LANG_EN_GB.equals(lang)) tag = "en-GB";
        else tag = "en-GB";
        return Locale.forLanguageTag(tag);
    }

    // Applies state changes and keeps dependent UI/data values synchronized.
    public static Context applyFull(Context context) {
        try {
            Context app = context.getApplicationContext();
            SettingsPrefs prefs = SettingsPrefs.get(app);
            String lang = prefs.getLanguage();
            Locale locale = localeFromLang(lang);
            Locale.setDefault(locale);
            float fontScale = prefs.isLargeText() ? LARGE_TEXT_SCALE : 1.0f;
            return wrap(context, locale, fontScale);
        } catch (Exception e) {
            return context;
        }
    }

    // Applies state changes and keeps dependent UI/data values synchronized.
    public static Context apply(Context context) {
        String lang = SettingsPrefs.get(context.getApplicationContext()).getLanguage();
        Locale locale = localeFromLang(lang);
        return wrap(context, locale, 1.0f);
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private static Context wrap(Context context, Locale locale, float fontScale) {
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.fontScale = fontScale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        res.updateConfiguration(config, res.getDisplayMetrics());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context = context.createConfigurationContext(config);
        }
        return context;
    }
}


