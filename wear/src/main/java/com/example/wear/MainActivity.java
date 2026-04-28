package com.example.wear;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;


import com.example.wear.Database.Database;
import com.example.wear.Prediction.Prediction;
import com.example.wear.config.ModelConfig;
import com.example.wear.config.SmartFallConfig;
import com.example.wear.util.Event;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    /**
     * A string TAG for debugging
     */
    private final String TAG = "MainActivity";

    public static boolean predictionInitialized = false;

    public static String uuid = null;

    public static boolean feedbackEnabled = true;



    /**
     *
     */
    Intent sensorIntent;


    /**
     * A constant integer representing that the received feedback label is a false positive label
     */
    public static final int FEEDBACK_LABEL_FP = 0;

    /**
     * A constant integer representing that the received feedback is a true positive label
     */
    public static final int FEEDBACK_LABEL_TP = 1;
    /**
     * A constant integer representing that the received feedback is a false negative label
     */
    private static final int FEEDBACK_LABEL_FN = 3;

    /**
     * A constant integer representing that the received feedback is a request for help
     */
    public static final int FEEDBACK_NEEDS_HELP = 2;

    /**
     * A constant integer representing that the state of the data collection is actively collecting
     */
    public static final int COLLECTION_STATE = 0;

    /**
     * A constant integer representing that the state of the data collection is awaiting feedback
     */
    public static final int FEEDBACK_STATE = 1;

    /**
     * A constant integer representing that the state of the data collection is inactive
     */
    public static final int IDLE_STATE = 2;

    /**
     * A constant string representing the designated path for receiving accelerometer data
     */
    public static final String ACCELEROMETER_EVENT_PATH = "accelerometer";

    /**
     * A constant string representing the designated path for receiving state data
     */
    public static final String STATE_EVENT_PATH = "state";

    /**
     * A constant string representing the designated path for receiving feedback data
     */
    public static final String FEEDBACK_EVENT_PATH = "feedback";

    public TextView tv2;
    private TextView xText, yText, zText, inferenceCountText, thresholdValueText;
    private SwitchMaterial manualToggle;
    private Slider thresholdSlider;
    private EditText targetIpInput;
    private MaterialButton assitBtn;

    /**
     * A list of all declared paths used for sending and receiving ble data
     */
    private static String [] blePaths = new String []
            { ACCELEROMETER_EVENT_PATH, STATE_EVENT_PATH, FEEDBACK_EVENT_PATH };

    public Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        initializeAll(getApplicationContext());
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }


        String message = UUID.randomUUID().toString();

        SharedPreferences pref;
        SharedPreferences.Editor editor;
        pref = getApplicationContext().getSharedPreferences("Fall_Detection",0);
        editor = pref.edit();

        editor.putString("uuid", message);
        editor.commit();

        assitBtn = (MaterialButton) findViewById(R.id.assitBtn);
        assitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(MainActivity.this, FeedbackActivity.class);
                startActivity(intent1);
            }
        });

        tv2 = findViewById(R.id.textView2);
        xText = findViewById(R.id.x_axis);
        yText = findViewById(R.id.y_axis);
        zText = findViewById(R.id.z_axis);
        inferenceCountText = findViewById(R.id.inference_count);

        manualToggle = findViewById(R.id.manualToggle);
        manualToggle.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startMonitoring();
                } else {
                    stopMonitoring();
                }
            }
        });

        thresholdValueText = findViewById(R.id.threshold_value_text);
        thresholdSlider = findViewById(R.id.threshold_slider);
        
        // Load initial value
        float currentThreshold = pref.getFloat("model_threshold", com.example.wear.config.SmartFallConfig.OFFLINE_MODEL_THRESHOLD);
        thresholdSlider.setValue(currentThreshold);
        thresholdValueText.setText(String.format(Locale.US, "%.2f", currentThreshold));

        thresholdSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                thresholdValueText.setText(String.format(Locale.US, "%.2f", value));
                Prediction.setThreshold(value, getApplicationContext());
            }
        });

        targetIpInput = findViewById(R.id.target_ip_input);
        String savedIp = pref.getString("target_ip", "10.205.203.251");
        com.example.wear.util.UdpClient.TARGET_IP = savedIp;
        targetIpInput.setText(savedIp);

        targetIpInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String ip = s.toString().trim();
                com.example.wear.util.UdpClient.TARGET_IP = ip;
                pref.edit().putString("target_ip", ip).apply();
            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        IntentFilter dataReceiverFilter = new IntentFilter("data-receiver");
        DataReceiver dataReceiver = new DataReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, dataReceiverFilter);

        IntentFilter sensorDataFilter = new IntentFilter("sensor-data");
        SensorDataReceiver sensorDataReceiver = new SensorDataReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver, sensorDataFilter);

        IntentFilter feedbackFilter = new IntentFilter("FeedbackIntent");
        FeedbackReceiver feedbackReceiver = new FeedbackReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(feedbackReceiver, feedbackFilter);

        IntentFilter collectionFilter = new IntentFilter("CollectionIntent");
        CollectionReceiver collectionReceiver = new CollectionReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(collectionReceiver, collectionFilter);


        IntentFilter IdleFilter = new IntentFilter("IdleIntent");
        IdleReceiver IdleReceiver = new IdleReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(IdleReceiver, IdleFilter);

    }


    private void startMonitoring() {
        feedbackEnabled = true;
        initializeAll(getApplicationContext());
        sensorIntent = new Intent(MainActivity.this, SensorService.class);
        ContextCompat.startForegroundService(MainActivity.this, sensorIntent);
        tv2.setText("ACTIVATED");
        tv2.setTextColor(getResources().getColor(R.color.status_active));
        if (!manualToggle.isChecked()) {
            manualToggle.setChecked(true);
        }
    }

    private void stopMonitoring() {
        feedbackEnabled = false;
        Prediction.uploadTracker(context);
        if (sensorIntent != null) {
            stopService(sensorIntent);
        }
        Prediction.reset();
        tv2.setText("DEACTIVATED");
        tv2.setTextColor(getResources().getColor(R.color.status_standby));
        if (manualToggle.isChecked()) {
            manualToggle.setChecked(false);
        }
    }

    public void  initializeAll(Context context){
        try {
            /** Database **/
            Database.initialize(context);
            /** Prediction **/
            Prediction.initialize(context);
            predictionInitialized = true;
            ModelConfig config = ModelConfig.getModelConfig(context);
            config.uuid = context.getSharedPreferences("Fall_Detection",0).getString("uuid",null);
            if(!SmartFallConfig.MODEL_TYPE.equals("PERSONALIZED")){
                config.modelVersion = 0;
                config.newDocID = SmartFallConfig.MODEL_TYPE.toLowerCase();
                config.modelContent = null;
                config.modelWeights = null;
            }
            Prediction.updateTracker(context);
        } catch (Exception e){
            e.printStackTrace();
            tv2.setText("MODEL ERROR");
            tv2.setTextColor(getResources().getColor(R.color.status_standby));
        }

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }




    // Register the local broadcast receiver to receive messages from the listener.
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String path = intent.getStringExtra("path");
            String message = intent.getStringExtra("message");
            Log.v(TAG, "Main activity received message: " + message);
            Log.v(TAG, "Main activity received path: " + path);

            if(path.equals("/user/uuid")) {

                SharedPreferences pref;
                SharedPreferences.Editor editor;
                pref = getApplicationContext().getSharedPreferences("Fall_Detection", 0);
                editor = pref.edit();

                editor.putString("uuid", message);
                editor.commit();
            }
            else {
                if(message.equals("on")) {
                    startMonitoring();
                }
                else {
                    stopMonitoring();
                }
            }
        }
    }

    public class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                String[] parts = message.split(",");
                if (parts.length >= 3) {
                    try {
                        xText.setText(String.format(Locale.US, "%.2f", Float.parseFloat(parts[0])));
                        yText.setText(String.format(Locale.US, "%.2f", Float.parseFloat(parts[1])));
                        zText.setText(String.format(Locale.US, "%.2f", Float.parseFloat(parts[2])));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (parts.length >= 4) {
                    inferenceCountText.setText(parts[3]);
                }
            }
        }
    }

    public class FeedbackReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(feedbackEnabled) {

                feedbackEnabled = false;
                String message = intent.getStringExtra("message");
                Log.v(TAG, "Main activity received message: " + message);
                stopService(sensorIntent);
                Prediction.reset();
//            initializeAll(context);
                Intent intent1 = new Intent(MainActivity.this, FeedbackActivity.class);
                startActivity(intent1);
            }
        }
    }

    public class CollectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            feedbackEnabled = true;
            String message = intent.getStringExtra("message");
            Log.v(TAG, "Main activity received message: " + message);
            Prediction.reset();
            initializeAll(context);
            sensorIntent = new Intent(MainActivity.this, SensorService.class );
            ContextCompat.startForegroundService(MainActivity.this, sensorIntent);
        }
    }

    public class IdleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v(TAG, "Main activity received message: " + message);
            feedbackEnabled = false;
            stopService(sensorIntent);
        }
    }

    public class SensorDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");

            String[] parts = data.split(",");
            float[] mmSensorValues = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    mmSensorValues[i] = Float.parseFloat(parts[i]);
                } catch (NumberFormatException e) {
                    // Handle the exception (e.g., print an error message or throw the exception)
                    e.printStackTrace();
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(4 * mmSensorValues.length);

            for (float value : mmSensorValues) {
                buffer.putFloat(value);
            }

            byte[] bytes = buffer.array();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            Event event = new Event(bytes, timestamp, UUID.fromString(getApplicationContext().getSharedPreferences("Fall_Detection", 0).getString("uuid", null)));

            try {
                Prediction.makePrediction(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}