package jp.kshoji.blehid;

import android.content.Context;

/**
 * BLE Mouse
 *
 * @author K.Shoji
 */
public final class MousePeripheral extends HidPeripheral {
    /**
     * Characteristic Data(Report Map)
     */
    private static final byte[] REPORT_MAP = {
            USAGE_PAGE(1),      0x01,         // Generic Desktop
            USAGE(1),           0x02,         // Mouse
            COLLECTION(1),      0x01,         // Application
            USAGE(1),           0x01,         //  Pointer
            COLLECTION(1),      0x00,         //  Physical
            USAGE_PAGE(1),      0x09,         //   Buttons
            USAGE_MINIMUM(1),   0x01,
            USAGE_MAXIMUM(1),   0x03,
            LOGICAL_MINIMUM(1), 0x00,
            LOGICAL_MAXIMUM(1), 0x01,
            REPORT_COUNT(1),    0x03,         //   3 bits (Buttons)
            REPORT_SIZE(1),     0x01,
            INPUT(1),           0x02,         //   Data, Variable, Absolute
            REPORT_COUNT(1),    0x01,         //   5 bits (Padding)
            REPORT_SIZE(1),     0x05,
            INPUT(1),           0x01,         //   Constant
            USAGE_PAGE(1),      0x01,         //   Generic Desktop
            USAGE(1),           0x30,         //   X
            USAGE(1),           0x31,         //   Y
            USAGE(1),           0x38,         //   Wheel
            LOGICAL_MINIMUM(1), (byte) 0x81,  //   -127
            LOGICAL_MAXIMUM(1), 0x7f,         //   127
            REPORT_SIZE(1),     0x08,         //   Three bytes
            REPORT_COUNT(1),    0x03,
            INPUT(1),           0x06,         //   Data, Variable, Relative
            END_COLLECTION(0),
            END_COLLECTION(0),
    };

    @Override
    protected byte[] getReportMap() {
        return REPORT_MAP;
    }

    /**
     * Constructor<br />
     * Before constructing the instance, check the Bluetooth availability.
     *
     * @param context the applicationContext
     */
    public MousePeripheral(final Context context) throws UnsupportedOperationException {
        super(context.getApplicationContext(), true, false, false, 10);
    }
    
    private final byte[] lastSent = new byte[4];

    /**
     * Move the mouse pointer
     * 
     * @param dx delta X (-127 .. +127)
     * @param dy delta Y (-127 .. +127)
     * @param wheel wheel (-127 .. +127)
     * @param leftButton true : button down
     * @param rightButton true : button down
     * @param middleButton true : button down
     */
    public void movePointer(int dx, int dy, int wheel, final boolean leftButton, final boolean rightButton, final boolean middleButton) {
        if (dx > 127) dx = 127;
        if (dx < -127) dx = -127;
        if (dy > 127) dy = 127;
        if (dy < -127) dy = -127;
        if (wheel > 127) wheel = 127;
        if (wheel < -127) wheel = -127;
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

        final byte[] report = new byte[4];
        report[0] = (byte) (button & 7);
        report[1] = (byte) dx;
        report[2] = (byte) dy;
        report[3] = (byte) wheel;
        
        if (lastSent[0] == 0 && lastSent[1] == 0 && lastSent[2] == 0 && lastSent[3] == 0 &&
                report[0] == 0 && report[1] == 0 && report[2] == 0 && report[3] == 0) {
            return;
        }
        lastSent[0] = report[0];
        lastSent[1] = report[1];
        lastSent[2] = report[2];
        lastSent[3] = report[3];
        addInputReport(report);
    }

    @Override
    protected void onOutputReport(final byte[] outputReport) {
        // do nothing
    }
}
