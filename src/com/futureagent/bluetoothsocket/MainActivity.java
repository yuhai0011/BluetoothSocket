
package com.futureagent.bluetoothsocket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements ResultHandler {
    public final static String NAME = "future-agent";
    public final static UUID AGENT_UUID = UUID.nameUUIDFromBytes(NAME.getBytes());

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private ClientThread mClientThread;
    private ServerThread mServerThread;
    private List<BluetoothDevice> foundDevices;
    private ArrayAdapter<String> BTArrayAdapter;

    private final static int BLUETOOTH_INTENT_ENABLE = 1;
    private final static int BLUETOOTH_INTENT_DISCOVERABLE = 3;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                try {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    foundDevices.add(device);
                    BTArrayAdapter.add(device.getName());
                    BTArrayAdapter.notifyDataSetChanged();
                } catch (Exception ignore) {
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_list);
        initBluetoothAdapter();
        initView();
    }

    private void initView() {
        Button btn_send = (Button) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mConnectedThread == null) {
                    Toast.makeText(getApplicationContext(), "ClientThread is null",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                EditText text = (EditText) findViewById(R.id.msg_send);
                mConnectedThread.write(text.getText().toString().getBytes());
                Toast.makeText(getApplicationContext(), "send:" + text.getText().toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        ListView listView = (ListView) findViewById(R.id.BluetoothDeviceList);
        listView.setAdapter(BTArrayAdapter);
        final MainActivity mainActivity = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothDevice = getBluetoothDeviceByName(
                        (String) parent.getItemAtPosition(position));
                if (mClientThread != null) {
                    mClientThread.cancel();
                }
                mClientThread = null;
                mClientThread = new ClientThread(mainActivity);
                mClientThread.start();
                Toast.makeText(getApplicationContext(), "ClientThread create", Toast.LENGTH_SHORT)
                        .show();
            }
        });
        findViewById(R.id.searchDevices).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                findBluetoothDevices();
            }
        });
        findViewById(R.id.serverCreate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
                        BLUETOOTH_INTENT_DISCOVERABLE);
            }
        });
        turnOnBluetooth();
    }

    private void initBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new RuntimeException("bluetoothAdapter is null");
        }
        BTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        foundDevices = new ArrayList<>();
    }

    public BluetoothDevice getBluetoothDeviceByName(String name) {
        for (BluetoothDevice device : foundDevices) {
            if (name.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    public void findBluetoothDevices() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            ((Button) findViewById(R.id.searchDevices)).setText(getString(R.string.search_start));
        } else {
            ((Button) findViewById(R.id.searchDevices)).setText(getString(R.string.search_stop));
            BTArrayAdapter.clear();
            bluetoothAdapter.startDiscovery();
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void manageConnectedSocket(BluetoothSocket bluetoothSocket) {
        if (bluetoothSocket != null) {
            Globals.bluetoothSocket = bluetoothSocket;
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startListenThread();
                }
            });
        }
    }

    private ConnectedThread mConnectedThread;

    private void startListenThread() {
        final MainActivity mainsActivity = this;
        mConnectedThread = new ConnectedThread(mainsActivity, mainsActivity);
        mConnectedThread.execute();
        Toast.makeText(getApplicationContext(), "ConnectedThread create", Toast.LENGTH_SHORT)
                .show();
    }

    public void turnOnBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    @Override
    public void handleResult(byte[] parsedData, Activity activity) {
        EditText text = (EditText) findViewById(R.id.msg_rev);
        String val = new String(parsedData);
        text.setText("");
        text.setText(val);
        Toast.makeText(getApplicationContext(), "recv:" + val, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLUETOOTH_INTENT_DISCOVERABLE:
                if (mServerThread != null) {
                    mServerThread.cancel();
                }
                mServerThread = null;
                mServerThread = new ServerThread(this);
                mServerThread.start();
                Toast.makeText(getApplicationContext(), "ServerThread create", Toast.LENGTH_SHORT)
                        .show();
                return;
            case BLUETOOTH_INTENT_ENABLE:
                return;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignore) {
            /* receiver not registered, no big deal, ur server it's normal */ }
        invalidateBluetooth();
    }

    private void invalidateBluetooth() {
        if (mServerThread != null) {
            mServerThread.cancel();
        }
        if (mClientThread != null) {
            mClientThread.cancel();
        }
        if (bluetoothAdapter != null) {
            bluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Bluetooth close", Toast.LENGTH_SHORT).show();
        }
    }
}
