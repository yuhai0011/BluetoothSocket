
package com.futureagent.bluetoothsocket;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ConnectedThread extends AsyncTask<Void, Void, Void> {

    private static final int BUFFER_SIZE = 1024;

    private ResultHandler resultHandler;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Activity activity;
    private byte[] buffer;
    private boolean repeat;

    public ConnectedThread(ResultHandler resultHandler, Activity activity) {
        this.resultHandler = resultHandler;
        this.activity = activity;
        repeat = true;
        buffer = new byte[BUFFER_SIZE];
        try {
            inputStream = Globals.bluetoothSocket.getInputStream();
            outputStream = Globals.bluetoothSocket.getOutputStream();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            do {
                if (inputStream.read(buffer) == -1) {
                    throw new Exception("Error reading data.");
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultHandler.handleResult(buffer, activity);
                    }
                });
            } while (repeat);
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
        return null;
    }

    public void write(byte[] bytes) {
        // TODO
        // 缓冲区有点问题，上次传输的内容还会缓存在缓冲区，导致本次也能读出来
        try {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }

    public void cancel() {
        try {
            Globals.bluetoothSocket.close();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }
}
