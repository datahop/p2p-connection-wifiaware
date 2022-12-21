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

import datahop.WifiHotspot;
import datahop.WifiHotspotNotifier;

public class WifiAwareServer implements WifiHotspot{

    private static volatile WifiAwareServer mWifiHotspot;

    private static WifiHotspotNotifier notifier;

    private static final String TAG="WifiAware";
    private BroadcastReceiver broadcastReceiver;
    private WifiAwareManager wifiAwareManager;
    private ConnectivityManager connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private NetworkSpecifier networkSpecifier;
    private Context context;

    private byte[] port, peerId,status,ip;
    private int peerIdLength, statusLength;

    public static final int  PEERID_MESSAGE = 55;
    public static final int  STATUS_MESSAGE = 66;
    public static final int  PORT_MESSAGE = 77;
    public static final int  IP_MESSAGE = 88;

    private boolean serverStarted,sent;

    public WifiAwareServer(Context context){
        wifiAwareManager = null;
        wifiAwareSession = null;
        networkSpecifier = null;
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        this.context = context;
    }

    /* Singleton method that returns a WifiDirectHotSpot instance
     * @return WifiDirectHotSpot instance
     */
    public static synchronized WifiAwareServer getInstance(Context appContext) {
        if (mWifiHotspot == null) {
            mWifiHotspot = new WifiAwareServer(appContext);
            // initDriver();
        }
        return mWifiHotspot;
    }

    /**
     * Set the notifier that receives the events advertised
     * when creating or destroying the group or when receiving users connections
     * @param notifier instance
     */
    public void setNotifier(WifiHotspotNotifier notifier) {
        //Log.d(TAG, "Trying to start");
        this.notifier = notifier;
    }


    @Override
    public void start() {
        if (notifier == null) {
            Log.e(TAG, "notifier not found");
            return;
        }
    }

    @Override
    public void stop() {

    }

    @TargetApi(26)
    private void requestNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (networkSpecifier == null) {
            Log.d("myTag", "No NetworkSpecifier Created ");
            return;
        }
        Log.d("myTag", "building network interface");
        Log.d("myTag", "using networkspecifier: " + networkSpecifier.toString());
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .setNetworkSpecifier(networkSpecifier)
                .build();

        Log.d("myTag", "finish building network interface");
        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d("myTag", "Network Available: " + network.toString());
            }

            @Override
            public void onLosing(Network network, int maxMsToLive) {
                super.onLosing(network, maxMsToLive);
                Log.d("myTag", "losing Network");
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d("myTag", "Lost Network");
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.d("myTag", "entering onUnavailable ");
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                WifiAwareNetworkInfo peerAwareInfo = (WifiAwareNetworkInfo) networkCapabilities.getTransportInfo();
                Inet6Address peerIpv6 = peerAwareInfo.getPeerIpv6Addr();
                int peerPort = peerAwareInfo.getPort();
                Log.d("myTag", "entering onCapabilitiesChanged "+peerIpv6+" "+peerPort);
                /*try {
                    if(ip!=null&&!sent) {
                        Log.d("subscribeToService", "sending file");
                        Client.clientSendFile(Inet6Address.getByAddress("WifiAwareHost", ip, peerIpv6.getScopedInterface()), byteToPortInt(port));
                        sent=true;
                    }

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }*/
            }

            //-------------------------------------------------------------------------------------------- +++++
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                //TODO: create socketServer on different thread to transfer files

                Log.d("myTag", "entering linkPropertiesChanged ");

                //TODO: create socketServer on different thread to transfer files
                try {

                    NetworkInterface awareNi = NetworkInterface.getByName(
                            linkProperties.getInterfaceName());

                    Enumeration<InetAddress> Addresses = awareNi.getInetAddresses();
                    while (Addresses.hasMoreElements()) {
                        InetAddress addr = Addresses.nextElement();
                        if (addr instanceof Inet6Address) {
                            Log.d("myTag", "netinterface ipv6 address: " + addr.toString());
                            if (((Inet6Address) addr).isLinkLocalAddress()) {

                                byte[] myIP = addr.getAddress();
                                Log.d("myTag","sending top "+new String(myIP));
                                /*if (pub.getSession() != null && serverStarted) {
                                    Log.d("myTag","sending to subs");
                                    pub.sendIP(myIP);
                                }*/
                                break;
                            }
                        }
                    }
                }
                catch (SocketException e) {
                    Log.d("myTag", "socket exception " + e.toString());
                }
                catch (Exception e) {
                    //EXCEPTION!!! java.lang.NullPointerException: Attempt to invoke virtual method 'java.util.Enumeration java.net.NetworkInterface.getInetAddresses()' on a null object reference
                    Log.d("myTag", "EXCEPTION!!! " + e.toString());
                }
                //Log.d("myTag", "entering linkPropertiesChanged "+peerIpv6+" "+peerPort+" "+otherIP);

            }
            //-------------------------------------------------------------------------------------------- -----

        });
    }


    public int byteToPortInt(byte[] bytes){
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }

    public byte[] portToBytes(int port){
        byte[] data = new byte [2];
        data[0] = (byte) (port & 0xFF);
        data[1] = (byte) ((port >> 8) & 0xFF);
        return data;
    }

}
