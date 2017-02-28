package jp.kshoji.blehid.sample;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

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

        findViewById(id.sendButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (keyboard != null) {
                    keyboard.sendKeys(((TextView) findViewById(id.editText)).getText().toString());
                }
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
    protected void onDestroy() {
        super.onDestroy();
        
        if (keyboard != null) {
            keyboard.stopAdvertising();
        }
    }
}
