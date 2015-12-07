
package com.futureagent.bluetoothsocket;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class ClientThread extends Thread {
    private static String TAG = "ClientThread";
    private BluetoothSocket bluetoothSocket;
    private MainActivity mainActivity;

    public ClientThread(MainActivity activity) {
        BluetoothDevice bluetoothDevice = activity.getBluetoothDevice();
        this.mainActivity = activity;
        try {
            bluetoothSocket = bluetoothDevice
                    .createRfcommSocketToServiceRecord(MainActivity.AGENT_UUID);
        } catch (Exception e) {
            LogHelper.e(TAG, "Error while creating socket.", e);
        }
    }

    @Override
    public void run() {
        mainActivity.getBluetoothAdapter().cancelDiscovery();
        try {
            LogHelper.d(TAG, "client before connect");
            bluetoothSocket.connect();
            LogHelper.d(TAG, "client after connect");
            mainActivity.manageConnectedSocket(bluetoothSocket);
        } catch (Exception e) {
            LogHelper.e(TAG, "Trouble with connecting to socket.", e);
            try {
                bluetoothSocket.close();
            } catch (Exception ignore) {
            }
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (Exception ignore) {
        }
    }
}
