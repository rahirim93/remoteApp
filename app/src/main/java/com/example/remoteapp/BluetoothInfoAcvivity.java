package com.example.remoteapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.chrono.ThaiBuddhistEra;
import java.util.Calendar;
import java.util.UUID;

import static android.widget.Toast.LENGTH_SHORT;

public class BluetoothInfoAcvivity extends AppCompatActivity {

    // Переменные для извлечения информации о блютузе из главного активити
    public static final String EXTRA_BLUETOOTH_NAME = "name";
    public static final String EXTRA_BLUETOOTH_ADDRESS = "address";
    String bluetoothName;
    String bluetoothAddress;

    TextView textViewName;
    TextView textViewAddress;
    TextView textViewStatus;
    TextView textViewMessage;
    TextView textViewSpeedHeating;

    ToggleButton toggleButton;

    ConnectThread myConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_info_acvivity);

        init();

        getInfoBluetoothDevice();

        outputNameAndAddress();

        connect();
    }

    private void init() {

        textViewName = findViewById(R.id.textViewName);
        textViewAddress = findViewById(R.id.textViewAdress);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewMessage = findViewById(R.id.textViewMessage);
        textViewSpeedHeating = findViewById(R.id.textViewSpeedHeating);

        toggleButton = findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        myConnect.sendMsg("0");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        myConnect.sendMsg("1");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    void getInfoBluetoothDevice() {

        bluetoothName = (String) getIntent().getExtras().get(EXTRA_BLUETOOTH_NAME);
        bluetoothAddress = (String) getIntent().getExtras().get(EXTRA_BLUETOOTH_ADDRESS);

    }

    private void outputNameAndAddress() {

        textViewName.setText("Имя: " + bluetoothName);
        textViewAddress.setText("Адрес: " + bluetoothAddress);

    }

    public void connectButton(View view) {

        connect();

    }

    private void connect() {
        try {
            myConnect = new ConnectThread(bluetoothAddress);
            myConnect.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID =UUID.fromString ("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        InputStream inputStream;
        OutputStream outputStream;

        // Переменные для расчета скорости нагрева
        long timeFirstPoint;
        double tempFirstPoint;
        double timeDifference;
        double tempDifference;
        double speedOfHeating;
        int counter = 0;
        String stringSpeedHeating;
        boolean flag = true;

        public ConnectThread(String bluetoothAddress) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = bluetoothAdapter.getRemoteDevice(bluetoothAddress);

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewStatus.setText("Статус: Подключение...");
                    }
                });
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            //Показать тост если соединение успешно
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewStatus.setText("Статус: Подключен");
                    textViewStatus.setTextColor(Color.GREEN);
                }
            });


            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;

            try {

                tempInputStream = mmSocket.getInputStream();
                tempOutputStream = mmSocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

            StringBuilder stringBuilder = new StringBuilder();

            inputStream = tempInputStream;
            outputStream = tempOutputStream;

            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            while (true){
                try {
                    bytes = inputStream.read(buffer);
                    String stringIncom = new String(buffer, 0, bytes);
                    stringBuilder.append(stringIncom);
                    int endOfLineIndex = stringBuilder.indexOf("\r\n");
                    if (endOfLineIndex > 0) {
                        String sbprint = stringBuilder.substring(0, endOfLineIndex);
                        stringBuilder.delete(0, stringBuilder.length());

                        // Подсчет скорости нагрева
                        if (counter == 0) {
                            timeFirstPoint = Calendar.getInstance().getTimeInMillis();
                            tempFirstPoint = Double.parseDouble(sbprint);
                            counter++;
                        } else if (counter == 1) {
                            // Разница времени для подсчета скорости нагрева. Деление на 1000 для приведения к секундам
                            timeDifference = ((double) (Calendar.getInstance().getTimeInMillis() - timeFirstPoint)) / 1000;
                            tempDifference = Double.parseDouble(sbprint) - tempFirstPoint;
                            speedOfHeating = tempDifference / timeDifference;
                            stringSpeedHeating = String.format("%.2f", speedOfHeating);
                            timeFirstPoint = Calendar.getInstance().getTimeInMillis();
                            tempFirstPoint = Double.parseDouble(sbprint);
                            System.out.println(stringSpeedHeating + "\t" + timeDifference + "\t" + tempDifference);
                        }

                        // Вывод информации в TextView
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textViewMessage.setText("Температура: " + sbprint);
                                textViewSpeedHeating.setText("Скорость нагрева: " + stringSpeedHeating + " град/сек");

                                // Вывод уведомления при достижении температуры отключения
                                if (Double.parseDouble(sbprint) > 70 & flag) {
                                    flag = false;
                                    CharSequence name = "Simple Notification";
                                    String description = "Include all the simple notification";
                                    int importance = NotificationManager.IMPORTANCE_DEFAULT;

                                    NotificationChannel notificationChannel = new NotificationChannel("CHANNEL_ID", name, importance);
                                    notificationChannel.setDescription(description);


                                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                    notificationManager.createNotificationChannel(notificationChannel);

                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(BluetoothInfoAcvivity.this, "CHANNEL_ID");
                                    builder.setContentText("Чайник вскипятился")
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setTicker("Ticker text")
                                            //.setDefaults(Notification.DEFAULT_VIBRATE)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(BluetoothInfoAcvivity.this);
                                    notificationManagerCompat.notify(1, builder.build());
                                }
                                if (Double.parseDouble(sbprint) < 70) {
                                    flag = true;
                                }
                            }
                        });


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMsg(String message) {
            try {
                outputStream = mmSocket.getOutputStream();
                byte[] messageBuffer = message.getBytes();
                outputStream.write(messageBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!(myConnect == null)){
            try {
                myConnect.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}