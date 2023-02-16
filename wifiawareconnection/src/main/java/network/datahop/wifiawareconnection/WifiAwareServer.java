package network.datahop.wifiawareconnection;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import datahop.Datahop;
import datahop.WifiAwareServerDriver;
import datahop.WifiAwareNotifier;

public class WifiAwareServer implements  Publication.Published, WifiAwareServerDriver {

    private static volatile WifiAwareServer mWifiAwareServer;

    private static WifiAwareNotifier notifier;

    private static final String TAG="WifiAware";
    private BroadcastReceiver broadcastReceiver;
    private WifiAwareManager wifiAwareManager;
    private ConnectivityManager connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private NetworkSpecifier networkSpecifier;
    private Context context;

    private byte[] port, peerId;

    public static final int  PEERID_MESSAGE = 55;
    public static final int  STATUS_MESSAGE = 66;
    public static final int  PORT_MESSAGE = 77;
    public static final int  IP_MESSAGE = 88;

    private boolean serverStarted;
    private Publication pub;
    public WifiAwareServer(Context context){
        wifiAwareManager = null;
        wifiAwareSession = null;
        networkSpecifier = null;
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.pub = new Publication(this);
        this.context = context;
        this.serverStarted = false;
    }

    /* Singleton method that returns a WifiDirectHotSpot instance
     * @return WifiDirectHotSpot instance
     */
    public static synchronized WifiAwareServer getInstance(Context appContext) {
        if (mWifiAwareServer == null) {
            mWifiAwareServer = new WifiAwareServer(appContext);
            // initDriver();
        }
        return mWifiAwareServer;
    }

    /**
     * Set the notifier that receives the events advertised
     * when creating or destroying the group or when receiving users connections
     * @param notifier instance
     */
    public void setNotifier(WifiAwareNotifier notifier) {
        //Log.d(TAG, "Trying to start");
        this.notifier = notifier;
    }


    public void start(String peerId, long port) {
        if (notifier == null) {
            Log.e(TAG, "notifier not found");
            return;
        }
        startManager(peerId,port);
    }

    public void stop() {

    }

    public boolean startManager(String peerId, long port){
        Log.d(TAG,"Starting Wifi Aware ");
        PackageManager packageManager = context.getPackageManager();
        boolean hasNan  = false;
        this.peerId = peerId.getBytes(StandardCharsets.UTF_8);
        this.port = portToBytes(port);
        this.serverStarted = false;
        if (packageManager == null) {
            return false;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasNan = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
            }
        }

        if (!hasNan) {
            return false;
        } else {

            wifiAwareManager = (WifiAwareManager)context.getSystemService(Context.WIFI_AWARE_SERVICE);

            if (wifiAwareManager == null) {
                return false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Entering OnResume is executed");
            IntentFilter filter   = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
            broadcastReceiver     = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    wifiAwareManager.getCharacteristics();
                    boolean nanAvailable = wifiAwareManager.isAvailable();
                    Log.d(TAG, "NAN is available");
                    if (nanAvailable) {
                        attachToNanSession();
                        Log.d(TAG, "NAN attached");
                    } else {
                        Log.d(TAG, "NAN unavailable");
                        //return false;
                    }

                }
            };

            context.registerReceiver(broadcastReceiver, filter);

            boolean nanAvailable = wifiAwareManager.isAvailable();
            if (nanAvailable) {
                attachToNanSession();
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Handles attaching to NAN session.
     *
     */
    @TargetApi(26)
    private void attachToNanSession() {
        Log.d(TAG,"attachToNanSession");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Only once
        if (wifiAwareSession != null) {
            return;
        }

        if (wifiAwareManager == null || !wifiAwareManager.isAvailable()) {
            //setStatus("NAN is Unavailable in attach");
            return;
        }

        Log.d(TAG,"attaching...");

        wifiAwareManager.attach(new AttachCallback() {
            @Override
            public void onAttached(WifiAwareSession session) {
                super.onAttached(session);
                Log.d(TAG,"onAttached");

                closeSession();
                wifiAwareSession = session;
                //setHaveSession(true);
                startDiscovery(wifiAwareSession);
            }

            @Override
            public void onAttachFailed() {
                super.onAttachFailed();
                //setHaveSession(false);
                //setStatus("attach() failed.");
                Log.d(TAG,"attach() failed");
            }

        }, new IdentityChangedListener() {
            @Override
            public void onIdentityChanged(byte[] mac) {
                super.onIdentityChanged(mac);
                //setMacAddress(mac);
            }
        }, null);
    }

    private void startDiscovery(WifiAwareSession session){
        pub.closeSession();
        pub.publishService(session,port,peerId);
    }

    private void closeSession() {

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
    }


    @TargetApi(26)
    private void requestNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (networkSpecifier == null) {
            Log.d(TAG, "No NetworkSpecifier Created ");
            notifier.onConnectionFailure("No NetworkSpecifier Created ");
            return;
        }
        Log.d(TAG, "building network interface");
        Log.d(TAG, "using networkspecifier: " + networkSpecifier.toString());
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .setNetworkSpecifier(networkSpecifier)
                .build();

        Log.d(TAG, "finish building network interface");
        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network Available: " + network.toString());
            }

            @Override
            public void onLosing(Network network, int maxMsToLive) {
                super.onLosing(network, maxMsToLive);
                Log.d(TAG, "losing Network");
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(TAG, "Lost Network");
                notifier.onDisconnect();
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.d(TAG, "entering onUnavailable ");
                notifier.onConnectionFailure("network unavailable");

            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                WifiAwareNetworkInfo peerAwareInfo = (WifiAwareNetworkInfo) networkCapabilities.getTransportInfo();
                Inet6Address peerIpv6 = peerAwareInfo.getPeerIpv6Addr();
                int peerPort = peerAwareInfo.getPort();
                Log.d(TAG, "entering onCapabilitiesChanged "+peerIpv6+" "+peerPort);

            }

            //-------------------------------------------------------------------------------------------- +++++
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                //TODO: create socketServer on different thread to transfer files

                Log.d(TAG, "entering linkPropertiesChanged ");

                //TODO: create socketServer on different thread to transfer files
                try {

                    NetworkInterface awareNi = NetworkInterface.getByName(
                            linkProperties.getInterfaceName());

                    Enumeration<InetAddress> Addresses = awareNi.getInetAddresses();
                    while (Addresses.hasMoreElements()) {
                        InetAddress addr = Addresses.nextElement();
                        if (addr instanceof Inet6Address) {
                            Log.d(TAG, "netinterface ipv6 address: " + addr.toString());
                            if (((Inet6Address) addr).isLinkLocalAddress()) {


                                if (pub.getSession() != null && serverStarted) {


                                    String peerid = notifier.onConnectionServerSuccess(addr.getHostAddress().split("%")[0],addr.getHostAddress().split("%")[1],byteToPortInt(port));
                                    Log.d(TAG,"connection server "+peerid);

                                    pub.sendIP(peerid.getBytes());
                                }
                                break;
                            }
                        }
                    }
                }
                catch (SocketException e) {
                    Log.d(TAG, "socket exception " + e.toString());
                }
                catch (Exception e) {
                    //EXCEPTION!!! java.lang.NullPointerException: Attempt to invoke virtual method 'java.util.Enumeration java.net.NetworkInterface.getInetAddresses()' on a null object reference
                    Log.d(TAG, "EXCEPTION!!! " + e.toString());
                }
                //Log.d(TAG, "entering linkPropertiesChanged "+peerIpv6+" "+peerPort+" "+otherIP);

            }
            //-------------------------------------------------------------------------------------------- -----

        });
    }


    public int byteToPortInt(byte[] bytes){
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }

    public byte[] portToBytes(long port){
        byte[] data = new byte [2];
        data[0] = (byte) (port & 0xFF);
        data[1] = (byte) ((port >> 8) & 0xFF);
        return data;
    }

    @Override
    public void messageReceived(byte[] message) {
        if (message.length == 2) {
            //if (message.hashCode() < status.hashCode()) {
                networkSpecifier = pub.specifyNetwork(port);
                Log.d(TAG, "Starting connection");
                requestNetwork();
                //Server.startServer(byteToPortInt(port),3);
                serverStarted=true;
            //}
        }
    }
}
