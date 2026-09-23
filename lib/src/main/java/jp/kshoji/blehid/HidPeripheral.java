package jp.kshoji.blehid;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseData.Builder;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import jp.kshoji.blehid.util.BleUuidUtils;

/**
 * BLE HID over GATT base features
 *
 * @author K.Shoji
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
public abstract class HidPeripheral {
    private static final String TAG = HidPeripheral.class.getSimpleName();

    /**
     * Main items
     */
    protected static byte INPUT(final int size) {
        return (byte) (0x80 | size);
    }
    protected static byte OUTPUT(final int size) {
        return (byte) (0x90 | size);
    }
    protected static byte COLLECTION(final int size) {
        return (byte) (0xA0 | size);
    }
    protected static byte FEATURE(final int size) {
        return (byte) (0xB0 | size);
    }
    protected static byte END_COLLECTION(final int size) {
        return (byte) (0xC0 | size);
    }

    /**
     * Global items
     */
    protected static byte USAGE_PAGE(final int size) {
        return (byte) (0x04 | size);
    }
    protected static byte LOGICAL_MINIMUM(final int size) {
        return (byte) (0x14 | size);
    }
    protected static byte LOGICAL_MAXIMUM(final int size) {
        return (byte) (0x24 | size);
    }
    protected static byte PHYSICAL_MINIMUM(final int size) {
        return (byte) (0x34 | size);
    }
    protected static byte PHYSICAL_MAXIMUM(final int size) {
        return (byte) (0x44 | size);
    }
    protected static byte UNIT_EXPONENT(final int size) {
        return (byte) (0x54 | size);
    }
    protected static byte UNIT(final int size) {
        return (byte) (0x64 | size);
    }
    protected static byte REPORT_SIZE(final int size) {
        return (byte) (0x74 | size);
    }
    protected static byte REPORT_ID(final int size) {
        return (byte) (0x84 | size);
    }
    protected static byte REPORT_COUNT(final int size) {
        return (byte) (0x94 | size);
    }

    /**
     * Local items
     */
    protected static byte USAGE(final int size) {
        return (byte) (0x08 | size);
    }
    protected static byte USAGE_MINIMUM(final int size) {
        return (byte) (0x18 | size);
    }
    protected static byte USAGE_MAXIMUM(final int size) {
        return (byte) (0x28 | size);
    }

    protected static byte LSB(final int value) {
        return (byte) (value & 0xff);
    }
    protected static byte MSB(final int value) {
        return (byte) (value >> 8 & 0xff);
    }
    
    /**
     * Device Information Service
     */
    private static final UUID SERVICE_DEVICE_INFORMATION = BleUuidUtils.fromShortValue(0x180A);
    private static final UUID CHARACTERISTIC_MANUFACTURER_NAME = BleUuidUtils.fromShortValue(0x2A29);
    private static final UUID CHARACTERISTIC_MODEL_NUMBER = BleUuidUtils.fromShortValue(0x2A24);
    private static final UUID CHARACTERISTIC_SERIAL_NUMBER = BleUuidUtils.fromShortValue(0x2A25);
    private static final int DEVICE_INFO_MAX_LENGTH = 20;

    private String manufacturer = "kshoji.jp";
    private String deviceName = "BLE HID";
    private String serialNumber = "12345678";

    /**
     * Battery Service
     */
    private static final UUID SERVICE_BATTERY = BleUuidUtils.fromShortValue(0x180F);
    private static final UUID CHARACTERISTIC_BATTERY_LEVEL = BleUuidUtils.fromShortValue(0x2A19);

    /**
     * HID Service
     */
    private static final UUID SERVICE_BLE_HID = BleUuidUtils.fromShortValue(0x1812);
    private static final UUID CHARACTERISTIC_HID_INFORMATION = BleUuidUtils.fromShortValue(0x2A4A);
    private static final UUID CHARACTERISTIC_REPORT_MAP = BleUuidUtils.fromShortValue(0x2A4B);
    private static final UUID CHARACTERISTIC_HID_CONTROL_POINT = BleUuidUtils.fromShortValue(0x2A4C);
    private static final UUID CHARACTERISTIC_REPORT = BleUuidUtils.fromShortValue(0x2A4D);
    private static final UUID CHARACTERISTIC_PROTOCOL_MODE = BleUuidUtils.fromShortValue(0x2A4E);

    /**
     * Represents Report Map byte array
     * @return Report Map data
     */
    protected abstract byte[] getReportMap();
    
    /**
     * HID Input Report
     */
    private final Queue<byte[]> inputReportQueue = new ConcurrentLinkedQueue<>();
    protected final void addInputReport(final byte[] inputReport) {
        if (inputReport != null && inputReport.length > 0) {
            inputReportQueue.offer(inputReport);
        }
    }

    /**
     * HID Output Report
     *
     * @param outputReport the report data
     */
    protected abstract void onOutputReport(final byte[] outputReport);

    /**
     * Gatt Characteristic Descriptor
     */
    private static final UUID DESCRIPTOR_REPORT_REFERENCE = BleUuidUtils.fromShortValue(0x2908);
    private static final UUID DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION = BleUuidUtils.fromShortValue(0x2902);

    private static final byte[] EMPTY_BYTES = {};
    private static final byte[] RESPONSE_HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};

    /**
     * Instances for the peripheral
     */
    private final Context applicationContext;
    private final Handler handler;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattCharacteristic inputReportCharacteristic;
    @Nullable
    private BluetoothGattServer gattServer;
    private final Map<String, BluetoothDevice> bluetoothDevicesMap = new HashMap<>();

    /**
     * GATT services must be added one-by-one, waiting for {@link BluetoothGattServerCallback#onServiceAdded}.
     */
    private final Deque<BluetoothGattService> pendingGattServices = new ConcurrentLinkedDeque<>();
    @Nullable
    private BluetoothGattService gattServiceBeingAdded;
    private volatile boolean gattServicesReady;
    private boolean advertisingRequested;
    private boolean useReducedAdvertiseData;

    /**
     * Connection state reported to client code (from PR #10).
     */
    public static final class ConnectionState {
        public final BluetoothDevice device;
        public final int status;
        public final int newState;

        public ConnectionState(@NonNull final BluetoothDevice device, final int status, final int newState) {
            this.device = device;
            this.status = status;
            this.newState = newState;
        }
    }

    /**
     * Listener for GATT connection state changes.
     * Prefer this over {@code java.util.function.Consumer} so minSdk 21 stays supported without desugaring.
     */
    public interface ConnectionStateCallback {
        void onConnectionStateChanged(@NonNull ConnectionState connectionState);
    }

    @Nullable
    private ConnectionStateCallback connectionStateCallback;

    /**
     * Sets a callback invoked when a Central connects or disconnects.
     *
     * @param connectionStateCallback the callback, or null to clear
     */
    public void setConnectionStateCallback(@Nullable final ConnectionStateCallback connectionStateCallback) {
        this.connectionStateCallback = connectionStateCallback;
    }

    private void notifyConnectionState(@NonNull final BluetoothDevice device, final int status, final int newState) {
        final ConnectionStateCallback callback = connectionStateCallback;
        if (callback != null) {
            callback.onConnectionStateChanged(new ConnectionState(device, status, newState));
        }
    }

    /**
     * Constructor<br />
     * Before constructing the instance, check the Bluetooth availability.
     *
     * @param context the ApplicationContext
     * @param needInputReport true: serves 'Input Report' BLE characteristic
     * @param needOutputReport true: serves 'Output Report' BLE characteristic
     * @param needFeatureReport true: serves 'Feature Report' BLE characteristic
     * @param dataSendingRate sending rate in milliseconds
     * @throws UnsupportedOperationException if starting Bluetooth LE Peripheral failed
     */
    protected HidPeripheral(final Context context, final boolean needInputReport, final boolean needOutputReport, final boolean needFeatureReport, final int dataSendingRate) throws UnsupportedOperationException {
        applicationContext = context.getApplicationContext();
        handler = new Handler(applicationContext.getMainLooper());

        final BluetoothManager bluetoothManager = (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);

        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            throw new UnsupportedOperationException("Bluetooth is not available.");
        }

        if (!bluetoothAdapter.isEnabled()) {
            throw new UnsupportedOperationException("Bluetooth is disabled.");
        }

        Log.d(TAG, "isMultipleAdvertisementSupported:" + bluetoothAdapter.isMultipleAdvertisementSupported());
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            throw new UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.");
        }

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        Log.d(TAG, "bluetoothLeAdvertiser: " + bluetoothLeAdvertiser);
        if (bluetoothLeAdvertiser == null) {
            throw new UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.");
        }

        this.bluetoothAdapter = bluetoothAdapter;

        gattServer = bluetoothManager.openGattServer(applicationContext, gattServerCallback);
        if (gattServer == null) {
            throw new UnsupportedOperationException("gattServer is null, check Bluetooth is ON.");
        }

        // Queue services; add sequentially in onServiceAdded to avoid flaky GATT setup
        pendingGattServices.offerLast(setUpHidService(needInputReport, needOutputReport, needFeatureReport));
        pendingGattServices.offerLast(setUpDeviceInformationService());
        pendingGattServices.offerLast(setUpBatteryService());
        handler.post(new Runnable() {
            @Override
            public void run() {
                addNextGattService();
            }
        });
        
        // send report each dataSendingRate, if data available
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final byte[] polled = inputReportQueue.poll();
                if (polled != null && inputReportCharacteristic != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyInputReport(polled);
                        }
                    });
                }
            }
        }, 0, dataSendingRate);
    }

    /**
     * Track a Central so input reports can be notified to it.
     */
    private void registerDevice(@NonNull final BluetoothDevice device) {
        synchronized (bluetoothDevicesMap) {
            bluetoothDevicesMap.put(device.getAddress(), device);
        }
        Log.d(TAG, "Registered device for notifications: " + device.getAddress());
    }

    /**
     * Send one HID input report to all registered Centrals.
     */
    private void notifyInputReport(@NonNull final byte[] report) {
        if (gattServer == null || inputReportCharacteristic == null) {
            return;
        }

        // Keep legacy setter for older stacks / descriptors that read the characteristic value
        inputReportCharacteristic.setValue(report);

        final Set<BluetoothDevice> devices = getDevices();
        if (devices.isEmpty()) {
            Log.w(TAG, "Input report dropped: no registered Centrals");
            return;
        }

        for (final BluetoothDevice device : devices) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+: value must be passed explicitly or notifications are empty/ignored
                    final int status = gattServer.notifyCharacteristicChanged(
                            device, inputReportCharacteristic, false, report);
                    if (status != BluetoothStatusCodes.SUCCESS) {
                        Log.w(TAG, "notifyCharacteristicChanged failed for "
                                + device.getAddress() + ", status=" + status);
                    }
                } else {
                    final boolean sent = gattServer.notifyCharacteristicChanged(
                            device, inputReportCharacteristic, false);
                    if (!sent) {
                        Log.w(TAG, "notifyCharacteristicChanged failed for " + device.getAddress());
                    }
                }
            } catch (final Throwable e) {
                Log.w(TAG, "notifyCharacteristicChanged error for " + device.getAddress(), e);
            }
        }
    }

    /**
     * Add the next queued GATT service, or mark ready when the queue is empty.
     */
    private void addNextGattService() {
        if (gattServer == null) {
            return;
        }

        gattServiceBeingAdded = pendingGattServices.pollFirst();
        if (gattServiceBeingAdded == null) {
            gattServicesReady = true;
            Log.d(TAG, "All GATT services added.");
            if (advertisingRequested) {
                startAdvertisingInternal();
            }
            return;
        }

        final boolean accepted = gattServer.addService(gattServiceBeingAdded);
        Log.d(TAG, "Adding Service: " + gattServiceBeingAdded.getUuid() + ", accepted: " + accepted);
        if (!accepted) {
            final BluetoothGattService retry = gattServiceBeingAdded;
            gattServiceBeingAdded = null;
            pendingGattServices.offerFirst(retry);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    addNextGattService();
                }
            }, 100);
        }
    }

    /**
     * Setup Device Information Service
     *
     * @return the service
     */
    private static BluetoothGattService setUpDeviceInformationService() {
        final BluetoothGattService service = new BluetoothGattService(SERVICE_DEVICE_INFORMATION, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_MANUFACTURER_NAME, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            while (!service.addCharacteristic(characteristic));
        }
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_MODEL_NUMBER, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            while (!service.addCharacteristic(characteristic));
        }
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_SERIAL_NUMBER, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            while (!service.addCharacteristic(characteristic)) ;
        }

        return service;
    }

    /**
     * Setup Battery Service
     *
     * @return the service
     */
    private static BluetoothGattService setUpBatteryService() {
        final BluetoothGattService service = new BluetoothGattService(SERVICE_BATTERY, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Battery Level
        final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_BATTERY_LEVEL,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);

        final BluetoothGattDescriptor clientCharacteristicConfigurationDescriptor = new BluetoothGattDescriptor(
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        clientCharacteristicConfigurationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        characteristic.addDescriptor(clientCharacteristicConfigurationDescriptor);

        while (!service.addCharacteristic(characteristic));

        return service;
    }

    /**
     * Setup HID Service
     *
     * @param isNeedInputReport true: serves 'Input Report' BLE characteristic
     * @param isNeedOutputReport true: serves 'Output Report' BLE characteristic
     * @param isNeedFeatureReport true: serves 'Feature Report' BLE characteristic
     * @return the service
     */
    private BluetoothGattService setUpHidService(final boolean isNeedInputReport, final boolean isNeedOutputReport, final boolean isNeedFeatureReport) {
        final BluetoothGattService service = new BluetoothGattService(SERVICE_BLE_HID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // HID Information
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_HID_INFORMATION,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);

            while (!service.addCharacteristic(characteristic));
        }

        // Report Map
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_REPORT_MAP,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);

            while (!service.addCharacteristic(characteristic));
        }

        // Protocol Mode
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_PROTOCOL_MODE,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(new byte[]{0x01}); // Report Protocol (required by Windows HID stack)

            while(!service.addCharacteristic(characteristic));
        }

        // HID Control Point
        {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_HID_CONTROL_POINT,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            while (!service.addCharacteristic(characteristic));
        }

        // Input Report
        if (isNeedInputReport) {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);

            final BluetoothGattDescriptor clientCharacteristicConfigurationDescriptor = new BluetoothGattDescriptor(
                    DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED); //  | BluetoothGattDescriptor.PERMISSION_WRITE
            clientCharacteristicConfigurationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            characteristic.addDescriptor(clientCharacteristicConfigurationDescriptor);

            final BluetoothGattDescriptor reportReferenceDescriptor = new BluetoothGattDescriptor(
                    DESCRIPTOR_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            characteristic.addDescriptor(reportReferenceDescriptor);

            while (!service.addCharacteristic(characteristic));
            inputReportCharacteristic = characteristic;
        }

        // Output Report
        if (isNeedOutputReport) {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            final BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                    DESCRIPTOR_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            characteristic.addDescriptor(descriptor);

            while (!service.addCharacteristic(characteristic));
        }

        // Feature Report
        if (isNeedFeatureReport) {
            final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    CHARACTERISTIC_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);

            final BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                    DESCRIPTOR_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            characteristic.addDescriptor(descriptor);

            while (!service.addCharacteristic(characteristic));
        }

        return service;
    }

    /**
     * Starts advertising.<br />
     * If GATT services are still being registered, advertising starts after they are ready.
     */
    public final void startAdvertising() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                advertisingRequested = true;
                useReducedAdvertiseData = false;
                if (!gattServicesReady) {
                    Log.d(TAG, "startAdvertising deferred until GATT services are ready.");
                    return;
                }
                startAdvertisingInternal();
            }
        });
    }

    /**
     * Build and start LE advertising. Prefer a small ADV payload so startAdvertising does not fail with DATA_TOO_LARGE.
     */
    private void startAdvertisingInternal() {
        try {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        } catch (final Exception ignored) {
            // not advertising / BT off
        }

        // Keep adapter name in sync so setIncludeDeviceName reflects setDeviceName()
        try {
            bluetoothAdapter.setName(deviceName);
        } catch (final SecurityException e) {
            Log.d(TAG, "Failed to set adapter name", e);
        }

        final AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();

        // ADV (~31 bytes): HID UUID is enough for discovery; put extras in scan response
        final Builder advertiseDataBuilder = new Builder()
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(SERVICE_BLE_HID));

        final Builder scanResultBuilder = new Builder()
                .addServiceUuid(new ParcelUuid(SERVICE_BATTERY));

        if (useReducedAdvertiseData) {
            // Device name often makes ADV exceed the limit; move it to scan response
            advertiseDataBuilder.setIncludeDeviceName(false);
            scanResultBuilder.setIncludeDeviceName(true);
        } else {
            advertiseDataBuilder.setIncludeDeviceName(true);
            scanResultBuilder.setIncludeDeviceName(false);
        }

        final AdvertiseData advertiseData = advertiseDataBuilder.build();
        final AdvertiseData scanResult = scanResultBuilder.build();

        Log.d(TAG, "startAdvertising reduced=" + useReducedAdvertiseData
                + ", advertiseData: " + advertiseData + ", scanResult: " + scanResult);
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, scanResult, advertiseCallback);
    }

    /**
     * Stops advertising
     */
    public final void stopAdvertising() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                advertisingRequested = false;
                try {
                    bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                } catch (final IllegalStateException ignored) {
                    // BT Adapter is not turned ON
                }
                try {
                    if (gattServer != null) {
                        final Set<BluetoothDevice> devices = getDevices();
                        for (final BluetoothDevice device : devices) {
                            gattServer.cancelConnection(device);
                        }

                        gattServer.close();
                        gattServer = null;
                    }
                } catch (final IllegalStateException ignored) {
                    // BT Adapter is not turned ON
                }
            }
        });
    }

    /**
     * Callback for BLE advertising
     */
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(final AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertising started: " + settingsInEffect);
        }

        @Override
        public void onStartFailure(final int errorCode) {
            final String reason;
            switch (errorCode) {
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    reason = "DATA_TOO_LARGE";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    reason = "TOO_MANY_ADVERTISERS";
                    break;
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    reason = "ALREADY_STARTED";
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    reason = "INTERNAL_ERROR";
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    reason = "FEATURE_UNSUPPORTED";
                    break;
                default:
                    reason = "UNKNOWN(" + errorCode + ")";
                    break;
            }
            Log.e(TAG, "Advertising failed: " + reason);

            if (!advertisingRequested) {
                return;
            }

            if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                return;
            }

            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE && !useReducedAdvertiseData) {
                useReducedAdvertiseData = true;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (advertisingRequested && gattServicesReady) {
                            startAdvertisingInternal();
                        }
                    }
                });
                return;
            }

            if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                    || errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (advertisingRequested && gattServicesReady) {
                            startAdvertisingInternal();
                        }
                    }
                }, 500);
            }
        }
    };

    /**
     * Obtains connected Bluetooth devices
     *
     * @return the connected Bluetooth devices
     */
    private Set<BluetoothDevice> getDevices() {
        final Set<BluetoothDevice> deviceSet = new HashSet<>();
        synchronized (bluetoothDevicesMap) {
            deviceSet.addAll(bluetoothDevicesMap.values());
        }
        return Collections.unmodifiableSet(deviceSet);
    }

    /**
     * Callback for BLE data transfer
     */
    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status, final int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(TAG, "onConnectionStateChange status: " + status + ", newState: " + newState);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    // check bond status
                    Log.d(TAG, "BluetoothProfile.STATE_CONNECTED bondState: " + device.getBondState());
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(final Context context, final Intent intent) {
                                final String action = intent.getAction();
                                Log.d(TAG, "onReceive action: " + action);

                                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                                    if (state == BluetoothDevice.BOND_BONDED) {
                                        // successfully bonded
                                        context.unregisterReceiver(this);

                                        registerDevice(device);
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (gattServer != null) {
                                                    gattServer.connect(device, true);
                                                }
                                            }
                                        });
                                        Log.d(TAG, "successfully bonded");
                                    }
                                }
                            }
                        };
                        final IntentFilter bondStateFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            applicationContext.registerReceiver(bondStateReceiver, bondStateFilter, Context.RECEIVER_NOT_EXPORTED);
                        } else {
                            applicationContext.registerReceiver(bondStateReceiver, bondStateFilter);
                        }

                        // create bond
                        try {
                            device.setPairingConfirmation(true);
                        } catch (final SecurityException e) {
                            Log.d(TAG, e.getMessage(), e);
                        }
                        device.createBond();
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        registerDevice(device);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (gattServer != null) {
                                    gattServer.connect(device, true);
                                }
                            }
                        });
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                        // Stay connected while pairing UI is shown; register once bonded via receiver below if needed
                        registerDevice(device);
                    }

                    notifyConnectionState(device, status, newState);
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    final String deviceAddress = device.getAddress();

                    // try reconnect immediately
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (gattServer != null) {
                                // gattServer.cancelConnection(device);
                                gattServer.connect(device, true);
                            }
                        }
                    });
                    
                    synchronized (bluetoothDevicesMap) {
                        bluetoothDevicesMap.remove(deviceAddress);
                    }
                    notifyConnectionState(device, status, newState);
                    break;

                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            if (gattServer == null) {
                return;
            }
            Log.d(TAG, "onCharacteristicReadRequest characteristic: " + characteristic.getUuid() + ", offset: " + offset);

            handler.post(new Runnable() {
                @Override
                public void run() {

                    final UUID characteristicUuid = characteristic.getUuid();
                    if (BleUuidUtils.matches(CHARACTERISTIC_HID_INFORMATION, characteristicUuid)) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, RESPONSE_HID_INFORMATION);
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_REPORT_MAP, characteristicUuid)) {
                        if (offset == 0) {
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, getReportMap());
                        } else {
                            final int remainLength = getReportMap().length - offset;
                            if (remainLength > 0) {
                                final byte[] data = new byte[remainLength];
                                System.arraycopy(getReportMap(), offset, data, 0, remainLength);
                                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, data);
                            } else {
                                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
                            }
                        }
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_PROTOCOL_MODE, characteristicUuid)) {
                        byte[] value = characteristic.getValue();
                        if (value == null || value.length == 0) {
                            value = new byte[]{0x01}; // Report Protocol
                        }
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_HID_CONTROL_POINT, characteristicUuid)) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte []{0});
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_REPORT, characteristicUuid)) {
                        byte[] value = characteristic.getValue();
                        if (value == null) {
                            value = EMPTY_BYTES;
                        }
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_MANUFACTURER_NAME, characteristicUuid)) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, manufacturer.getBytes(StandardCharsets.UTF_8));
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_SERIAL_NUMBER, characteristicUuid)) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, serialNumber.getBytes(StandardCharsets.UTF_8));
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_MODEL_NUMBER, characteristicUuid)) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, deviceName.getBytes(StandardCharsets.UTF_8));
                    } else if (BleUuidUtils.matches(CHARACTERISTIC_BATTERY_LEVEL, characteristicUuid)) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[] {0x64}); // always 100%
                    } else {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
                    }
                }
            });
        }

        @Override
        public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "onDescriptorReadRequest requestId: " + requestId + ", offset: " + offset + ", descriptor: " + descriptor.getUuid());

            if (gattServer == null) {
                return;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (BleUuidUtils.matches(DESCRIPTOR_REPORT_REFERENCE, descriptor.getUuid())) {
                        final int characteristicProperties = descriptor.getCharacteristic().getProperties();
                        if (characteristicProperties == (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                            // Input Report
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{0, 1});
                        } else if (characteristicProperties == (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
                            // Output Report
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{0, 2});
                        } else if (characteristicProperties == (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                            // Feature Report
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{0, 3});
                        } else {
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, EMPTY_BYTES);
                        }
                    } else if (BleUuidUtils.matches(DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION, descriptor.getUuid())) {
                        byte[] value = descriptor.getValue();
                        if (value == null) {
                            value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                        }
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    } else {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId, final BluetoothGattCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onCharacteristicWriteRequest characteristic: " + characteristic.getUuid() + ", value: " + Arrays.toString(value));

            if (gattServer == null) {
                return;
            }

            if (BleUuidUtils.matches(CHARACTERISTIC_REPORT, characteristic.getUuid())) {
                if (characteristic.getProperties() == (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
                    // Output Report
                    onOutputReport(value);
                }
            } else if (BleUuidUtils.matches(CHARACTERISTIC_PROTOCOL_MODE, characteristic.getUuid())) {
                characteristic.setValue(value);
                Log.d(TAG, "Protocol Mode updated: " + Arrays.toString(value));
            } else if (BleUuidUtils.matches(CHARACTERISTIC_HID_CONTROL_POINT, characteristic.getUuid())) {
                Log.d(TAG, "HID Control Point: " + Arrays.toString(value));
            }

            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, EMPTY_BYTES);
            }
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, final int requestId, final BluetoothGattDescriptor descriptor, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onDescriptorWriteRequest descriptor: " + descriptor.getUuid() + ", value: " + Arrays.toString(value) + ", responseNeeded: " + responseNeeded + ", preparedWrite: " + preparedWrite);

            descriptor.setValue(value);

            if (BleUuidUtils.matches(DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION, descriptor.getUuid())) {
                if (value != null && value.length >= 2
                        && (value[0] & 0x01) == 0x01) {
                    // notifications (and possibly indications) enabled — Windows is ready for input reports
                    registerDevice(device);
                }
            }

            if (responseNeeded) {
                if (gattServer != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, EMPTY_BYTES);
                }
            }
        }

        @Override
        public void onServiceAdded(final int status, final BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d(TAG, "onServiceAdded status: " + status + ", service: " + service.getUuid());

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServiceAdded Adding Service failed, will retry.");
                final BluetoothGattService failed = gattServiceBeingAdded != null ? gattServiceBeingAdded : service;
                gattServiceBeingAdded = null;
                pendingGattServices.offerFirst(failed);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        addNextGattService();
                    }
                }, 100);
                return;
            }

            gattServiceBeingAdded = null;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    addNextGattService();
                }
            });
        }
    };

    /**
     * Set the manufacturer name
     *
     * @param newManufacturer the name
     */
    public final void setManufacturer(@NonNull final String newManufacturer) {
        // length check
        final byte[] manufacturerBytes = newManufacturer.getBytes(StandardCharsets.UTF_8);
        if (manufacturerBytes.length > DEVICE_INFO_MAX_LENGTH) {
            // shorten
            final byte[] bytes = new byte[DEVICE_INFO_MAX_LENGTH];
            System.arraycopy(manufacturerBytes, 0, bytes, 0, DEVICE_INFO_MAX_LENGTH);
            manufacturer = new String(bytes, StandardCharsets.UTF_8);
        } else {
            manufacturer = newManufacturer;
        }
    }

    /**
     * Set the device name
     *
     * @param newDeviceName the name
     */
    public final void setDeviceName(@NonNull final String newDeviceName) {
        // length check
        final byte[] deviceNameBytes = newDeviceName.getBytes(StandardCharsets.UTF_8);
        if (deviceNameBytes.length > DEVICE_INFO_MAX_LENGTH) {
            // shorten
            final byte[] bytes = new byte[DEVICE_INFO_MAX_LENGTH];
            System.arraycopy(deviceNameBytes, 0, bytes, 0, DEVICE_INFO_MAX_LENGTH);
            deviceName = new String(bytes, StandardCharsets.UTF_8);
        } else {
            deviceName = newDeviceName;
        }
    }

    /**
     * Set the serial number
     *
     * @param newSerialNumber the number
     */
    public final void setSerialNumber(@NonNull final String newSerialNumber) {
        // length check
        final byte[] deviceNameBytes = newSerialNumber.getBytes(StandardCharsets.UTF_8);
        if (deviceNameBytes.length > DEVICE_INFO_MAX_LENGTH) {
            // shorten
            final byte[] bytes = new byte[DEVICE_INFO_MAX_LENGTH];
            System.arraycopy(deviceNameBytes, 0, bytes, 0, DEVICE_INFO_MAX_LENGTH);
            serialNumber = new String(bytes, StandardCharsets.UTF_8);
        } else {
            serialNumber = newSerialNumber;
        }
    }
}
