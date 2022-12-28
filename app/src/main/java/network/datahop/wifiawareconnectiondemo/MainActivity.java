package network.datahop.wifiawareconnectiondemo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;


import java.nio.charset.StandardCharsets;

import datahop.WifiAwareServerNotifier;
import datahop.WifiAwareClientNotifier;

import network.datahop.wifiawareconnection.WifiAwareClient;
import network.datahop.wifiawareconnection.WifiAwareServer;

public class MainActivity extends AppCompatActivity implements WifiAwareServerNotifier, WifiAwareClientNotifier {


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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        peerIdServer.setText("PeerID: "+peerIdServerString);
        peerIdClient.setText("PeerID: "+peerIdClientString);
        statusClient.setText("Status: "+statusClientString);
        statusServer.setText("StatusL "+statusServerString);

        peerIdClientString = "peerId2";
        peerIdServerString = "peerId1";

        statusServerString = "status1";
        statusClientString = "status2";

        startHSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Starting HS");
                hotspot.start(peerIdServerString,port);
            }
        });

        stopHSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Stopping HS");
                hotspot.stop();
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //connection.disconnect();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Connecting ");
                //connection.connectV2(ssid.getText().toString(),password.getText().toString(),"","");
                connection.connect(peerIdClientString);
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
    public void startOnSuccess() {
    }

    @Override
    public void startOnFailure(long code) {
    }

    @Override
    public void stopOnSuccess() {
    }

    @Override
    public void stopOnFailure(long code) {
    }

    @Override
    public void networkReady(String ip, long port) {
    }

    @Override
    public void onConnectionFailure(long code, long started, long failed) {
    }

    @Override
    public void onConnectionSuccess(String ip, long port, String peerId) {
    }

    @Override
    public void onDisconnect() {
    }

}

