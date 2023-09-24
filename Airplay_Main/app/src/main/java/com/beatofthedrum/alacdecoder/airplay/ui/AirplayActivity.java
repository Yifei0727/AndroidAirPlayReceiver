package com.beatofthedrum.alacdecoder.airplay.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.yifei0727.adnroid.airplay.AirPlayPrefSetting;
import com.github.yifei0727.adnroid.airplay.service.AirPlayAndroidService;
import com.wh.R;

import java.util.logging.Logger;

/**
 * Created by Administrator on 2018/6/29.
 */

public class AirplayActivity extends Activity {
    private static final Logger LOG = Logger.getLogger(AirplayActivity.class.getName());

    private final ServiceConnection bindService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOG.info("onServiceConnected name = " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private boolean useBindService = false;
    private boolean run = false;
    private final Handler stateRefreshUIhandler = new Handler();

    private void connectAndStart() {
        Toast.makeText(this, getText(R.string.service_daemon_starting), Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent serviceIntent = new Intent(this, AirPlayAndroidService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            useBindService = true;
            bindService(serviceIntent, bindService, BIND_AUTO_CREATE);
        } else {
            AirPlayAndroidService service = AirPlayAndroidService.getInstance();
            if (null != service) {
                service.run((AudioManager) getSystemService(AUDIO_SERVICE));
            }
        }
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if (run) {
                final Switch stateSwitch = findViewById(R.id.switch5);
                stateSwitch.setEnabled(false);
                final Button button = findViewById(R.id.button);
                final AirPlayAndroidService airPlayAndroidService = AirPlayAndroidService.getInstance();
                if (null != airPlayAndroidService) {
                    try {
                        switch (airPlayAndroidService.getBackendState()) {
                            case RUNNING:
                                stateSwitch.setChecked(true);
                                button.setEnabled(true);
                                button.setText(getString(R.string.service_state_stop));
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        LOG.info("clicked, stop service ...");
                                        button.setEnabled(false);
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                airPlayAndroidService.pause();
                                            }
                                        }).start();
                                    }
                                });
                                break;
                            case STOPPED:
                                stateSwitch.setChecked(false);
                                button.setEnabled(true);
                                button.setText(getString(R.string.service_state_start));
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        LOG.info("clicked, start service ...");
                                        button.setEnabled(false);
                                        connectAndStart();
                                    }
                                });
                                break;
                            case LOCKED:
                                button.setEnabled(false);
                                stateSwitch.setChecked(false);
                                break;
                        }
                    } catch (Exception e) {
                        LOG.warning("refreshThread error " + e.getMessage());
                    }
                } else {
                    // 没有服务 自动启动
                    connectAndStart();
                }
                stateRefreshUIhandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiMainView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (useBindService) {
            //取消 Bind
            unbindService(bindService);
        }
    }

    private void uiMainView() {
        setContentView(R.layout.activity_main);
        run = true;
        final Switch stateSwitch = findViewById(R.id.switch5);
        stateSwitch.setEnabled(false);
        final Button button = findViewById(R.id.button);

        // 允许用户查看当前状态 、 启动或者停止服务
        final AirPlayAndroidService airPlayAndroidService = AirPlayAndroidService.getInstance();
        if (null == airPlayAndroidService) {
            // 没有服务 必然是停止状态
            LOG.info("airPlayAndroidService is null, first boot");
            stateSwitch.setChecked(false);
            button.setEnabled(false); //刚启动 等待会儿
            button.setText(getString(R.string.service_state_start));
        } else {
            LOG.info("airPlayAndroidService state = " + airPlayAndroidService.getBackendState());
        }
        Toast.makeText(this, getText(R.string.service_state_detected), Toast.LENGTH_SHORT).show();
        stateRefreshUIhandler.postDelayed(task, 1500);
    }

    private void uiSettingsView() {
        setContentView(R.layout.settings);
        run = false;
        // 根据用户配置 显示用户偏好
        final Switch autoStartSwitch = findViewById(R.id.switch3);
        autoStartSwitch.setChecked(AirPlayPrefSetting.isAutoStart(this));
        autoStartSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LOG.info("onCheckedChanged isChecked = " + isChecked);
                AirPlayPrefSetting.setAutoStart(AirplayActivity.this, isChecked);
            }
        });

        final TextView viewById = findViewById(R.id.editTextText);
        viewById.setText(AirPlayPrefSetting.getAirplayName(this));
        viewById.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!viewById.getText().toString().trim().isEmpty()) {
                    AirPlayPrefSetting.setAirplayName(AirplayActivity.this, viewById.getText().toString());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        LOG.info("onOptionsItemSelected id = " + id);
        if (id == R.id.action_settings) {
            uiSettingsView();
        } else {
            uiMainView();
        }
        return true;
    }
}
