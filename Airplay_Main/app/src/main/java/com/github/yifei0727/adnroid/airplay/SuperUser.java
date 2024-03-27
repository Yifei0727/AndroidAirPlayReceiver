package com.github.yifei0727.adnroid.airplay;

import com.beatofthedrum.alacdecoder.airplay.ui.AirplayActivity;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class SuperUser {

    private static final Logger LOG = Logger.getLogger(AirplayActivity.class.getName());

    public static boolean hasRoot() {
        Process process = null;
        boolean result = false;
        try {
            process = Runtime.getRuntime().exec("su");
            OutputStream outputStream = process.getOutputStream();
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            result = 0 == process.waitFor();
        } catch (Exception e) {
            LOG.info("SuperUser -- hasRoot: " + e.getMessage());
        } finally {
            if (null != process) process.destroy();
        }
        return result;
    }

    public static void exec(String... commands) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                os.writeBytes(command);
                os.writeBytes("\n");
                os.flush();
            }
            os.writeBytes("exit\n");
            os.flush();
            Thread.sleep(1000);
        } catch (Exception e) {
            LOG.info("SuperUser -- exec: " + e.getMessage());
        } finally {
            if (null != process) process.destroy();
        }
    }
}
