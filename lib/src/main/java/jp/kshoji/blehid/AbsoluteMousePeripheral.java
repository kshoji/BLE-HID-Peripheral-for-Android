package jp.kshoji.blehid;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;

/**
 * BLE Mouse(Absolute Position)
 *
 * @author K.Shoji
 */
public final class AbsoluteMousePeripheral extends HidPeripheral {
    /**
     * Characteristic Data(Report Map)
     */
    private static final byte[] REPORT_MAP = {
        // @formatter:off
        USAGE_PAGE(1), 0x01,           // Generic Desktop
        USAGE(1), 0x02,                // Mouse
        COLLECTION(1), 0x01,           // Application
            USAGE(1), 0x01,                // Pointer
            COLLECTION(1), 0x00,           // Physical
                USAGE_PAGE(1), 0x01,            // Generic Desktop
                USAGE(1), 0x30,                 // X
                USAGE(1), 0x31,                 // Y
                LOGICAL_MINIMUM(1), 0x00,       // 0
                LOGICAL_MAXIMUM(2), (byte)0xff, 0x7f, // 32767
                REPORT_SIZE(1), 0x10,
                REPORT_COUNT(1), 0x02,
                INPUT(1), 0x02,                 // Data, Variable, Absolute
                USAGE_PAGE(1), 0x01,            // Generic Desktop
                USAGE(1), 0x38,                 // scroll
                LOGICAL_MINIMUM(1), (byte)0x81,       // -127
                LOGICAL_MAXIMUM(1), 0x7f,       // 127
                REPORT_SIZE(1), 0x08,
                REPORT_COUNT(1), 0x01,
                INPUT(1), 0x06,                 // Data, Variable, Relative
                USAGE_PAGE(1), 0x09,            // Buttons
                USAGE_MINIMUM(1), 0x01,
                USAGE_MAXIMUM(1), 0x03,
                LOGICAL_MINIMUM(1), 0x00,       // 0
                LOGICAL_MAXIMUM(1), 0x01,       // 1
                REPORT_COUNT(1), 0x03,
                REPORT_SIZE(1), 0x01,
                INPUT(1), 0x02,                 // Data, Variable, Absolute
                REPORT_COUNT(1), 0x01,
                REPORT_SIZE(1), 0x05,
                INPUT(1), 0x01,                 // Constant
            END_COLLECTION(0),
        END_COLLECTION(0)
        // @formatter:on
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
    public AbsoluteMousePeripheral(final Context context) throws UnsupportedOperationException {
        super(context.getApplicationContext(), true, false, false, 10);
    }
    
    /**
     * Move the mouse pointer
     * 
     * @param x absolute X (0 .. 32767)
     * @param y absolute Y (0 .. 32767)
     * @param wheel wheel (-127 .. +127)
     * @param leftButton true : button down
     * @param rightButton true : button down
     * @param middleButton true : button down
     */
    public void movePointer(int x, int y, int wheel, final boolean leftButton, final boolean rightButton, final boolean middleButton) {
        if (x > 32767) x = 32767;
        if (x < 0) x = 0;
        if (y > 32767) y = 32767;
        if (y < 0) y = 0;
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

        final byte[] report = new byte[6];
        report[0] = LSB(x);
        report[1] = MSB(x);
        report[2] = LSB(y);
        report[3] = MSB(y);
        report[4] = (byte) wheel;
        report[5] = (byte) (button & 0x07);

        addInputReport(report);
    }

    @Override
    protected void onOutputReport(final byte[] outputReport) {
        // do nothing
    }
}
