package com.github.yifei0727.android.airplay.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.yifei0727.android.airplay.AirPlayPrefSetting;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 如果配置了开机自启动 则开机后注册并启动后台服务
        if (AirPlayPrefSetting.isAutoStart(context)) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, AirPlayAndroidService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
