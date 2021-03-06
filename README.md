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
show a catches per minute

make another time time consistency that is based on last x catches as well as the current one that we have now that is based on the whole pattern

make something that checks to see if multiple identical catches are being submitted

bug: two same balls used at once and it crashes

new catch listing system
  it should show time since last catch and intensity at a glance


consistency considerations
  should we try to stay as close to the average timing for each ball, or should it be based on first n catches

make a intensity consistency rating
  first make sure that catch feeling translates well to the number given
  the process should be pretty similar to 
make an overall consistency rating

make a 2d array that holds the info from every catch, including the time
on every catch look back at the last few catches and if there are any that are identical or very similar, remove them from the history
use this 2d array for catches
make sure to remove them from the listview

siteswap:
  keep a history of the order in which balls were caught
  once we get to about 20 catches
    convert every catch to a number based on how far away it is from the next time it is caught
    take this sequence of numbers and get the smallest chunk
    convert this chunk into its highest form
    keep track of this siteswap and once we have had the same siteswap for X number of times, show it in the app

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
maybe it should take the pattern/site swap into account, maybe just the values leading up to the drop
it should probably also take the space between catches into account as well

figure out tv remote sleep setting

RGBRGBRGBRGB 3
RRGGBBRRGGBB 51
rgbbrggbrrgbbrgg 441

Music
use camera software to skeletal track and change instruments based on location as well as catches
collisions could be a different midi signal, or they could be ignored(maybe this is good for gathers)

