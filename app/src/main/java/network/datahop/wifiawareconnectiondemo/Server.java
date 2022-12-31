package network.datahop.wifiawareconnectiondemo;

import android.annotation.TargetApi;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {


    @TargetApi(26)
    public static void startServer(final int port, final int backlog) {
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d("serverThread", "thread running");
                    //Thread.sleep(1000);
                    ServerSocket serverSocket = new ServerSocket(port, backlog);
                    //ServerSocket serverSocket = new ServerSocket();
                    while (true) {


                        Log.d("serverThread", "server waiting to accept on " + serverSocket.toString());
                        Socket clientSocket = serverSocket.accept();
                        DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

                        int count = in.available();

                        byte[] bs = new byte[count];

                        in.read(bs);

                        Log.d("serverThread", "finished file transfer: " + new String(bs));


                    }
                } catch (IOException e) {
                    Log.d("serverThread", "socket exception " + e.toString());
                    Log.d("serverThread",  e.getStackTrace().toString());
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();

    }

}
