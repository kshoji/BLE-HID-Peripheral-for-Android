package jp.kshoji.blehid.sample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import jp.kshoji.blehid.JoystickPeripheral;
import jp.kshoji.blehid.sample.R.id;
import jp.kshoji.blehid.sample.R.layout;
import jp.kshoji.blehid.sample.R.string;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Activity for BLE Joystick peripheral
 * 
 * @author K.Shoji
 */
public class JoystickActivity extends AbstractBleActivity implements SensorEventListener {

    private JoystickPeripheral joystick;
    private boolean left, middle, right;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_joystick);

        setTitle(getString(string.ble_joystick));
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        
        findViewById(id.leftButton).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        left = true;
                        return true;

                    case ACTION_UP:
                    case ACTION_POINTER_UP:
                        left = false;
                        return true;
                }
                return false;
            }
        });
        findViewById(id.middleButton).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        middle = true;
                        return true;

                    case ACTION_UP:
                    case ACTION_POINTER_UP:
                        middle = false;
                        return true;
                }
                return false;
            }
        });
        findViewById(id.rightButton).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        right = true;
                        return true;

                    case ACTION_UP:
                    case ACTION_POINTER_UP:
                        right = false;
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    void setupBlePeripheralProvider() {
        joystick = new JoystickPeripheral(this);
        joystick.setDeviceName(getString(string.ble_joystick));
        joystick.startAdvertising();
    }

    private final float[] gravity = new float[3];
    private final float[] linear_acceleration = new float[3];
    private final float[] velocity = new float[3];
    @Override
    public void onSensorChanged(final SensorEvent event){
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        
        velocity[0] += linear_acceleration[0];
        velocity[1] += linear_acceleration[1];
        velocity[2] += linear_acceleration[2];
        
        if (joystick != null) {
            joystick.movePointer((int) velocity[0], (int) velocity[1], (int) velocity[2], left, middle, right);
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        // do nothing
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this, accelerometerSensor);
                
        if (joystick != null) {
            joystick.stopAdvertising();
        }
    }
}
