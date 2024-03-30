package com.github.yifei0727.android.airplay;

import com.github.yifei0727.android.airplay.ui.AirplayActivity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class SuperUser {

    private static final Logger LOG = Logger.getLogger(AirplayActivity.class.getName());

    private static boolean permitUseRoot = false;

    public static void setPermitUseRoot(boolean permitUseRoot) {
        SuperUser.permitUseRoot = permitUseRoot;
    }

    public static boolean hasRoot() {
        if (!permitUseRoot) return false;
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

    /**
     * execute command with root permission
     *
     * @param commands command list to execute
     * @throws IOException if root is not permitted
     */
    public static void exec(String... commands) throws IOException {
        if (!permitUseRoot) {
            throw new IOException("root is not permitted");
        }
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
