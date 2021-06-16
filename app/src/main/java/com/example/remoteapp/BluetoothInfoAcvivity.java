package com.example.remoteapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

public class BluetoothInfoAcvivity extends AppCompatActivity {

    // Переменные для извлечения информации о блютузе из главного активити
    public static final String EXTRA_BLUETOOTH_NAME = "name";
    public static final String EXTRA_BLUETOOTH_ADDRESS = "address";
    String bluetoothName;
    String bluetoothAddress;

    TextView textViewName;
    TextView textViewAddress;
    TextView textViewStatus;
    TextView textViewTempFirst;
    TextView textViewTempSecond;
    TextView textViewSpeedHeatingFirst;
    TextView textViewSpeedHeatingSecond;
    TextView textViewStatusRelay;
    TextView textViewMaxSpeedHeating;

    ToggleButton toggleButton;

    ConnectThread myConnect;

    CheckBox checkBox;

    boolean flag = false;                        // Флаг для

    // Переменные для сохранения настроек в SharedPreferences
    public static final String APP_PREFERENCES = "mysettings";
    public static final String APP_PREFERENCES_AUTO_ON_OFF = "";
    private SharedPreferences mSettings;

    // Переменная для хранения максимальной скорости нагрева
    double maxSpeedHeating = 0;

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
        textViewTempFirst = findViewById(R.id.textViewTempFirst);
        textViewTempSecond = findViewById(R.id.textViewTempSecond);
        textViewSpeedHeatingFirst = findViewById(R.id.textViewSpeedHeatingFirst);
        textViewSpeedHeatingSecond = findViewById(R.id.textViewSpeedHeatingSecond);
        textViewStatusRelay = findViewById(R.id.textViewStatusRelay);
        textViewMaxSpeedHeating = findViewById(R.id.textViewMaxSpeedHeating);


        checkBox = findViewById(R.id.checkBox);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        toggleButton = findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(APP_PREFERENCES_AUTO_ON_OFF, checkBox.isChecked());
        editor.apply();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSettings.contains(APP_PREFERENCES_AUTO_ON_OFF)) {
            checkBox.setChecked(mSettings.getBoolean(APP_PREFERENCES_AUTO_ON_OFF, true));
        }

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
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        InputStream inputStream;
        OutputStream outputStream;

        // Переменные для расчета скорости нагрева
        long timeFirstPoint;                        // Время первого замера температуры
        double tempFirstPointFirstSensor;           // Температура первого замера первого датчика
        double tempFirstPointSecondSensor;          // Температура первого замера второго датчика
        double timeDifference;                      // Разница времени между первым и вторым замером
        double tempDifferenceFirstSensor;           // Разница температуры между первым и вторым замером первого датчика
        double tempDifferenceSecondSensor;          // Разница температуры между первым и вторым замером второго датчика
        double speedOfHeatingFirstSensor;           // Скорость нагрева первого датчика
        double speedOfHeatingSecondSensor;          // Скорость нагрева второго датчика
        int counter = 0;                            //
        String stringSpeedHeatingFirstSensor;       // String cкорости нагрева первого датчика
        String stringSpeedHeatingSecondSensor;      // String cкорости нагрева второго датчика


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

            if (checkBox.isChecked()) {
                sendMsg("0");   // Включение чайника при запуске приложения
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toggleButton.setChecked(true);
                    }
                });
            }


            StringBuilder stringBuilder = new StringBuilder();

            inputStream = tempInputStream;
            outputStream = tempOutputStream;

            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            while (mmSocket.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);                                 // Запись входещего потока в буфер с присвоением bytes количества байтов
                    String stringIncom = new String(buffer, 0, bytes);           // Формирование строки на основе прочитанного массива байтов

                    stringBuilder.append(stringIncom);                                // Добавляем сформированную строку в StringBuilder

                    int endOfLineIndex1 = stringBuilder.indexOf("A");                 // Находим индекс первого параметра
                    int endOfLineIndex2 = stringBuilder.indexOf("B");                 // Находим индекс второго параметра
                    int endOfLineIndex3 = stringBuilder.indexOf("\r\n");              // Находим конец строки, также является индексом конца последнего параметра

                    // Если в строке что-то есть
                    if (endOfLineIndex3 > 0) {
                        String sbprint1 = stringBuilder.substring(0, endOfLineIndex1);                      // Формируем String первого параметра
                        String sbprint2 = stringBuilder.substring(endOfLineIndex1 + 1, endOfLineIndex2);    // Формируем String второго параметра
                        String sbprint3 = stringBuilder.substring(endOfLineIndex2 + 1, endOfLineIndex3);    // Формируем String третьего параметра
                        stringBuilder.delete(0, stringBuilder.length());                                    // Очищаем StringBuilder

                        //Подсчет скорости нагрева (пока для обоих датчиков).
                        if (counter == 0) {
                            timeFirstPoint = Calendar.getInstance().getTimeInMillis();
                            tempFirstPointFirstSensor = Double.parseDouble(sbprint1);
                            tempFirstPointSecondSensor = Double.parseDouble(sbprint2);
                            counter++;
                        } else if (counter == 1) {
                            timeDifference = ((double) (Calendar.getInstance().getTimeInMillis() - timeFirstPoint)) / 1000; // Разница времени для подсчета скорости нагрева

                            tempDifferenceFirstSensor = Double.parseDouble(sbprint1) - tempFirstPointFirstSensor;           // Разница температуры первого датчика
                            tempDifferenceSecondSensor = Double.parseDouble(sbprint2) - tempFirstPointSecondSensor;         // Разница температуры второго датчика

                            speedOfHeatingFirstSensor = tempDifferenceFirstSensor / timeDifference;                         // Скорость нагрева первого датчика
                            speedOfHeatingSecondSensor = tempDifferenceSecondSensor / timeDifference;                       // Скорость нагрева второго датчика

                            stringSpeedHeatingFirstSensor = String.format("%.2f", speedOfHeatingFirstSensor);               // String скорости нагрева первого датчика
                            stringSpeedHeatingSecondSensor = String.format("%.2f", speedOfHeatingSecondSensor);             // String скорости нагрева второго датчика

                            timeFirstPoint = Calendar.getInstance().getTimeInMillis();                                      // Текущее время для следующего расчета
                            tempFirstPointFirstSensor = Double.parseDouble(sbprint1);                                       // Температура первого датчика для следующего замера
                            tempFirstPointSecondSensor = Double.parseDouble(sbprint2);                                      // Температура второго датчика для следующего замера

                            if (speedOfHeatingFirstSensor > maxSpeedHeating) maxSpeedHeating = speedOfHeatingFirstSensor;
                        }

                        Log.i("rah", "Первая температура: " + sbprint1 + " Вторая температура: " + sbprint2 + " Состояния: " + sbprint3);

                        // Вывод информации в TextView
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                textViewTempFirst.setText("Температура (датчик 1): " + sbprint1);
                                textViewTempSecond.setText("Температура (датчик 2): " + sbprint2);
                                textViewSpeedHeatingFirst.setText("Скорость нагрева (датчик 1): " + stringSpeedHeatingFirstSensor + " град/сек");
                                textViewSpeedHeatingSecond.setText("Скорость нагрева (датчик 2): " + stringSpeedHeatingSecondSensor + " град/сек");
                                if (Integer.parseInt(sbprint3) == 1) {
                                    textViewStatusRelay.setText("Состояние реле: выключено");
                                } else {
                                    textViewStatusRelay.setText("Состояние реле: включено");
                                }
                                textViewMaxSpeedHeating.setText("Макс. скорость нагрева: " + maxSpeedHeating);

                                // Отправка уведомления при закипании чайника
                                if (Integer.parseInt(sbprint3) == 1 & Double.parseDouble(sbprint1) > 98.0 & flag) {
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
            flag = true;
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                inputStream.close();
                outputStream.close();
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!(myConnect == null)) {
            try {
                myConnect.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}