package com.mcproject.rounak.bluemap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class BlueMapMainList extends AppCompatActivity {

    private ListView btList;
    private ArrayList<String> btNeighbors = new ArrayList<>();
    private BluetoothAdapter mBTAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btList = (ListView) findViewById(R.id.bt_list);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        mBTAdapter.startDiscovery();

        IntentFilter justFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myBTRecv, justFound);

    }

    protected void onDestroy() {
        unregisterReceiver(myBTRecv);
        super.onDestroy();
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

    private final BroadcastReceiver myBTRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice nghbr = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btNeighbors.add(nghbr.getName());
                Log.i("BT", nghbr.getName() + "\n" + nghbr.getAddress());
                btList.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_expandable_list_item_1, btNeighbors));
            }
        }
    };
}
