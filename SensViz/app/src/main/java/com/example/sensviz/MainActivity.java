package com.example.sensviz;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;

import android.widget.Toast;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    // properties
    private final static String LOG_TAG = MainActivity.class.getName();

    private final static int REQUEST_CODE_ANDROID = 1001;
    private static String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private IMUConfig mConfig = new IMUConfig();
    private IMUSession mIMUSession;

    private DatagramSocket con;
    private String ipData;
    private PrintWriter writer;
    private int SocketState;
    private BackgroundTask bl1;

    private Handler mHandler = new Handler();
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private PowerManager.WakeLock mWakeLock;

    private TextView mLabelAccelDataX, mLabelAccelDataY, mLabelAccelDataZ;
    private TextView mLabelAccelBiasX, mLabelAccelBiasY, mLabelAccelBiasZ;
    private TextView mLabelGyroDataX, mLabelGyroDataY, mLabelGyroDataZ;
    private TextView mLabelGyroBiasX, mLabelGyroBiasY, mLabelGyroBiasZ;
    private TextView mLabelMagnetDataX, mLabelMagnetDataY, mLabelMagnetDataZ;
    private TextView mLabelMagnetBiasX, mLabelMagnetBiasY, mLabelMagnetBiasZ;

    private Button mStartStopButton;
    private TextView mLabelInterfaceTime;


    // Android activity lifecycle states
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize screen labels and buttons
        initializeViews();
        SocketState = 0;


        // setup sessions
        mIMUSession = new IMUSession(this);


        // battery power setting
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors_data_logger:wakelocktag");
        mWakeLock.acquire();


        // monitor various sensor measurements
        displayIMUSensorMeasurements();
        mLabelInterfaceTime.setText(R.string.ready_title);
    }

    class BackgroundTask extends AsyncTask<String,String,Void>
    {

        @Override
        protected Void doInBackground(String... voids) {
            try {
                String msg = voids[0];
                if(SocketState == 0) {
                    msg = "1,2,3,4,5,6,7,8,9";
                    ipData = voids[1];
                    con = new DatagramSocket();
//                    writer = new PrintWriter(con.getOutputStream());
//                    writer.write(msg);
//                    writer.flush();
                    byte[] buf = msg.getBytes();
                    DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ipData.split(":")[0]), Integer.parseInt(ipData.split(":")[1]));
                    con.send(p);
                    SocketState = 1;
                }else if(SocketState == 1){
                    con = new DatagramSocket();
                    byte[] buf = msg.getBytes();
//                    showToast(ipData);
                    DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ipData.split(":")[0]), Integer.parseInt(ipData.split(":")[1]));
                    con.send(p);
//                    writer.write(msg);
//                    writer.flush();
                }else if(SocketState == 2){
//                    writer.close();
                    SocketState = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_ANDROID);
        }
        updateConfig();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        if (mIsRecording.get()) {
            stopRecording();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mIMUSession.unregisterSensors();
        super.onDestroy();
    }


    // methods
    public void connection(View view) {
        if (!mIsRecording.get()) {

            // start recording sensor measurements when button is pressed
            startConnection();

        } else {

            // stop recording sensor measurements when button is pressed
            stopRecording();

            mLabelInterfaceTime.setText(R.string.ready_title);
        }
    }


    private void startConnection() {

        mIsRecording.set(true);

        // update Start/Stop button UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setEnabled(true);
                mStartStopButton.setText(R.string.stop_title);
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String connectionData = sharedPreferences.getString("ipaddress", "") + ":" + sharedPreferences.getString("port", "");

        BackgroundTask bl = new BackgroundTask();
        bl.execute("Hello World", connectionData);

        showToast("Connection starts with " + connectionData);

    }



    protected void stopRecording() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                // stop each session
                mIMUSession.stopSession();
                mIsRecording.set(false);
                SocketState = 2;
                BackgroundTask bl = new BackgroundTask();
                bl.execute("Hello World", "connectionData");

                // update screen UI and button
                showToast("Connection stops!");
                resetUI();
            }
        });
    }


    private static boolean hasPermissions(Context context, String... permissions) {

        // check Android hardware permissions
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private void updateConfig() {
        final int MICRO_TO_SEC = 1000;
    }


    public void showAlertAndStop(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(text)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                stopRecording();
                            }
                        }).show();
            }
        });
    }


    public void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void resetUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setEnabled(true);
                mStartStopButton.setText(R.string.start_title);
            }
        });
    }


    @Override
    public void onBackPressed() {

        // nullify back button when recording starts
        if (!mIsRecording.get()) {
            super.onBackPressed();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_ANDROID) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                showToast("Permission not granted");
                finish();
                return;
            }
        }
    }


    private void initializeViews() {

        mLabelAccelDataX = (TextView) findViewById(R.id.label_accel_X);
        mLabelAccelDataY = (TextView) findViewById(R.id.label_accel_Y);
        mLabelAccelDataZ = (TextView) findViewById(R.id.label_accel_Z);

        mLabelAccelBiasX = (TextView) findViewById(R.id.label_accel_bias_X);
        mLabelAccelBiasY = (TextView) findViewById(R.id.label_accel_bias_Y);
        mLabelAccelBiasZ = (TextView) findViewById(R.id.label_accel_bias_Z);

        mLabelGyroDataX = (TextView) findViewById(R.id.label_gyro_X);
        mLabelGyroDataY = (TextView) findViewById(R.id.label_gyro_Y);
        mLabelGyroDataZ = (TextView) findViewById(R.id.label_gyro_Z);

        mLabelGyroBiasX = (TextView) findViewById(R.id.label_gyro_bias_X);
        mLabelGyroBiasY = (TextView) findViewById(R.id.label_gyro_bias_Y);
        mLabelGyroBiasZ = (TextView) findViewById(R.id.label_gyro_bias_Z);

        mLabelMagnetDataX = (TextView) findViewById(R.id.label_magnet_X);
        mLabelMagnetDataY = (TextView) findViewById(R.id.label_magnet_Y);
        mLabelMagnetDataZ = (TextView) findViewById(R.id.label_magnet_Z);

        mLabelMagnetBiasX = (TextView) findViewById(R.id.label_magnet_bias_X);
        mLabelMagnetBiasY = (TextView) findViewById(R.id.label_magnet_bias_Y);
        mLabelMagnetBiasZ = (TextView) findViewById(R.id.label_magnet_bias_Z);

        mStartStopButton = (Button) findViewById(R.id.button_start_stop);
        mLabelInterfaceTime = (TextView) findViewById(R.id.label_interface_time);
    }


    private void displayIMUSensorMeasurements() {

        // get IMU sensor measurements from IMUSession
        final float[] acce_data = mIMUSession.getAcceMeasure();
        final float[] acce_bias = mIMUSession.getAcceBias();

        final float[] gyro_data = mIMUSession.getGyroMeasure();
        final float[] gyro_bias = mIMUSession.getGyroBias();

        final float[] magnet_data = mIMUSession.getMagnetMeasure();
        final float[] magnet_bias = mIMUSession.getMagnetBias();

        // update current screen (activity)
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String SensorData = "";
                mLabelAccelDataX.setText(String.format(Locale.US, "%.3f", acce_data[0]));
                mLabelAccelDataY.setText(String.format(Locale.US, "%.3f", acce_data[1]));
                mLabelAccelDataZ.setText(String.format(Locale.US, "%.3f", acce_data[2]));

                SensorData = SensorData + (String.format(Locale.US, "%.3f", acce_data[0])) + ",";
                SensorData = SensorData + (String.format(Locale.US, "%.3f", acce_data[1])) + ",";
                SensorData = SensorData + (String.format(Locale.US, "%.3f", acce_data[2])) + ",";

                mLabelAccelBiasX.setText(String.format(Locale.US, "%.3f", acce_bias[0]));
                mLabelAccelBiasY.setText(String.format(Locale.US, "%.3f", acce_bias[1]));
                mLabelAccelBiasZ.setText(String.format(Locale.US, "%.3f", acce_bias[2]));

                mLabelGyroDataX.setText(String.format(Locale.US, "%.3f", gyro_data[0]));
                mLabelGyroDataY.setText(String.format(Locale.US, "%.3f", gyro_data[1]));
                mLabelGyroDataZ.setText(String.format(Locale.US, "%.3f", gyro_data[2]));

                SensorData = SensorData + (String.format(Locale.US, "%.3f", gyro_data[0])) + ",";
                SensorData = SensorData + (String.format(Locale.US, "%.3f", gyro_data[1])) + ",";
                SensorData = SensorData + (String.format(Locale.US, "%.3f", gyro_data[2])) + ",";

                mLabelGyroBiasX.setText(String.format(Locale.US, "%.3f", gyro_bias[0]));
                mLabelGyroBiasY.setText(String.format(Locale.US, "%.3f", gyro_bias[1]));
                mLabelGyroBiasZ.setText(String.format(Locale.US, "%.3f", gyro_bias[2]));

                mLabelMagnetDataX.setText(String.format(Locale.US, "%.3f", magnet_data[0]));
                mLabelMagnetDataY.setText(String.format(Locale.US, "%.3f", magnet_data[1]));
                mLabelMagnetDataZ.setText(String.format(Locale.US, "%.3f", magnet_data[2]));

                SensorData = SensorData + (String.format(Locale.US, "%.3f", magnet_data[0])) + ",";
                SensorData = SensorData + (String.format(Locale.US, "%.3f", magnet_data[1])) + ",";
                SensorData = SensorData + (String.format(Locale.US, "%.3f", magnet_data[2]));

                mLabelMagnetBiasX.setText(String.format(Locale.US, "%.3f", magnet_bias[0]));
                mLabelMagnetBiasY.setText(String.format(Locale.US, "%.3f", magnet_bias[1]));
                mLabelMagnetBiasZ.setText(String.format(Locale.US, "%.3f", magnet_bias[2]));

                if(SocketState == 1) {
                    bl1 = new BackgroundTask();
                    bl1.execute(SensorData, "connectionData");
                }
            }
        });

        // determine display update rate (100 ms)
        final long displayInterval = 100;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayIMUSensorMeasurements();
            }
        }, displayInterval);
    }


    private String interfaceIntTime(final int second) {

        // check second input
        if (second < 0) {
            showAlertAndStop("Second cannot be negative.");
        }

        // extract hour, minute, second information from second
        int input = second;
        int hours = input / 3600;
        input = input % 3600;
        int mins = input / 60;
        int secs = input % 60;

        // return interface int time
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs);
    }

}