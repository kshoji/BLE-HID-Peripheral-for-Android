package jp.kshoji.blehid.sample;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import jp.kshoji.blehid.AbsoluteMousePeripheral;
import jp.kshoji.blehid.MousePeripheral;
import jp.kshoji.blehid.sample.R.id;
import jp.kshoji.blehid.sample.R.layout;
import jp.kshoji.blehid.sample.R.string;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Activity for BLE Mouse peripheral
 * 
 * @author K.Shoji
 */
public class AbsoluteMouseActivity extends AbstractBleActivity {

    private AbsoluteMousePeripheral mouse;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_absolute_mouse);
        
        setTitle(getString(string.ble_mouse));

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        findViewById(id.activity_mouse).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        if (mouse != null) {
                            mouse.movePointer((int) (32767 * motionEvent.getX() / metrics.widthPixels), (int) (32767 * motionEvent.getY() / metrics.heightPixels), 0, true, false, false);
                        }
                        return true;
                    
                    case ACTION_MOVE:
                        if (mouse != null) {
                            mouse.movePointer((int) (32767 * motionEvent.getX() / metrics.widthPixels), (int) (32767 * motionEvent.getY() / metrics.heightPixels), 0, true, false, false);
                        }
                        return true;
                    
                    case ACTION_UP:
                    case ACTION_POINTER_UP:
                        mouse.movePointer((int) (32767 * motionEvent.getX() / metrics.widthPixels), (int) (32767 * motionEvent.getY() / metrics.heightPixels), 0, false, false, false);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    void setupBlePeripheralProvider() {
        mouse = new AbsoluteMousePeripheral(this);
        mouse.setDeviceName(getString(string.ble_mouse));
        mouse.startAdvertising();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mouse != null) {
            mouse.stopAdvertising();
        }
    }
}
