package nz.co.iswe.android.airplay;

import java.util.List;

public interface AudioControlService {

    boolean supportVolumeCtrl();

    /**
     * 获取当前音量
     *
     * @return 0-100
     */
    int getVolume();

    /**
     * 设置音量
     *
     * @param volume 0-100
     */
    void setVolume(int volume);


    // airplay (mute, 1-15, max) 级 音量 百分比对照表
    List<Integer> levelVolume();
}
