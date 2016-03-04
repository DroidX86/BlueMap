package com.mcproject.rounak.bluemap;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BlueMapMainList extends AppCompatActivity {

    private ListView btList;
    private HashMap<String, Short> btNeighbors = new HashMap<>();
    //private ArrayList<String> found = new ArrayList<>();
    private BluetoothAdapter mBTAdapter;
    //private final BroadcastReceiver myBTRecv;
    private Timer refresh;

    public final int REQUEST_ENABLE_BT = 1;

    //public BlueMapMainList() {
    private final BroadcastReceiver myBTRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice nghbr = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //Log.d("BTLIST", "Found: " + nghbr.getName());
                    //found.add(nghbr.getName());
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    btNeighbors.put(nghbr.getName(), rssi);
                    Log.d("BTLIST", nghbr.getName() + ":\t" + rssi);
                    List<String> vals = new ArrayList<>();

                    for (Map.Entry<String, Short> entry : btNeighbors.entrySet()) {
                        String toShow = entry.getKey() + "\n" + entry.getValue();
                        vals.add(toShow);
                        Log.d("BTLIST", "Adding: " + toShow);
                    }

                    btList.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_expandable_list_item_1, vals));
                }
            }
        };
    //}

    private void doDiscovery() {
        Log.d("BTLIST", "Gonna start discovery");
        mBTAdapter.startDiscovery();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btList = (ListView) findViewById(R.id.bt_list);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBTAdapter == null) {
            Log.d("BTLIST", "No BT adpater installed");
            showError();
            return;
        }

        if (!mBTAdapter.isEnabled()) {
            Log.d("BTLIST", "BT adapter is not enabled");
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        } else {
            doDiscovery();
        }

        IntentFilter justFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myBTRecv, justFound);
    }

    private void showError() {
        AlertDialog errDiag = new AlertDialog.Builder(BlueMapMainList.this).create();
        errDiag.setTitle("Error");
        errDiag.setMessage("Could not load bluetooth neighbors");
        errDiag.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finishAndRemoveTask();
                    }
                });
        errDiag.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                doDiscovery();
            } else {
                showError();
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(myBTRecv);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh = new Timer();
        refresh.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btNeighbors = new HashMap<>();
                        doDiscovery();
                    }
                });
            }
        }, 0, 30000);       /* refresh every 30 seconds */
    }

    @Override
    protected void onPause() {
        refresh.cancel();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_blue_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
