package com.github.yifei0727.adnroid.airplay;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class AirPlayPrefSetting {
    private static final String CONFIG = "config";
    private static final String NAME = "display_name";
    private static final String AUTO_START = "auto_start";

    public static String getAirplayName(Context Context) {
        SharedPreferences sharedPreferences = Context.getSharedPreferences(CONFIG, MODE_PRIVATE);
        String name = sharedPreferences.getString(NAME, Build.MODEL);
        if (null == name || name.trim().isEmpty()) {
            name = Build.MODEL;
        }
        return name.trim();
    }

    public static boolean isAutoStart(Context Context) {
        SharedPreferences sharedPreferences = Context.getSharedPreferences(CONFIG, MODE_PRIVATE);
        return sharedPreferences.getBoolean(AUTO_START, true);
    }


    public static void setAirplayName(Context Context, String name) {
        SharedPreferences sharedPreferences = Context.getSharedPreferences(CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(NAME, name == null ? Build.MODEL : name.trim());
        edit.apply();
    }

    public static void setAutoStart(Context Context, boolean autoStart) {
        SharedPreferences sharedPreferences = Context.getSharedPreferences(CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(AUTO_START, autoStart);
        edit.apply();
    }

}
