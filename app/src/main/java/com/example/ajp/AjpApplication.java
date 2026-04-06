package com.example.ajp;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;

/** Application: applies saved locale and dark-mode preference before the first activity. */
public class AjpApplication extends Application {

    @Override

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyFull(base));
    }

    @Override

    public void onCreate() {
        super.onCreate();
        boolean dark = SettingsPrefs.get(this).isDarkMode();
        // AppCompat names this API "night"; it drives dark theme + values-night/ resources for our Dark Mode toggle.
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}


