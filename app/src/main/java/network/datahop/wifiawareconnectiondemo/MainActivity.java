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

import datahop.WifiConnectionNotifier;
import datahop.WifiHotspotNotifier;

import network.datahop.wifiawareconnection.WifiAwareClient;
import network.datahop.wifiawareconnection.WifiAwareServer;

public class MainActivity extends AppCompatActivity implements WifiHotspotNotifier, WifiConnectionNotifier {


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
                hotspot.start(peerIdServerString,port,statusClientString.getBytes(StandardCharsets.UTF_8));
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
                connection.start(peerIdClientString,port,statusClientString.getBytes(StandardCharsets.UTF_8));
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
    public void clientsConnected(long l) {
        Log.d(TAG,"Clients connected "+l);
        if(counter>0&&l==0&&!stopping){
            Log.d(TAG,"Discovery restart");
            stopping=true;
            //discoveryDriver.start(TAG,id,2000,30000);
            hotspot.stop();
        }
        counter= (int) l;
        stopping=false;
    }

    @Override
    public void networkInfo(String net, String pass) {
        //ssidView.setText("SSID: "+net);
        //passView.setText("Pass: "+pass);

        Log.d(TAG,"Network info "+net+" "+pass);
    }

    @Override
    public void onFailure(long l) {
        Log.d(TAG,"onFailure");

    }

    @Override
    public void onSuccess() {
        Log.d(TAG,"onSuccess");

    }

    @Override
    public void onDisconnect() {
        Log.d(TAG,"onDisconnect");

    }

    @Override
    public void onConnectionFailure(long code, long started, long failed) {
        Log.d(TAG,"onFailure "+code);

    }

    @Override
    public void onConnectionSuccess(long started, long completed, long rssi , long speed ,long freq) {
        Log.d(TAG,"onSuccess");

    }

    @Override
    public void stopOnFailure(long l) {
        Log.d(TAG,"stopOnFailure "+l);
    }

    @Override
    public void stopOnSuccess() {
        Log.d(TAG,"stopOnSuccess");

    }




}