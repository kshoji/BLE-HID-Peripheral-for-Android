package jp.kshoji.blehid.sample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import jp.kshoji.blehid.sample.R.string;
import jp.kshoji.blehid.util.BleUtils;

/**
 * Common procedures for BLE activities
 *
 * @author K.Shoji
 */
public abstract class AbstractBleActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BLE_PERMISSIONS = 0xb1e2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startBleSetup();
    }

    abstract void setupBlePeripheralProvider();

    private void startBleSetup() {
        if (!hasBlePermissions()) {
            requestBlePermissions();
            return;
        }

        if (!BleUtils.isBluetoothEnabled(this)) {
            BleUtils.enableBluetooth(this);
            return;
        }

        proceedWithBleSupportCheck();
    }

    private void proceedWithBleSupportCheck() {
        if (!BleUtils.isBleSupported(this) || !BleUtils.isBlePeripheralSupported(this)) {
            final AlertDialog alertDialog = new Builder(this).create();
            alertDialog.setTitle(getString(string.not_supported));
            alertDialog.setMessage(getString(string.ble_perip_not_supported));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(string.ok),
                    new OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(final DialogInterface dialog) {
                    finish();
                }
            });
            alertDialog.show();
        } else {
            setupBlePeripheralProvider();
        }
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_SCAN
                },
                REQUEST_CODE_BLE_PERMISSIONS
        );
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_BLE_PERMISSIONS) {
            return;
        }
        if (hasBlePermissions()) {
            startBleSetup();
        } else {
            Toast.makeText(this, string.requires_bl_enabled, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BleUtils.REQUEST_CODE_BLUETOOTH_ENABLE) {
            if (!BleUtils.isBluetoothEnabled(this)) {
                Toast.makeText(this, string.requires_bl_enabled, Toast.LENGTH_LONG).show();
                return;
            }
            proceedWithBleSupportCheck();
        }
    }
}
