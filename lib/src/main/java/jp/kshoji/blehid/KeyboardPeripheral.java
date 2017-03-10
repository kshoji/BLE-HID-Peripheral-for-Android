package jp.kshoji.blehid;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;

/**
 * BLE Keyboard (US layout)
 *
 * @author K.Shoji
 */
public final class KeyboardPeripheral extends HidPeripheral {
    private static final String TAG = KeyboardPeripheral.class.getSimpleName();

    public static final int MODIFIER_KEY_NONE = 0;
    public static final int MODIFIER_KEY_CTRL = 1;
    public static final int MODIFIER_KEY_SHIFT = 2;
    public static final int MODIFIER_KEY_ALT = 4;

    public static final int KEY_F1 = 0x3a;
    public static final int KEY_F2 = 0x3b;
    public static final int KEY_F3 = 0x3c;
    public static final int KEY_F4 = 0x3d;
    public static final int KEY_F5 = 0x3e;
    public static final int KEY_F6 = 0x3f;
    public static final int KEY_F7 = 0x40;
    public static final int KEY_F8 = 0x41;
    public static final int KEY_F9 = 0x42;
    public static final int KEY_F10 = 0x43;
    public static final int KEY_F11 = 0x44;
    public static final int KEY_F12 = 0x45;

    public static final int KEY_PRINT_SCREEN = 0x46;
    public static final int KEY_SCROLL_LOCK = 0x47;
    public static final int KEY_CAPS_LOCK = 0x39;
    public static final int KEY_NUM_LOCK = 0x53;
    public static final int KEY_INSERT = 0x49;
    public static final int KEY_HOME = 0x4a;
    public static final int KEY_PAGE_UP = 0x4b;
    public static final int KEY_PAGE_DOWN = 0x4e;

    public static final int KEY_RIGHT_ARROW = 0x4f;
    public static final int KEY_LEFT_ARROW = 0x50;
    public static final int KEY_DOWN_ARROW = 0x51;
    public static final int KEY_UP_ARROW = 0x52;

    /**
     * Modifier code for US Keyboard
     * 
     * @param aChar String contains one character
     * @return modifier code
     */
    public static byte modifier(final String aChar) {
        switch (aChar) {
            case "A":
            case "B":
            case "C":
            case "D":
            case "E":
            case "F":
            case "G":
            case "H":
            case "I":
            case "J":
            case "K":
            case "L":
            case "M":
            case "N":
            case "O":
            case "P":
            case "Q":
            case "R":
            case "S":
            case "T":
            case "U":
            case "V":
            case "W":
            case "X":
            case "Y":
            case "Z":
            case "!":
            case "@":
            case "#":
            case "$":
            case "%":
            case "^":
            case "&":
            case "*":
            case "(":
            case ")":
            case "_":
            case "+":
            case "{":
            case "}":
            case "|":
            case ":":
            case "\"":
            case "~":
            case "<":
            case ">":
            case "?":
                return MODIFIER_KEY_SHIFT;
            default:
                return 0;
        }
    }

    /**
     * Key code for US Keyboard
     * 
     * @param aChar String contains one character
     * @return keyCode
     */
    public static byte keyCode(final String aChar) {
        switch (aChar) {
            case "A":
            case "a":
                return 0x04;
            case "B":
            case "b":
                return 0x05;
            case "C":
            case "c":
                return 0x06;
            case "D":
            case "d":
                return 0x07;
            case "E":
            case "e":
                return 0x08;
            case "F":
            case "f":
                return 0x09;
            case "G":
            case "g":
                return 0x0a;
            case "H":
            case "h":
                return 0x0b;
            case "I":
            case "i":
                return 0x0c;
            case "J":
            case "j":
                return 0x0d;
            case "K":
            case "k":
                return 0x0e;
            case "L":
            case "l":
                return 0x0f;
            case "M":
            case "m":
                return 0x10;
            case "N":
            case "n":
                return 0x11;
            case "O":
            case "o":
                return 0x12;
            case "P":
            case "p":
                return 0x13;
            case "Q":
            case "q":
                return 0x14;
            case "R":
            case "r":
                return 0x15;
            case "S":
            case "s":
                return 0x16;
            case "T":
            case "t":
                return 0x17;
            case "U":
            case "u":
                return 0x18;
            case "V":
            case "v":
                return 0x19;
            case "W":
            case "w":
                return 0x1a;
            case "X":
            case "x":
                return 0x1b;
            case "Y":
            case "y":
                return 0x1c;
            case "Z":
            case "z":
                return 0x1d;
            case "!":
            case "1":
                return 0x1e;
            case "@":
            case "2":
                return 0x1f;
            case "#":
            case "3":
                return 0x20;
            case "$":
            case "4":
                return 0x21;
            case "%":
            case "5":
                return 0x22;
            case "^":
            case "6":
                return 0x23;
            case "&":
            case "7":
                return 0x24;
            case "*":
            case "8":
                return 0x25;
            case "(":
            case "9":
                return 0x26;
            case ")":
            case "0":
                return 0x27;
            case "\n": // LF
                return 0x28;
            case "\b": // BS
                return 0x2a;
            case "\t": // TAB
                return 0x2b;
            case " ":
                return 0x2c;
            case "_":
            case "-":
                return 0x2d;
            case "+":
            case "=":
                return 0x2e;
            case "{":
            case "[":
                return 0x2f;
            case "}":
            case "]":
                return 0x30;
            case "|":
            case "\\":
                return 0x31;
            case ":":
            case ";":
                return 0x33;
            case "\"":
            case "'":
                return 0x34;
            case "~":
            case "`":
                return 0x35;
            case "<":
            case ",":
                return 0x36;
            case ">":
            case ".":
                return 0x37;
            case "?":
            case "/":
                return 0x38;
            default:
                return 0;
        }
    }
    
    /**
     * Characteristic Data(Report Map)
     */
    private static final byte[] REPORT_MAP = {
            USAGE_PAGE(1),      0x01,       // Generic Desktop Ctrls
            USAGE(1),           0x06,       // Keyboard
            COLLECTION(1),      0x01,       // Application
            USAGE_PAGE(1),      0x07,       //   Kbrd/Keypad
            USAGE_MINIMUM(1), (byte) 0xE0,
            USAGE_MAXIMUM(1), (byte) 0xE7,
            LOGICAL_MINIMUM(1), 0x00,
            LOGICAL_MAXIMUM(1), 0x01,
            REPORT_SIZE(1),     0x01,       //   1 byte (Modifier)
            REPORT_COUNT(1),    0x08,
            INPUT(1),           0x02,       //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position
            REPORT_COUNT(1),    0x01,       //   1 byte (Reserved)
            REPORT_SIZE(1),     0x08,
            INPUT(1),           0x01,       //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
            REPORT_COUNT(1),    0x05,       //   5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
            REPORT_SIZE(1),     0x01,
            USAGE_PAGE(1),      0x08,       //   LEDs
            USAGE_MINIMUM(1),   0x01,       //   Num Lock
            USAGE_MAXIMUM(1),   0x05,       //   Kana
            OUTPUT(1),          0x02,       //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
            REPORT_COUNT(1),    0x01,       //   3 bits (Padding)
            REPORT_SIZE(1),     0x03,
            OUTPUT(1),          0x01,       //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
            REPORT_COUNT(1),    0x06,       //   6 bytes (Keys)
            REPORT_SIZE(1),     0x08,
            LOGICAL_MINIMUM(1), 0x00,
            LOGICAL_MAXIMUM(1), 0x65,       //   101 keys
            USAGE_PAGE(1),      0x07,       //   Kbrd/Keypad
            USAGE_MINIMUM(1),   0x00,
            USAGE_MAXIMUM(1),   0x65,
            INPUT(1),           0x00,       //   Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
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
    public KeyboardPeripheral(final Context context) throws UnsupportedOperationException {
        super(context.getApplicationContext(), true, true, false, 20);
    }
    
    private static final int KEY_PACKET_MODIFIER_KEY_INDEX = 0;
    private static final int KEY_PACKET_KEY_INDEX = 2;

    /**
     * Send text to Central device
     * @param text the text to send
     */
    public void sendKeys(final String text) {
        String lastKey = null;
        for (int i = 0; i < text.length(); i++) {
            final String key = text.substring(i, i + 1);
            final byte[] report = new byte[8];
            report[KEY_PACKET_MODIFIER_KEY_INDEX] = modifier(key);
            report[KEY_PACKET_KEY_INDEX] = keyCode(key);

            if (key.equals(lastKey)) {
                sendKeyUp();
            }
            addInputReport(report);
            lastKey = key;
        }
        sendKeyUp();
    }

    /**
     * Send Key Down Event
     * @param modifier modifier key
     * @param keyCode key code
     */
    public void sendKeyDown(final byte modifier, final byte keyCode) {
        final byte[] report = new byte[8];
        report[KEY_PACKET_MODIFIER_KEY_INDEX] = modifier;
        report[KEY_PACKET_KEY_INDEX] = keyCode;
        
        addInputReport(report);
    }

    private static final byte[] EMPTY_REPORT = new byte[8];

    /**
     * Send Key Up Event
     */
    public void sendKeyUp() {
        addInputReport(EMPTY_REPORT);
    }

    @Override
    protected void onOutputReport(final byte[] outputReport) {
        Log.i(TAG, "onOutputReport data: " + Arrays.toString(outputReport));
    }
}
