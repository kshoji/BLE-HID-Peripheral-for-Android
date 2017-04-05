# BLE-HID-Peripheral-for-Android
[![Build Status](https://travis-ci.org/kshoji/BLE-HID-Peripheral-for-Android.svg?branch=master)](https://travis-ci.org/kshoji/BLE-HID-Peripheral-for-Android)

## BLE HID over GATT Profile for Android

This library provides BLE HID Peripheral feature to Android devices. <br/>
Android device will behave as:

- BLE Mouse (relative position / absolute position)
- BLE Keyboard
- BLE Joystick

Tested connection:

- Android(Peripheral) <--> Android(Central)
    - Relative Position Mouse, Keyboard
- Android(Peripheral) <--> OS X(Central)
    - Absolute Position Mouse, Relative Position Mouse, Keyboard

Currently, connection with iOS central is not tested yet.

Requirements
------------

- **API Level 21 or later** and **Bluetooth LE Peripheral feature** will be needed.

Repository Overview
-------------------

- Library Project: `lib`
- Sample Project: `app`

LICENSE
=======
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
