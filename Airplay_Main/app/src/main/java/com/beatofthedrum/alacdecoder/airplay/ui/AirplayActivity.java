package com.beatofthedrum.alacdecoder.airplay.ui;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nz.co.iswe.android.airplay.AirPlayDaemonPlayer;
import nz.co.iswe.android.airplay.AudioControlService;

/**
 * Created by Administrator on 2018/6/29.
 */

public class AirplayActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        final AirPlayDaemonPlayer airPlayDaemonPlayer = AirPlayDaemonPlayer.getInstance();
        airPlayDaemonPlayer.setVolumeControlService(new AudioControlService() {
            @Override
            public boolean supportVolumeCtrl() {
                return null != audioManager;
            }

            @Override
            public int getVolume() {
                if (supportVolumeCtrl()) {
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    return (int) (currentVolume * 100.0 / maxVolume);
                } else {
                    return 100;
                }
            }

            @Override
            public void setVolume(int volume) {
                if (supportVolumeCtrl()) {
                    if (volume < 1) {
                        volume = 0;
                    } else if (volume > 99) {
                        volume = 100;
                    }
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int currentVolume = (int) (volume * maxVolume / 100.0);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
                }
            }

            @Override
            public List<Integer> levelVolume() {
                List<Integer> preDefine = Arrays.asList(0, 10, 20, 30, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100);
                return Collections.unmodifiableList(preDefine);
            }

        });
        airPlayDaemonPlayer.setHostName(Build.MODEL);//使用设备名称 如果有多个同样设备 请注释掉这行
        airPlayDaemonPlayer.setRtspPort(5000);
        Thread thread = new Thread(airPlayDaemonPlayer);
        thread.start();
    }
}
