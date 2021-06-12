package com.example.remoteapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ListView listView;
    ArrayList <BluetoothDevice> bluetoothDeviceArrayList;
    ArrayList <String> bluetoothDeviceNameArrayList;
    BluetoothAdapter bluetoothAdapter;
    IntentFilter filter;
    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        init();

        startDiscovery();
    }

    private void init() {

        listView = findViewById(R.id.listView);

        bluetoothDeviceArrayList = new ArrayList<>();

        bluetoothDeviceNameArrayList = new ArrayList<>();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bluetoothDeviceNameArrayList);
        listView.setAdapter(arrayAdapter);

    }

    private void checkAndRequestPermissions() {
        if((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.BLUETOOTH}, 1);
        }

        if((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN) ==
                PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.BLUETOOTH_ADMIN}, 1);
        }

        if((ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        if((ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null){
                    if (!bluetoothDeviceArrayList.contains(device)){
                        bluetoothDeviceArrayList.add(device);
                        if (device.getName() != null){
                            bluetoothDeviceNameArrayList.add(device.getName());
                        } else bluetoothDeviceNameArrayList.add("unknown device");
                    }

                    if (device.getName() != null) {
                        if (device.getName().equals("HC-05")) {
                            Intent intent1 = new Intent(MainActivity.this, BluetoothInfoAcvivity.class);
                            intent1.putExtra(BluetoothInfoAcvivity.EXTRA_BLUETOOTH_NAME, device.getName());
                            intent1.putExtra(BluetoothInfoAcvivity.EXTRA_BLUETOOTH_ADDRESS, device.getAddress());
                            startActivity(intent1);
                        }
                    }
                }
                arrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public void startDiscoveryButton(View view) {
       startDiscovery();
    }

    private void startDiscovery() {
        bluetoothDeviceArrayList.clear();
        bluetoothDeviceNameArrayList.clear();
        if (!bluetoothAdapter.isEnabled()) { //Если блютуз выключен, сделать запрос на включение
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        if (bluetoothAdapter.isEnabled()){ //Если блютуз включен, начать поиск устройств
            bluetoothAdapter.startDiscovery();
            arrayAdapter.notifyDataSetChanged();

        }
    }
}