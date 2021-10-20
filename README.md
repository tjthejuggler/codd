BLE MIDI for Android
====================
[![Build Status](https://travis-ci.com/kshoji/BLE-MIDI-for-Android.svg?branch=develop)](https://travis-ci.com/kshoji/BLE-MIDI-for-Android)

MIDI over Bluetooth LE library for Android `API Level 18`(4.3, JellyBean) or later

- Protocol compatible with [Apple Bluetooth Low Energy MIDI Specification](https://developer.apple.com/bluetooth/Apple-Bluetooth-Low-Energy-MIDI-Specification.pdf).
    - The app can be connected with iOS 8 / OS X Yosemite MIDI apps, and BLE MIDI devices.
- BLE Central function
    - `Central` means `BLE MIDI Device's client`.
- BLE Peripheral function
    - `Peripheral` means `BLE MIDI Device`.

Requirements
------------

- BLE Central function needs:
    - Bluetooth LE(4.0) support
    - `API Level 18`(4.3, JellyBean) or above
        - Bluetooth Pairing function needs `API Level 19`(4.4, KitKat) or above
- BLE Peripheral function needs:
    - Bluetooth LE(4.0) support
    - Bluetooth LE Peripheral support(Nexus 5 with custom ROM, Nexus 6, Nexus 9, etc.)
    - `API Level 21`(5.0, Lollipop) or above

Repository Overview
-------------------

- Library Project: `library`
- Sample Project: `sample`
    - Includes `BleMidiCentralActivity`, and `BleMidiPeripheralActivity` examples.

Usage of the library
--------------------

For the detail, see the [wiki](https://github.com/kshoji/BLE-MIDI-for-Android/wiki).

LICENSE
=======
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

TODO
====

dark color scheme
design desired layout
scrollable run history
  detect run stops

bug
  once it was counting a ball as 3 catches every time caught, cant reproduce it

figure out how to:
get ball color
show catch history as ball colors

eventual layout
big intensity consistency number
shown and vocal siteswap announcer


remove clutter
get rid of things im not using one by one and see what breaks it


change the 2 ball in a row rule to an amount of time in between same ball catches
keep both methods now for checking
make site swap identifier
it could even have an audio feature that tells you what site swap you are doing

detect when juggling stops and make a new entry and save the site swap(eventually) with it


Eventually
use machine learning on the catch values to predict drops
maybe it should take the patter/site swap into account, maybe just the values leading up to the drop

figure out tv remote sleep setting

RGBRGBRGBRGB 3
RRGGBBRRGGBB 51
rgbbrggbrrgbbrgg 441

Music
use camera software to skeletal track and change instruments based on location as well as catches

