package com.mcproject.rounak.mcapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainList extends AppCompatActivity {

    private ListView btList;
    private BluetoothAdapter myBTAdapter;
    private final BroadcastReceiver myBTRecv;
    private Timer refresh;

    /* ListView is loaded from this container */
    private HashMap<String, String> btDistClass = new HashMap<>();

    /* details are kept here; might contain currently out-of-range devices as well*/
    private HashMap<String, Short> btRSSI = new HashMap<>();
    private HashMap<String, String> btAddress = new HashMap<>();
    private HashMap<String, Double> btDistance = new HashMap<>();

    /* Keeps track of running average of RSSI (even for those currently out of range)*/
    private HashMap<String, Double> btLPF = new HashMap<>();

    /* Calibration constants (Set from menu)*/
    protected String curMode;
    protected Double txPower;

    /* Values for the propagation constant */
    public final Double n_avg = 3.0;
    public final Double n_indoor = 2.0;
    public final Double n_outdoor = 4.0;

    /* Mode definitons */
    public final String AVERAGE = "Average";
    public final String INDOOR = "Indoor";
    public final String OUTDOOR = "Outdoor";

    /* misc. constants */
    public final int REQUEST_ENABLE_BT = 69;
    /* Refreshing interval in milliseconds, must be more than 12 */
    public final int REFRESH_INTERVAL = 20000;
    /* Granularity of the distance scale */
    public final double SCALE_GRAN = 5.0;
    /* Weight of averaging RSSI */
    public final double ALPHA = 0.70;

    /* Distance class definitions, defined by upper limit / 5.0 */
    public final double IMMEDIATE_TOP = 2.0;
    public final double NEAR_TOP = 4.0;
    public final double FAR_TOP = 6.0;

    /* Distance class display strings */
    public final String IMMEDIATE = "Very close";
    public final String NEAR = "Nearby";
    public final String FAR = "Far away";
    public final String NOPE = "Not close";

    public MainList() {
        /* Set up calibration constants to default */
        curMode = INDOOR;
        txPower = -50.0;

        /* Register discovery broadcast receiver for bluetooth discovery frames */
        myBTRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    /* Get device name and rssi value */
                    BluetoothDevice nghbr = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    Log.d("BTLIST", "Discovered: " + nghbr.getName() + ":\t" + rssi);
                    /* Add the address and RSSI*/
                    btRSSI.put(nghbr.getName(), rssi);
                    btAddress.put(nghbr.getName(), nghbr.getAddress());
                    /* Approximate current distance */
                    updateDistance(nghbr.getName(), rssi);
                    /* To show list */
                    refreshList(context);
                }
            }
        };
    }

    protected void updatePreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("BTPREF", "Reading shared prefs");

        Double txPref = 50.0;
        String gotVal = pref.getString("pref_key_tx_power", null);
        if (gotVal != null) {
            txPref = Double.parseDouble(gotVal);
        }
        Log.d("BTPREF", "Read txPower as: " + txPref);
        this.txPower = txPref * -1.0;

        int modePref = 1;
        gotVal = pref.getString("pref_key_mode_choice", null);
        if (gotVal != null) {
            modePref = Integer.parseInt(gotVal);
        }
        Log.d("BTPREF", "Read location mode as: " + modePref);
        switch (modePref){
            case 1:
                this.curMode = INDOOR;
                break;
            case 2:
                this.curMode = OUTDOOR;
                break;
            default:
                this.curMode = AVERAGE;
        }
    }

    /**
     * Update the list of devices
     * @param context Passed from upper call
     */
    protected void refreshList(Context context) {
        /* TODO: Make more efficient by adding instead of redrawing */
        List<String> vals = new ArrayList<>();
        for (Map.Entry<String, String> entry : btDistClass.entrySet()) {
            String toShow = entry.getKey() + ":::" + entry.getValue();
            vals.add(toShow);
        }
        btList.setAdapter(new BTArrayAdapter(context, vals));
    }

    /**
     * This formula is used to approximate distance based on the current smoothed RSSI value
     * @param rssi Current RSSI value
     * @return Double The approximated distance
     */
    protected Double formula (Double rssi) {
        Double upside = rssi + txPower;
        Double downside;
        switch(curMode) {
            case INDOOR:
                downside = -10 * n_indoor;
                break;
            case OUTDOOR:
                downside = -10 * n_outdoor;
                break;
            default:
                downside = -10 * n_avg;
        }
        Double exp = upside / downside;
        return Math.pow(10.0, exp);
    }

    /**
     * Calculate the distance class from the distance
     * @param distance current approximation of distance
     * @return Name of distance class
     */
    protected String getDistanceClass(Double distance) {
        double dist = distance / SCALE_GRAN;
        String retval;
        if (dist < IMMEDIATE_TOP) {
            retval =  IMMEDIATE;
        } else if (dist < NEAR_TOP) {
            retval = NEAR;
        } else if (dist < FAR_TOP) {
            retval = FAR;
        } else {
            retval = NOPE;
        }
        return retval;
    }

    /**
     * Update the distance to a node
     * @param name Name as reported by the node
     * @param rssi Current RSSI reading
     */
    protected void updateDistance(String name, short rssi) {
        /* Smooth using LPF */
        if (!btLPF.containsKey(name)) {
            btLPF.put(name, rssi/1.0);
        }
        Double curLPF = btLPF.get(name);
        Double nextLPF = ALPHA*curLPF + (1 - ALPHA) * rssi;
        btLPF.put(name, nextLPF);
        /* Estimate distance */
        Double distance = formula(nextLPF);
        distance /= Math.pow(10, 6);       /* adjust units */
        Log.d("BTLIST", "Approximated distance to " + name + " is " + distance);
        btDistance.put(name, distance);
        /* Return the distance class description */
        String distClass = getDistanceClass(distance);
        Log.d("BTLIST", name + " classified as: " + distClass);
        btDistClass.put(name, distClass);
    }

    /**
     * Start a Bluetooth discovery phase
     */
    private void doDiscovery() {
        Log.d("BTLIST", "Gonna start discovery");
        myBTAdapter.startDiscovery();
    }

    /**
     * Show error dialogue and exit
     */
    private void showError() {
        AlertDialog errDiag = new AlertDialog.Builder(MainList.this).create();
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

    /* All overridden function are below */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btList = (ListView) findViewById(R.id.bt_list);
        btList.setLongClickable(true);

        btList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos, long id) {
                /* Get name of clicked item */
                String showing = (String) adapter.getAdapter().getItem(pos);
                String name = showing.split(":::")[0];
                /* Show more info in alert dialog */
                final AlertDialog infoDiag = new AlertDialog.Builder(MainList.this).create();
                infoDiag.setTitle(name + "\'s details");    /* Set title */
                String message = "Address:  " + btAddress.get(name);    /* Set address */
                /* Build message to show */
                message = message + "\nRSSI:  " + btRSSI.get(name) + " dBm";
                Double d = btDistance.get(name);
                DecimalFormat df = new DecimalFormat("##.####");
                df.setRoundingMode(RoundingMode.CEILING);
                message = message + "\nAppr.Distance:  " + df.format(d) + " m";
                infoDiag.setMessage(message);
                infoDiag.setButton(AlertDialog.BUTTON_NEUTRAL, "Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                Log.d("BTLIST", "Long press: pos=" + pos + " name=" + name);
                infoDiag.show();
                /* Do not propagate */
                return true;
            }
        });

        myBTAdapter = BluetoothAdapter.getDefaultAdapter();

        if (myBTAdapter == null) {
            Log.e("BTLIST", "No BT adpater installed");
            showError();
            return;
        }

        Log.i("BTLIST", "Starting in mode: " + curMode);
        Log.i("BTLIST", "Calibrated to txPower: " + txPower + " dBm");

        if (!myBTAdapter.isEnabled()) {
            Log.d("BTLIST", "BT adapter is not enabled");
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        } else {
            doDiscovery();
        }

        IntentFilter justFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myBTRecv, justFound);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(myBTRecv);
        super.onDestroy();
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
    protected void onResume() {
        super.onResume();
        refresh = new Timer();
        refresh.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updatePreferences();
                        /* Clear the list, so that only currently discoverable devices are showed */
                        btDistClass = new HashMap<>();
                        btList.setAdapter(null);
                        doDiscovery();
                    }
                });
            }
        }, 0, REFRESH_INTERVAL);       /* refresh every interval seconds */
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
            Log.d("BTLIST", "Pressed settings");
            Intent intent = new Intent();
            intent.setClassName(this, "com.mcproject.rounak.mcapp.SettingsActivity");
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
