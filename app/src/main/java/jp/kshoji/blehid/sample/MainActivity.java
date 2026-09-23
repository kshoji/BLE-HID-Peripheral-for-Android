package jp.kshoji.blehid.sample;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;

import jp.kshoji.blehid.sample.R.id;
import jp.kshoji.blehid.sample.R.layout;
import jp.kshoji.blehid.sample.R.string;

/**
 * Main Activity
 * 
 * @author K.Shoji
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        setTitle(getString(string.ble_hid));
        
        findViewById(id.mouseButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MouseActivity.class));
            }
        });
        findViewById(id.absoluteMouseButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), AbsoluteMouseActivity.class));
            }
        });
        findViewById(id.keyboardButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), KeyboardActivity.class));
            }
        });
        findViewById(id.joystickButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), JoystickActivity.class));
            }
        });
    }
}
