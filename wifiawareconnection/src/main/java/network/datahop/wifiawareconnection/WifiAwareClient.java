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
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import datahop.WifiAwareClientDriver;
import datahop.WifiAwareNotifier;

public class WifiAwareClient implements Subscription.Subscribed, WifiAwareClientDriver{

    private static volatile WifiAwareClient mWifiHotspot;

    private static WifiAwareNotifier notifier;

    private static final String TAG="WifiAware";
    private BroadcastReceiver broadcastReceiver;
    private WifiAwareManager wifiAwareManager;
    private ConnectivityManager connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private NetworkCapabilities networkCapabilities_;
    private NetworkSpecifier networkSpecifier;
    private Context context;
    private Handler mHandler;
    private byte[]  peerId,ip;
    private int port;

    public static final int  PEERID_MESSAGE = 55;
    public static final int  STATUS_MESSAGE = 66;
    public static final int  PORT_MESSAGE = 77;
    public static final int  IP_MESSAGE = 88;

    private Subscription sub;

    public WifiAwareClient(Context context){
        wifiAwareManager = null;
        wifiAwareSession = null;
        networkSpecifier = null;
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.sub = new Subscription(this);
        this.context = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /* Singleton method that returns a WifiDirectHotSpot instance
     * @return WifiDirectHotSpot instance
     */
    public static synchronized WifiAwareClient getInstance(Context appContext) {
        if (mWifiHotspot == null) {
            mWifiHotspot = new WifiAwareClient(appContext);
            // initDriver();
        }
        return mWifiHotspot;
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


    @Override
    public void connect(String peerId) {
        if (notifier == null) {
            Log.e(TAG, "notifier not found");
            return;
        }
        startManager(peerId);
    }

    private boolean startManager(String peerId){
        Log.d(TAG,"Starting Wifi Aware");
        PackageManager packageManager = context.getPackageManager();
        boolean hasNan  = false;
        this.peerId = peerId.getBytes(StandardCharsets.UTF_8);

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

    public void startDiscovery(WifiAwareSession session){
        sub.closeSession();
        sub.subscribeToService(session,peerId);
    }

    private void closeSession() {

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
    }

    @Override
    public void disconnect() {
        sub.closeSession();
        closeSession();
    }

    @TargetApi(26)
    private void requestNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (networkSpecifier == null) {
            Log.d(TAG, "No NetworkSpecifier Created ");
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
                Log.d(TAG, "onAvailable");
                mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                while (networkCapabilities_ == null) {
                                }
                                WifiAwareNetworkInfo peerAwareInfo = (WifiAwareNetworkInfo) networkCapabilities_.getTransportInfo();
                                Inet6Address peerIpv6 = peerAwareInfo.getPeerIpv6Addr();
                                Log.d(TAG, "Network Available: " + peerIpv6.getHostAddress()+" "+port);
                                notifier.onConnectionSuccess(peerIpv6.getHostAddress(),port,new String(peerId));

                            }
                        }
                );

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
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                Log.d(TAG, "entering onCapabilitiesChanged");
                networkCapabilities_ = networkCapabilities;

            }

            //-------------------------------------------------------------------------------------------- -----

        });
    }


    public int byteToPortInt(byte[] bytes){
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }


    @Override
    public void messageReceived(byte[] message)  {

        if (message.length == 2) {
                networkSpecifier = sub.specifyNetwork();
                Log.d(TAG, "Starting connection");
                requestNetwork();
                this.port = byteToPortInt(message);

        }
    }

    @Override
    public String host(){
        return new String(peerId);
    }
}
