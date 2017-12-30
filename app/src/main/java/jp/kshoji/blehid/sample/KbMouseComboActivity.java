package jp.kshoji.blehid.sample;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import jp.kshoji.blehid.KbMousePeripheral;
import jp.kshoji.blehid.sample.R.string;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

/**
 * @author shinilms
 */

public class KbMouseComboActivity extends AbstractBleActivity
        implements View.OnTouchListener {

    private KbMousePeripheral comboPeripheral;
    private RelativeLayout mouse_layout;
    private LinearLayout keyboard_layout;

    private float X, Y, firstX, firstY;
    private int maxPointerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kb_mouse_combo);

        setTitle(getString(R.string.ble_kb_mouse));

        mouse_layout = (RelativeLayout) findViewById(R.id.mouse_layout);
        keyboard_layout = (LinearLayout) findViewById(R.id.type_layout);
        mouse_layout.setOnTouchListener(this);
    }

    @Override
    void setupBlePeripheralProvider() {
        comboPeripheral = new KbMousePeripheral(this);
        comboPeripheral.setDeviceName(getString(string.ble_keyboard));
        comboPeripheral.startAdvertising();
    }

    public void forceKeyboard(View view) {
        InputMethodManager inputMethodManager =  (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(keyboard_layout.getApplicationWindowToken(),
                InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
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
                if (comboPeripheral != null) {
                    comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                }
                X = motionEvent.getX();
                Y = motionEvent.getY();
                return true;

            case ACTION_UP:
            case ACTION_POINTER_UP:
                X = motionEvent.getX();
                Y = motionEvent.getY();
                if ((X-firstX) * (X-firstX) + (Y-firstY) * (Y-firstY) < 20) {
                    if (comboPeripheral != null) {
                        if (maxPointerCount == 1) {
                            comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, true, false, false);
                            comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                        } else if (maxPointerCount == 2) {
                            comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, true);
                            comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                        } else if (maxPointerCount > 2) {
                            comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, true, false);
                            comboPeripheral.movePointer((int) (motionEvent.getX() - X), (int) (motionEvent.getY() - Y), 0, false, false, false);
                        }
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (comboPeripheral != null) {
            if(keyCode == KeyEvent.KEYCODE_DEL)
                comboPeripheral.sendKeys("\b"); //for backspace
            else {
                char character = (char) event.getUnicodeChar(event.getMetaState());
                comboPeripheral.sendKeys(String.valueOf(character));
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (comboPeripheral != null)
            comboPeripheral.stopAdvertising();
    }
}
