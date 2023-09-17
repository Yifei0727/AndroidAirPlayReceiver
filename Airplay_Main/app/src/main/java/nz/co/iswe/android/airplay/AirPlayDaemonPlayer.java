package nz.co.iswe.android.airplay;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import nz.co.iswe.android.airplay.network.NetworkUtils;
import nz.co.iswe.android.airplay.network.raop.RaopRtspPipelineFactory;

/**
 * Android AirPlay Server Implementation
 *
 * @author Rafael Almeida
 */
public class AirPlayDaemonPlayer implements Runnable {

    private static final Logger LOG = Logger.getLogger(AirPlayDaemonPlayer.class.getName());

    /**
     * The AirTunes/RAOP service type
     */
    static final String AIR_TUNES_SERVICE_TYPE = "_raop._tcp.local.";

    /**
     * The AirTunes/RAOP M-DNS service properties (TXT record)
     */
    static final Map<String, String> AIRTUNES_SERVICE_PROPERTIES = map(
            "txtvers", "1",
            "tp", "UDP",
            "ch", "2",
            "ss", "16",
            "sr", "44100",
            "pw", "false",
            "sm", "false",
            "sv", "false",
            "ek", "1",
            "et", "0,1",
            "cn", "0,1",
            "vn", "3"
    );

    private static AirPlayDaemonPlayer instance = null;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isStopping = new AtomicBoolean(false);

    public static synchronized AirPlayDaemonPlayer getInstance() {
        if (instance == null) {
            instance = new AirPlayDaemonPlayer();
        }
        return instance;
    }

    /**
     * Global executor service. Used e.g. to initialize the various netty channel factories
     */
    protected ExecutorService executorService;

    /**
     * Channel execution handler. Spreads channel message handling over multiple threads
     */
    protected ExecutionHandler channelExecutionHandler;

    /**
     * All open RTSP channels. Used to close all open challens during shutdown.
     */
    protected ChannelGroup channelGroup;

    /**
     * JmDNS instances (one per IP address). Used to unregister the mDNS services
     * during shutdown.
     */
    protected final List<JmDNS> jmDNSInstances;


    /* Create AirTunes RTSP server */
    private ServerBootstrap airTunesRtspBootstrap;

    /* mDns publisher */
    private ExecutorService mDnsPublisher;

    /**
     * The AirTunes/RAOP RTSP port
     */
    private int rtspPort = 5000; //default value

    private String hostName = null; //default use the host name of the device


    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    private AirPlayDaemonPlayer() {
        //list of mDNS services
        jmDNSInstances = new java.util.LinkedList<JmDNS>();
    }

    /**
     * @see #onShutdown()
     */
    private void initResource() {
        //create executor service
        executorService = Executors.newCachedThreadPool();

        //create mDns publisher service
        mDnsPublisher = Executors.newSingleThreadExecutor();

        //create channel execution handler
        channelExecutionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(4, 0, 0));

        //channel group
        channelGroup = new DefaultChannelGroup();

        // create rtsp server
        airTunesRtspBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(executorService, executorService));
    }

    public int getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(int rtspPort) {
        this.rtspPort = rtspPort;
    }

    public void run() {
        isStopping.set(false);
        synchronized (AirPlayDaemonPlayer.class) {
            if (isRunning.get()) {
                LOG.info("AirPlayDaemonPlayer is already running. won't start again");
                return;
            }
            isRunning.set(true);
            initResource();
            startService();
        }
    }


    private void startService() {
        /* Make sure AirPlay Server shuts down gracefully */
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                onShutdown();
            }
        }));

        LOG.info("VM Shutdown Hook added successfully!");
        airTunesRtspBootstrap.setPipelineFactory(new RaopRtspPipelineFactory());
        airTunesRtspBootstrap.setOption("reuseAddress", true);
        airTunesRtspBootstrap.setOption("child.tcpNoDelay", true);
        airTunesRtspBootstrap.setOption("child.keepAlive", true);
        try {
            channelGroup.add(airTunesRtspBootstrap.bind(new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), getRtspPort())));
        } catch (UnknownHostException e) {
            LOG.log(Level.SEVERE, "Failed to bind RTSP Bootstrap on port: " + getRtspPort(), e);
        }

        LOG.info("Launched RTSP service on port " + getRtspPort());

        mDnsPublishJob();
    }

    // 如果网络发生变化，监听不变但是 mDNS 需要在有效网卡上重新广播
    private void mDnsPublishJob() {
        mDnsPublisher.execute(new Runnable() {
            @Override
            public void run() {
                while (isRunning.get() && !isStopping.get()) {
                    registerOrUpdateMdns();
                    try {
                        for (int i = 0; i < 3333 && isRunning.get() && !isStopping.get(); i++) {
                            Thread.sleep(1);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                unregisterOrUpdateMdns();
            }
        });
    }


    private void unregisterOrUpdateMdns() {
        synchronized (jmDNSInstances) {
            for (final JmDNS jmDNS : jmDNSInstances) {
                try {
                    LOG.info("Unregistered all services : " + jmDNS.getHostName());
                    jmDNS.unregisterAllServices();
                    jmDNS.close();
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "Failed to unregister some services", e);
                }
            }
            jmDNSInstances.clear();
            lastInterface.clear();
        }
        // 最后一个被关闭的服务 直接 重置状态
        isStopping.set(false);
    }

    private final Map<String, Set<String>> lastInterface = new ConcurrentHashMap<String, Set<String>>();

    private void registerOrUpdateMdns() {
        //get Network details
        final NetworkUtils networkUtils = NetworkUtils.getInstance();
        final String hostName = null != instance.hostName ? instance.hostName : networkUtils.getHostUtils();

        /* Create mDNS responders. */
        synchronized (jmDNSInstances) {
            final Set<NetworkInterface> currentEnabledInterface = networkUtils.getNetworkInterfaces();

            for (final NetworkInterface iface : currentEnabledInterface) {
                final Set<String> ipAddresses = lastInterface.get(iface.getName()) != null ? lastInterface.get(iface.getName()) : new HashSet<String>();
                // 仅处理新增的 IP 地址
                final String hardwareAddressString = networkUtils.getHardwareAddressString(iface);
                if (null == hardwareAddressString) {
                    LOG.info("Ignoring network interface " + iface.getName() + " because it has no hardware address");
                    continue; // should not happen
                }
                for (final InetAddress addr : networkUtils.getNetworkAddresses(iface)) {
                    if (!(addr instanceof Inet4Address) && !(addr instanceof Inet6Address)) {
                        LOG.info("Ignoring non-IP address " + iface.getName() + " " + addr);
                        continue;
                    }
                    if (ipAddresses.contains(addr.getHostAddress())) {
                        LOG.info("Ignoring duplicate address " + iface.getName() + " " + addr);
                        continue;
                    }

                    try {
                        final String name = hostName + "-jmdns";
                        // 一块网卡 刚起来或者 刚下线 此网卡的地址
                        LOG.info("prepare add new mDNS responder for address " + String.format("%s  name is %s", addr.getHostAddress(), name));
                        /* Create mDNS responder for address */
                        final JmDNS jmDNS = JmDNS.create(addr, name);
                        jmDNSInstances.add(jmDNS);

                        /* Publish RAOP service */
                        final ServiceInfo airTunesServiceInfo = ServiceInfo.create(
                                AIR_TUNES_SERVICE_TYPE,
                                hardwareAddressString + "@" + hostName + "(" + iface.getName() + ")",
                                getRtspPort(),
                                0 /* weight */, 0 /* priority */,
                                AIRTUNES_SERVICE_PROPERTIES
                        );
                        jmDNS.registerService(airTunesServiceInfo);
                        ipAddresses.add(addr.getHostAddress());
                        LOG.info("Registered AirTunes service '" + airTunesServiceInfo.getName() + "' on " + addr);
                    } catch (final Throwable e) {
                        LOG.log(Level.SEVERE, "Failed to publish service on " + addr, e);
                    } finally {
                        LOG.info("Registered AirTunes service Total " + jmDNSInstances.size() + " Details " + jmDNSInstances);
                    }
                }
                lastInterface.put(iface.getName(), ipAddresses);
            }
        }

    }

    //When the app is shutdown
    protected void onShutdown() {
        /* Close channels */
        final ChannelGroupFuture allChannelsClosed = channelGroup.close();

        /* Stop all mDNS responders */
        synchronized (jmDNSInstances) {
            for (final JmDNS jmDNS : jmDNSInstances) {
                try {
                    jmDNS.unregisterAllServices();
                    LOG.info("Unregistered all services on " + jmDNS.getInterface());
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "Failed to unregister some services", e);

                }
            }
        }

        /* Wait for all channels to finish closing */
        allChannelsClosed.awaitUninterruptibly();

        /* Stop the ExecutorService */
        executorService.shutdown();

        /* Release the OrderedMemoryAwareThreadPoolExecutor */
        channelExecutionHandler.releaseExternalResources();
        /* Release the ServerBootstrap */
        airTunesRtspBootstrap.releaseExternalResources();

        /* Reset state to stop */
        isRunning.set(false);
    }

    /**
     * Map factory. Creates a Map from a list of keys and values
     *
     * @param keys_values key1, value1, key2, value2, ...
     * @return a map mapping key1 to value1, key2 to value2, ...
     */
    private static Map<String, String> map(final String... keys_values) {
        assert keys_values.length % 2 == 0;
        final Map<String, String> map = new java.util.HashMap<String, String>(keys_values.length / 2);
        for (int i = 0; i < keys_values.length; i += 2)
            map.put(keys_values[i], keys_values[i + 1]);
        return Collections.unmodifiableMap(map);
    }

    public ChannelHandler getChannelExecutionHandler() {
        return channelExecutionHandler;
    }

    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    private AudioControlService audioControlService;

    public void setVolumeControlService(AudioControlService audioControlService) {
        this.audioControlService = audioControlService;
    }

    public AudioControlService getVolumeControlService() {
        return audioControlService;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isStopping() {
        return isStopping.get();
    }

    public void stop() {
        isStopping.set(true);
        synchronized (AirPlayDaemonPlayer.class) {
            if (!isRunning.get()) {
                LOG.info("AirPlayDaemonPlayer is already stopped");
                return;
            }
            LOG.info("AirPlayDaemonPlayer is stopping");
            onShutdown();
        }
    }
}
