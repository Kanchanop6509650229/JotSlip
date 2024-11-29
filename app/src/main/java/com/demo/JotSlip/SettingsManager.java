package com.demo.JotSlip;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class SettingsManager {
    private static final String PREFS_NAME = "AppSettings";
    private static final String PREF_LANGUAGE = "language";

    private final Context context;
    private final SharedPreferences preferences;

    public SettingsManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ตั้งค่าภาษา
    public void setLanguage(String languageCode) {
        preferences.edit().putString(PREF_LANGUAGE, languageCode).apply();
    }

    // ดึงค่าภาษาปัจจุบัน
    public String getCurrentLanguage() {
        return preferences.getString(PREF_LANGUAGE, "th"); // ค่าเริ่มต้นเป็นภาษาไทย
    }

    // ใช้ภาษาที่ตั้งค่าไว้
    public void applyLanguage() {
        String languageCode = getCurrentLanguage();
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);

        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}