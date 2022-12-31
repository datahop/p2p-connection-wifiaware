package network.datahop.wifiawareconnectiondemo;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {


    public static void clientSendFile(final String serverIP, final int serverPort) {
        Runnable clientTask = new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[4096];
                int bytesRead;
                Socket clientSocket = null;
                int fsize = 1;
                InputStream is = null;
                OutputStream outs = null;
                Log.d("clientThread", "thread running socket info "+ serverIP + "\t" + serverPort);
                try {
                    //clientSocket = new Socket(Inet6Address.getByAddress(serverIP),serverPort);
                    clientSocket = new Socket( serverIP , serverPort );
                    is = clientSocket.getInputStream();
                    outs = clientSocket.getOutputStream();
                    Log.d("clientThread", "socket created ");
                } catch (IOException ex) {
                    Log.d("clientThread", "socket could not be created " + ex.toString());
                }
                try {
                    String message = "hola";
                    InputStream in = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
                    int count;
                    int totalSent = 0;
                    DataOutputStream dos = new DataOutputStream(outs);
                    Log.d("clientThread", "beginning to send file (log updates every 2MB)");
                    while ((count = in.read(buffer))>0){
                        totalSent += count;
                        dos.write(buffer, 0, count);
                    }
                    in.close();
                    dos.close();
                    Log.d("clientThread", "finished sending file!!! "+totalSent);
                    //setStatus("Finished sending file");

                } catch(IOException e){
                    Log.d("clientThread", e.toString());
                }

            }
        };
        Thread clientThread = new Thread(clientTask);
        clientThread.start();

    }
}
