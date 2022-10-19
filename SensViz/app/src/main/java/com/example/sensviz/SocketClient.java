package com.example.sensviz;

import android.os.AsyncTask;
import static java.nio.charset.StandardCharsets.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class SocketClient extends AsyncTask<Void, Void, Void> {

    String data = "";
    String ip = "192.168.43.77";
    SocketClient(String t){
        data = t;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        if (data != "escape")
        {
            try {
                Socket con = new Socket("192.168.43.77", 9999);
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                byte arr[] = "escape".getBytes(ISO_8859_1);
                out.write(arr);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return null;
    }
}