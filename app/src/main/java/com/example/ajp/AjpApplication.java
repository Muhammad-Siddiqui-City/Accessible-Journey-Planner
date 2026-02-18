package com.example.ajp;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;

/**
 * Application entry point. Add in Commit 6 (after utils and themes).
 * Applies dark mode and locale at app start so the whole app uses the selected language.
 * Each Activity also applies locale in attachBaseContext so all screens respect the setting.
 */
public class AjpApplication extends Application {

    /* --- BLOCK: Locale at startup ---
     * PURPOSE: Apply user-chosen language before any UI is created.
     * WHY: LocaleHelper.applyFull() wraps the base context so resources and default locale match;
     *      must run in attachBaseContext so it's in place for Activity inflation.
     * ISSUES: If Application is not registered in AndroidManifest, this never runs.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyFull(base));
    }

    /* --- BLOCK: Dark mode at startup ---
     * PURPOSE: Set app-wide night mode from saved preference.
     * WHY: AppCompatDelegate.setDefaultNightMode affects all activities; doing it in onCreate
     *      ensures it's applied before first activity is shown.
     * ISSUES: None; ensure SettingsPrefs is available (add in Commit 5).
     */
    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = SettingsPrefs.get(this).isDarkMode();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
