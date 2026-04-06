package com.example.ajp;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;

/**
 * Core class for AjpApplication.
 * Provides app-level behavior required by surrounding features.
 * Responsibilities are kept scoped so this class can be reasoned about quickly.
 */

public class AjpApplication extends Application {

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyFull(base));
    }

    @Override
    // Initializes screen state, wiring, and startup behavior for this lifecycle stage.
    public void onCreate() {
        super.onCreate();
        boolean dark = SettingsPrefs.get(this).isDarkMode();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}


