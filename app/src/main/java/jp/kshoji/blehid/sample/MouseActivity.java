package jp.kshoji.blehid.sample;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

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
public class MouseActivity extends AbstractBleActivity {

    private MousePeripheral mouse;
    private float X, Y, firstX, firstY;
    private int maxPointerCount;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_mouse);
        
        setTitle(getString(string.ble_mouse));

        findViewById(id.activity_mouse).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        maxPointerCount = motionEvent.getPointerCount();
                        X = motionEvent.getX();
                        Y = motionEvent.getY();
                        firstX = X;
                        firstY = Y;
                        return true;
                    
                    case ACTION_MOVE:
                        maxPointerCount = Math.max(maxPointerCount, motionEvent.getPointerCount());
                        if (mouse != null) {
                            mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                        }
                        X = motionEvent.getX();
                        Y = motionEvent.getY();
                        return true;
                    
                    case ACTION_UP:
                    case ACTION_POINTER_UP:
                        X = motionEvent.getX();
                        Y = motionEvent.getY();
                        if ((X-firstX) * (X-firstX) + (Y-firstY) * (Y-firstY) < 20) {
                            if (mouse != null) {
                                if (maxPointerCount == 1) {
                                    mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, true, false, false);
                                    mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                                } else if (maxPointerCount == 2) {
                                    mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, true);
                                    mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                                } else if (maxPointerCount > 2) {
                                    mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, true, false);
                                    mouse.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                                }
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    void setupBlePeripheralProvider() {
        mouse = new MousePeripheral(this);
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
