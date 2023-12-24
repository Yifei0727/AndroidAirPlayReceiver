package nz.co.iswe.android.airplay.network;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkUtils {

    private static final Logger LOG = Logger.getLogger(NetworkUtils.class.getName());

    private static NetworkUtils instance;
    private static final List<String> specialInterface = Arrays.asList("ap", "eth", "rndis");

    public static boolean isSpecialInterface(NetworkInterface ifc) {
        for (String name : specialInterface) {
            if (ifc.getDisplayName().toLowerCase().startsWith(name)) {
                return true;
            }
        }
        return false;
    }

    public static NetworkUtils getInstance() {
        if (instance == null) {
            instance = new NetworkUtils();
        }
        return instance;
    }

    private NetworkUtils() {

    }


    /**
     * Returns a suitable hardware address.
     *
     * @return a MAC address
     */
    public byte[] getHardwareAddress() {
        try {
            /* Search network interfaces for an interface with a valid, non-blocked hardware address */
            for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback()) {
                    continue;
                }
                if (iface.isPointToPoint()) {
                    continue;
                }

                try {
                    final byte[] ifaceMacAddress = iface.getHardwareAddress();
                    if ((ifaceMacAddress != null) && (ifaceMacAddress.length == 6) && !isBlockedHardwareAddress(ifaceMacAddress)) {
                        LOG.info("Hardware address is " + toHexString(ifaceMacAddress) + " (" + iface.getDisplayName() + ")");
                        return Arrays.copyOfRange(ifaceMacAddress, 0, 6);
                    }
                } catch (final Throwable e) {
                    /* Ignore */
                }
            }
        } catch (final Throwable e) {
            /* Ignore */
        }

        /* Fallback to the IP address padded to 6 bytes */
        try {
            final byte[] hostAddress = Arrays.copyOfRange(InetAddress.getLocalHost().getAddress(), 0, 6);
            LOG.info("Hardware address is " + toHexString(hostAddress) + " (IP address)");
            return hostAddress;
        } catch (final Throwable e) {
            /* Ignore */
        }

        /* Fallback to a constant */
        LOG.info("Hardware address is 00DEADBEEF00 (last resort)");
        return new byte[]{(byte) 0x00, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x00};
    }

    /**
     * Converts an array of bytes to a hexadecimal string
     *
     * @param bytes array of bytes
     * @return hexadecimal representation
     */
    private String toHexString(final byte[] bytes) {
        final StringBuilder s = new StringBuilder();
        for (final byte b : bytes) {
            final String h = Integer.toHexString(0x100 | b);
            s.append(h.substring(h.length() - 2).toUpperCase());
        }
        return s.toString();
    }

    /**
     * Decides whether or nor a given MAC address is the address of some
     * virtual interface, like e.g. VMware's host-only interface (server-side).
     *
     * @param addr a MAC address
     * @return true if the MAC address is unsuitable as the device's hardware address
     */
    public boolean isBlockedHardwareAddress(final byte[] addr) {
        if ((addr[0] & 0x02) != 0)
            /* Locally administered */
            return true;
        else if ((addr[0] == 0x00) && (addr[1] == 0x50) && (addr[2] == 0x56))
            /* VMware */
            return true;
        else if ((addr[0] == 0x00) && (addr[1] == 0x1C) && (addr[2] == 0x42))
            /* Parallels */
            return true;
        else if ((addr[0] == 0x00) && (addr[1] == 0x25) && (addr[2] == (byte) 0xAE))
            /* Microsoft */
            return true;
        else
            return false;
    }

    public String getHostUtils() {
        String hostName = "DroidAirPlay";
        try {
            String name = InetAddress.getLocalHost().getHostName();
            if (name != null && !name.isEmpty() && !name.toLowerCase().contains("localhost")) {
                hostName = name.split("\\.")[0];
            }
        } catch (final Throwable e) {
            //do nothing
        }
        return hostName;
    }


    public List<InetAddress> getNetworkAddresses(NetworkInterface iface) {
        Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
        if (!inetAddresses.hasMoreElements()) {
            final List<InetAddress> out = new ArrayList<InetAddress>();
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "/system/bin/ip addr show " + iface.getDisplayName()});
                int stauts = process.waitFor();
                if (stauts != 0) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    InputStream inputStream = process.getErrorStream();
                    while (inputStream.available() > 0) {
                        byteArrayOutputStream.write(inputStream.read());
                    }
                    LOG.info("EEE getip" + byteArrayOutputStream);
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                InputStream inputStream = process.getInputStream();
                while (inputStream.available() > 0) {
                    byteArrayOutputStream.write(inputStream.read());
                }
                String s = byteArrayOutputStream.toString();
                for (String line : s.split("\n")) {
                    String[] split;
                    if (line.contains("inet6")) {
                        split = line.split("inet6");
                    } else if (line.contains("inet")) {
                        split = line.split("inet");
                    } else {
                        continue;
                    }

                    if (split.length > 1) {
                        String ip = split[1].trim().split(" ")[0].trim().split("/")[0].trim().split("%")[0].trim();
                        LOG.info("Hardware address is " + ip + " (" + iface.getDisplayName() + ")");
                        try {
                            out.add(InetAddress.getByName(ip));
                        } catch (Throwable ignore) {
                        }
                    }
                }
            } catch (Exception e) {
                //ignore
            }
            return out;
        } else {
            return Collections.list(inetAddresses);
        }
    }

    public String getHardwareAddressString(NetworkInterface iface) {
        try {
            final byte[] ifaceMacAddress = iface.getHardwareAddress();
            if ((ifaceMacAddress != null) && (ifaceMacAddress.length == 6) && !isBlockedHardwareAddress(ifaceMacAddress)) {
                LOG.info("Hardware address is " + toHexString(ifaceMacAddress) + " (" + iface.getDisplayName() + ")");
                return toHexString(Arrays.copyOfRange(ifaceMacAddress, 0, 6));
            }
            if (isSpecialInterface(iface)) {
                // 适配某些
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "/system/bin/ip addr show " + iface.getDisplayName()});

                int status = process.waitFor();
                if (status != 0) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    InputStream inputStream = process.getErrorStream();
                    while (inputStream.available() > 0) {
                        byteArrayOutputStream.write(inputStream.read());
                    }
                    LOG.log(Level.WARNING, "getHardwareAddressString::" + byteArrayOutputStream);
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                InputStream inputStream = process.getInputStream();
                while (inputStream.available() > 0) {
                    byteArrayOutputStream.write(inputStream.read());
                }
                String s = byteArrayOutputStream.toString();
                LOG.log(Level.INFO, "Hardware detect  " + s);
                String[] split = s.split("link/ether");
                if (split.length > 1) {
                    String res = split[1].trim().split(" ")[0].trim();
                    String mac = res.replaceAll(":", "").toUpperCase();
                    LOG.info("Hardware address is " + mac + " (" + iface.getDisplayName() + ")");
                    return mac;
                }
            }
        } catch (final Throwable e) {
            /* Ignore */
        }
        return null;
    }

    public Set<NetworkInterface> getNetworkInterfaces() {
        Enumeration<NetworkInterface> networkInterfaces;
        final Set<NetworkInterface> networkInterfaceList = new HashSet<NetworkInterface>();
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces != null) {
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    {
                        if (networkInterface.isLoopback()) {
                            continue;
                        }
                        if (networkInterface.isPointToPoint()) {
                            continue;
                        }
                        if (networkInterface.isVirtual()) {
                            continue;
                        }
                        if (!networkInterface.isUp()) {
                            continue;
                        }

                        if ((!isSpecialInterface(networkInterface)) && null == getHardwareAddressString(networkInterface)) {
                            continue;
                        }
                    }
                    networkInterfaceList.add(networkInterface);
                }
            }
        } catch (SocketException ignore) {
        }
        return networkInterfaceList;
    }

}
