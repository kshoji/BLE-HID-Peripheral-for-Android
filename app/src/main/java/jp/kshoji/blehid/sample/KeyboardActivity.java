package jp.kshoji.blehid.sample;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import jp.kshoji.blehid.KeyboardPeripheral;
import jp.kshoji.blehid.sample.R.id;
import jp.kshoji.blehid.sample.R.layout;
import jp.kshoji.blehid.sample.R.string;

/**
 * Activity for BLE Keyboard peripheral
 *
 * @author K.Shoji
 */
public class KeyboardActivity extends AbstractBleActivity {

    private KeyboardPeripheral keyboard;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_keyboard);

        setTitle(getString(string.ble_keyboard));

        findViewById(id.typeButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        });
    }

    @Override
    void setupBlePeripheralProvider() {
        keyboard = new KeyboardPeripheral(this);
        keyboard.setDeviceName(getString(string.ble_keyboard));
        keyboard.startAdvertising();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyboard != null) {
            if(keyCode == KeyEvent.KEYCODE_DEL)
                keyboard.sendKeys("\b"); //for backspace
            else {
                char character = (char) event.getUnicodeChar(event.getMetaState());
                keyboard.sendKeys(String.valueOf(character));
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboard != null)
            keyboard.stopAdvertising();
    }
}
