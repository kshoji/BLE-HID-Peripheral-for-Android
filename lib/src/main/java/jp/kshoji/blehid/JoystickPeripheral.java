package jp.kshoji.blehid;

import android.content.Context;

/**
 * BLE Joystick
 * 
 * @author K.Shoji
 */
public class JoystickPeripheral extends HidPeripheral {
    /**
     * Characteristic Data(Report Map)
     */
    private static final byte[] REPORT_MAP = {
            USAGE_PAGE(1),      0x01,         // Generic Desktop
            USAGE(1),           0x04,         // Joystick
            COLLECTION(1),      0x01,         // Application
            COLLECTION(1),      0x00,         //  Physical
            USAGE_PAGE(1),      0x09,         //   Buttons
            USAGE_MINIMUM(1),   0x01,
            USAGE_MAXIMUM(1),   0x03,
            LOGICAL_MINIMUM(1), 0x00,
            LOGICAL_MAXIMUM(1), 0x01,
            REPORT_COUNT(1),    0x03,         //   2 bits (Buttons)
            REPORT_SIZE(1),     0x01,
            INPUT(1),           0x02,         //   Data, Variable, Absolute
            REPORT_COUNT(1),    0x01,         //   6 bits (Padding)
            REPORT_SIZE(1),     0x05,
            INPUT(1),           0x01,         //   Constant
            USAGE_PAGE(1),      0x01,         //   Generic Desktop
            USAGE(1),           0x30,         //   X
            USAGE(1),           0x31,         //   Y
            USAGE(1),           0x32,         //   Z
            USAGE(1),           0x33,         //   Rx
            LOGICAL_MINIMUM(1), (byte) 0x81,         //   -127
            LOGICAL_MAXIMUM(1), 0x7f,         //   127
            REPORT_SIZE(1),     0x08,         //   Three bytes
            REPORT_COUNT(1),    0x04,
            INPUT(1),           0x02,         //   Data, Variable, Absolute (unlike mouse)
            END_COLLECTION(0),
            END_COLLECTION(0),
    };

    /**
     * Constructor<br />
     * Before constructing the instance, check the Bluetooth availability.
     *
     * @param context the applicationContext
     */
    public JoystickPeripheral(final Context context) throws UnsupportedOperationException {
        super(context.getApplicationContext(), true, false, false, 10);
    }

    @Override
    protected byte[] getReportMap() {
        return REPORT_MAP;
    }

    /**
     * Move the joystick pointer
     *
     * @param dx delta X (-127 .. +127)
     * @param dy delta Y (-127 .. +127)
     * @param dz delta Z (-127 .. +127)
     * @param leftButton true : button down
     * @param rightButton true : button down
     * @param middleButton true : button down
     */
    public void movePointer(int dx, int dy, int dz, final boolean leftButton, final boolean rightButton, final boolean middleButton) {
        if (dx > 127) dx = 127;
        if (dx < -127) dx = -127;
        if (dy > 127) dy = 127;
        if (dy < -127) dy = -127;
        if (dz > 127) dz = 127;
        if (dz < -127) dz = -127;
        byte button = 0;
        if (leftButton) {
            button |= 1;
        }
        if (rightButton) {
            button |= 2;
        }
        if (middleButton) {
            button |= 4;
        }

        final byte[] report = new byte[5];
        report[0] = (byte) (button & 7);
        report[1] = (byte) dx;
        report[2] = (byte) dy;
        report[3] = (byte) dz;

        addInputReport(report);
    }

    @Override
    protected void onOutputReport(byte[] outputReport) {
        // do nothing
    }
}
