package com.bluetoothchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public BluetoothServerSocket mmServerSocket;
    public BluetoothSocket mmSocket;

    public static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public static final String NAME = "CHAT_BLUETOOTH";

    Context context;

    public static final int REQUEST_ACCESS_LOCATION = 1;

    ListView listViewPaired;
    ListView listViewAvailable;
    BluetoothAdapter bluetoothAdapter;

    ArrayList<String> pairedDevicesFound;
    ArrayList<BluetoothDevice> pairedDevices;
    ArrayList<String> pairedDevicesFoundAddresses;
    ArrayAdapter arrayPairedAdapter;

    ArrayList<String> availableDevicesFound;
    ArrayList<BluetoothDevice> availableDevices;
    ArrayList<String> availableDevicesFoundAddresses;
    ArrayAdapter arrayAvailableAdapter;





    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(getApplicationContext(), "Action: " + action, Toast.LENGTH_SHORT).show();

            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

            }else if(BluetoothDevice.ACTION_FOUND.equals(action)){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));
                //Toast.makeText(getApplicationContext(), "Device found: " + name + " - Address : " + address + "RSSI : "+ rssi, Toast.LENGTH_SHORT).show();


                String deviceString;
                if(name != null && !name.equals("")) {
                    deviceString = "Name : " + name + " - Address : " + address + " - RSSI : " + rssi;
                } else {
                    deviceString = "Address : " + address + " - RSSI : " + rssi;
                }
                if(!availableDevicesFoundAddresses.contains(device.getAddress())){
                    availableDevicesFound.add(deviceString);
                    availableDevicesFoundAddresses.add(device.getAddress());
                    availableDevices.add(device);
                }

                arrayAvailableAdapter.notifyDataSetChanged();

            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                availableDevicesFound.clear();
                availableDevicesFoundAddresses.clear();
                availableDevices.clear();
                arrayAvailableAdapter.notifyDataSetChanged();
            }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){

            }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), "Bonded", Toast.LENGTH_SHORT).show();
                    searchClicked();

                }else if(device.getBondState() == BluetoothDevice.BOND_BONDING){

                    Toast.makeText(getApplicationContext(), "BONDING", Toast.LENGTH_SHORT).show();
                    searchClicked();

                }else if(device.getBondState() == BluetoothDevice.BOND_NONE){
                    Toast.makeText(getApplicationContext(), "BOND NONE", Toast.LENGTH_SHORT).show();
                    searchClicked();
                }
            }
        }
    };







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        listViewPaired = (ListView) findViewById(R.id.list_view_paired);
        listViewAvailable = (ListView) findViewById(R.id.list_view_available);


        availableDevicesFound = new ArrayList<String>();
        availableDevicesFoundAddresses = new ArrayList<String>();
        availableDevices = new ArrayList<BluetoothDevice>();





        checkCoarseLocationPermission();
        checkFineLocationPermission();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(broadcastReceiver, intentFilter);

        arrayAvailableAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, availableDevicesFound);
        listViewAvailable.setAdapter(arrayAvailableAdapter);

        listViewAvailable.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(context, "Mac Address = " + devicesFoundAdresses.get(position), Toast.LENGTH_LONG).show();

                bluetoothAdapter.cancelDiscovery();
                availableDevices.get(position).createBond();
            }
        });



        pairedDevicesFound = new ArrayList<String>();
        pairedDevicesFoundAddresses = new ArrayList<String>();

        arrayPairedAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDevicesFound);
        listViewPaired.setAdapter(arrayPairedAdapter);

        listViewPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(context, "Mac Address = " + devicesFoundAdresses.get(position), Toast.LENGTH_LONG).show();
                connectDevice(pairedDevices.get(position));

            }
        });

        //bluetoothEnable();

        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("You need to turn your location on to use the app")
                .setIcon(R.drawable.ic_location_on)
                .show();
    }

    public void searchClicked(){
        //Toast.makeText(this, "Searching...", Toast.LENGTH_LONG).show();
        bluetoothAdapter.startDiscovery();

        pairedDevicesFound.clear();
        pairedDevicesFoundAddresses.clear();
        pairedDevices.clear();
        for(BluetoothDevice bluetoothDevice: bluetoothAdapter.getBondedDevices()){
            pairedDevicesFound.add(bluetoothDevice.getName()+ " - " + bluetoothDevice.getAddress());
            pairedDevicesFoundAddresses.add(bluetoothDevice.getAddress());
            pairedDevices.add(bluetoothDevice);
        }

        arrayPairedAdapter.notifyDataSetChanged();
    }

    public void bluetoothEnable(){
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }

        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoveryIntent);
        }

        startListening();

    }

    public void startListening(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothServerSocket tmp = null;
                try {
                    // MY_UUID is the app's UUID string, also used by the client code.
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID_INSECURE);
                } catch (IOException e) {
                    Log.e(TAG, "Socket's listen() method failed", e);
                }
                mmServerSocket = tmp;

                BluetoothSocket socket = null;
                // Keep listening until exception occurs or a socket is returned.
                while (true) {
                    try {
                        socket = mmServerSocket.accept();
                    } catch (IOException e) {
                        Log.e(NAME, "Socket's accept() method failed", e);
                        break;
                    }

                    if (socket != null) {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread
                        Intent i = new Intent(context, ChatActivity.class);
                        i.putExtra("socket", String.valueOf(socket));
                        startActivity(i);
                        try {
                            mmServerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }).start();
    }

    public void connectDevice(BluetoothDevice device){
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothSocket tmp = null;

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mmSocket = tmp;

                // Make a connection to the BluetoothSocket

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();

                    Intent i = new Intent(context, ChatActivity.class);
                    i.putExtra("socket", String.valueOf(mmSocket));
                    startActivity(i);

                } catch (IOException e) {
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e1) {
                    }
                }

            }
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(broadcastReceiver);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_bluetooth_on:
                bluetoothEnable();
                //createLocationRequest();
                //Toast.makeText(this, "Bluetooth on clicked", Toast.LENGTH_LONG).show();
                return true;
            case R.id.option_search_devices:
                searchClicked();
                //Toast.makeText(this, "Search devices clicked", Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean checkCoarseLocationPermission() {
        //checks all needed permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_LOCATION);
            return false;
        }else{
            return true;
        }

    }

    private boolean checkFineLocationPermission() {
        //checks all needed permissions
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_LOCATION);
            return false;
        }else{
            return true;
        }

    }

//    protected void createLocationRequest() {
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setInterval(10000);
//        locationRequest.setFastestInterval(5000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        switch (requestCode){
            case REQUEST_ACCESS_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(),"Permission granted",Toast.LENGTH_SHORT).show();
                }else{
                    new AlertDialog.Builder(context)
                            .setCancelable(false)
                            .setMessage("Location permission is required!!")
                            .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkCoarseLocationPermission();
                                    checkFineLocationPermission();
                                }
                            }).setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.finish();
                                }
                            }).create();
                }
        }
    }
}