package com.example.ajp.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

/**
 * Applies saved language and font scale. Add in Commit 5.
 * PURPOSE: Wrap context with locale and (optionally) large-text scale; use in Application.attachBaseContext and Activity.
 * WHY: SettingsPrefs holds language; applyFull() applies locale + font scale so all screens respect it.
 * ISSUES: Must call in attachBaseContext before super; setLanguage uses commit() not apply() so value is written before recreate.
 */
// AI Generated
// Built with Claude
public final class LocaleHelper {

    /** Font scale when Large text is on; system default when off. */
    private static final float LARGE_TEXT_SCALE = 1.25f;

    /**
     * Wraps the context with locale and (optionally) large-text scale. Use this in attachBaseContext so the whole activity uses these settings.
     */
    /** Same method for every language: lang code -> BCP 47 tag -> Locale.forLanguageTag. */
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

    public static Context apply(Context context) {
        String lang = SettingsPrefs.get(context.getApplicationContext()).getLanguage();
        Locale locale = localeFromLang(lang);
        return wrap(context, locale, 1.0f);
    }

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
