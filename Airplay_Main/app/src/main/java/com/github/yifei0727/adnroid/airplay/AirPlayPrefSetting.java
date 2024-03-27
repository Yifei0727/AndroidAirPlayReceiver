package com.github.yifei0727.adnroid.airplay;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class AirPlayPrefSetting {
    private static final String CONFIG = "config";
    private static final String NAME = "display_name";
    private static final String AUTO_START = "auto_start";
    private static final String USE_USB_LAN_FIRST = "use_usb_network_first";

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

    public static void setUsbLanFirst(Context Context, boolean usbFirst) {
        SharedPreferences sharedPreferences = Context.getSharedPreferences(CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(USE_USB_LAN_FIRST, usbFirst);
        edit.apply();
    }

    public static boolean isUsbLanFirst(Context Context) {
        SharedPreferences sharedPreferences = Context.getSharedPreferences(CONFIG, MODE_PRIVATE);
        return sharedPreferences.getBoolean(USE_USB_LAN_FIRST, false);
    }

}
