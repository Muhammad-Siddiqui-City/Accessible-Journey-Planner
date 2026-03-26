package com.example.ajp;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ajp.utils.LocaleHelper;
import com.example.ajp.utils.SettingsPrefs;






/**
 * Class for AjpApplication.
 */
public class AjpApplication extends Application {







    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyFull(base));
    }







    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = SettingsPrefs.get(this).isDarkMode();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}

