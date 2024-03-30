package com.github.yifei0727.android.airplay;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import nz.co.iswe.android.airplay.network.NetworkUtils;

public class NetworkCardManager {
    // if enabled auto start rndis0


    public static void startLanNetwork() {
        List<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            return;
        }
        for (NetworkInterface ni : networkInterfaces) {
            if (ni.getName() != null && ni.getName().toLowerCase().startsWith("rndis") || ni.getName().toLowerCase().startsWith("eth") || ni.getName().toLowerCase().startsWith("usb")) {
                try {
                    NetworkUtils.getInstance().enableNetworkCard(ni);
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }


}
