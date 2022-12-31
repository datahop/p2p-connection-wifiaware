package network.datahop.wifiawareconnectiondemo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;


import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import datahop.WifiAwareNotifier;

import network.datahop.wifiawareconnection.WifiAwareClient;
import network.datahop.wifiawareconnection.WifiAwareServer;

public class MainActivity extends AppCompatActivity implements WifiAwareNotifier {


    private Button startHSButton,stopHSButton,connectButton,disconnectButton;

    private TextView peerIdClient, statusClient;
    private TextView peerIdServer, statusServer;

    private String statusClientString, statusServerString, peerIdServerString, peerIdClientString;
    private WifiAwareServer hotspot;
    private WifiAwareClient connection;

    private int counter,port;
    private boolean stopping;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_WIFI_STATE = 2;

    private static final String TAG = "WifiTransportDemo";
    private boolean server,client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        server=client=false;
        counter=0;
        stopping=false;
        port=4353;
        hotspot = WifiAwareServer.getInstance(getApplicationContext());
        connection = WifiAwareClient.getInstance(getApplicationContext());
        hotspot.setNotifier(this);
        connection.setNotifier(this);
        startHSButton = (Button) findViewById(R.id.activatebutton);
        connectButton = (Button) findViewById(R.id.connectbutton);

        stopHSButton = (Button) findViewById(R.id.stopbutton);
        disconnectButton = (Button) findViewById(R.id.disconnectbutton);

        peerIdClient = (TextView) findViewById(R.id.network);
        statusClient = (TextView) findViewById(R.id.password);

        peerIdServer = (TextView) findViewById(R.id.textview_ssid);
        statusServer = (TextView) findViewById(R.id.textview_pass);

        peerIdClientString = "peerId2";
        peerIdServerString = "peerId1";

        statusServerString = "status1";
        statusClientString = "status2";

        peerIdServer.setText("PeerID: "+peerIdServerString);
        peerIdClient.setText("PeerID: "+peerIdClientString);
        statusClient.setText("Status: "+statusClientString);
        statusServer.setText("Status: "+statusServerString);


        startHSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Starting HS");
                hotspot.start(peerIdServerString,port);
                server=true;
            }
        });

        stopHSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Stopping HS");
                hotspot.stop();
                server=false;
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection.disconnect();
                client=false;
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Connecting ");
                connection.connect(peerIdServerString);
                client=true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        hotspot.stop();
        super.onDestroy();
    }


    @Override
    public void onConnectionFailure(String message) {
        Log.d(TAG,"Connection failed "+message);
    }

    @Override
    public void onConnectionSuccess(String ip, long port, String peerId) {
        Log.d(TAG,"Connection succeeded "+ip+" "+port+" "+peerId);
        if(server)
            Server.startServer((int)port,3);
        else if(client) {
            Client.clientSendFile(ip, (int)port);
        }


    }

    @Override
    public void onDisconnect() {
        Log.d(TAG,"Disconnected");

    }

}

