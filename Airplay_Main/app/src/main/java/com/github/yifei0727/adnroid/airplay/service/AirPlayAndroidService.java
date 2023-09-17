package com.github.yifei0727.adnroid.airplay.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.github.yifei0727.adnroid.airplay.AirPlayPrefSetting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import nz.co.iswe.android.airplay.AirPlayDaemonPlayer;
import nz.co.iswe.android.airplay.AudioControlService;

public class AirPlayAndroidService extends Service {
    private static final Logger LOG = Logger.getLogger(AirPlayAndroidService.class.getName());
    private AirPlayDaemonPlayer airPlayDaemonPlayer;

    public BackendState getBackendState() {
        synchronized (AirPlayAndroidService.class) {
            if (airPlayDaemonPlayer.isRunning() && airPlayDaemonPlayer.isStopping()) {
                return BackendState.LOCKED;
            }
            if (airPlayDaemonPlayer.isRunning()) {
                return BackendState.RUNNING;
            }
            return BackendState.STOPPED;
        }
    }


    public void run(final AudioManager audioManager) {
        if (airPlayDaemonPlayer.isStopping()) {
            LOG.info("AirPlayAndroidService is stopping, try again later ...");
            return;
        }
        if (airPlayDaemonPlayer.isRunning()) {
            LOG.info("AirPlayAndroidService is running");
            return;
        }
        LOG.info("AirPlayAndroidService start ...");
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

        airPlayDaemonPlayer.setHostName(AirPlayPrefSetting.getAirplayName(this));//使用设备名称 如果有多个同样设备 请注释掉这行
        airPlayDaemonPlayer.setRtspPort(5000);
        Thread thread = new Thread(airPlayDaemonPlayer);
        thread.start();
        // 后端服务启动中 或者 启动完成
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LOG.info("AirPlayAndroidService onBind ...");
        run((AudioManager) getSystemService(AUDIO_SERVICE));
        return null;
    }

    @Override
    public void onCreate() {
        airPlayAndroidService = this;
        LOG.info("AirPlayAndroidService onCreate ...");
        airPlayDaemonPlayer = AirPlayDaemonPlayer.getInstance();
    }

    private static AirPlayAndroidService airPlayAndroidService;

    public static AirPlayAndroidService getInstance() {
        return airPlayAndroidService;
    }

    public void pause() {
        LOG.info("AirPlayAndroidService pause ...");
        airPlayDaemonPlayer.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("AirPlayAndroidService onStartCommand ...");
        run((AudioManager) getSystemService(AUDIO_SERVICE));
        return START_STICKY;
    }


    public enum BackendState {
        RUNNING, // 运行中
        LOCKED, // 启动或者关闭中
        STOPPED // 未运行
    }

    @Override
    public void onDestroy() {
        LOG.info("AirPlayAndroidService onDestroy ...");
        LOG.info("AirPlayAndroidService stop ...");
        airPlayDaemonPlayer.stop();
        super.onDestroy();
    }

}
