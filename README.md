Google Santa Tracker for Android
================================

## About

[Google Santa Tracker app for Android][play-store] is an educational and entertaining tradition that brings joy to millions of children (and children at heart) across the world over the December holiday period. The app is a companion to the [Google Santa Tracker][santa-web] website ([repository here](https://github.com/google/santa-tracker-web)), showcasing unique platform capabilities like Android Wear watchfaces, device notifications and more.
![Analytics](https://ga-beacon.appspot.com/UA-12846745-20/santa-tracker-android/readme?pixel)

![Village Screenshot](res/village.png) ![Snowdown Screenshot](res/snowdown.png)

## Features

* A beautiful materially designed village
* 6 exciting games
* 2 interactive Android Wear watchfaces (with sound!)
* Videos, animations and more.

## Building the app

First up, Santa Tracker is powered by [Firebase][firebase], so you'll need to enable it
on your Google account over at the [Firebase console][fire-console]. Once you're in the
console, follow these steps:

 * Create a new project
 * Add Firebase to your Android app
  * Package name: `com.google.android.apps.santatracker.debug`
  * Debug signing certificate can be blank, or follow the instructions in the
    tooltip to find yours.
 * Save the google-services.json file to the santa-tracker/ directory

Now you should be able to plug your phone in (or fire up an emulator) and run:

    ./gradlew installDebug

Alternatively, import the source code into Android Studio (File, Import Project).

## License
All image and audio files (including *.png, *.jpg, *.svg, *.mp3, *.wav 
and *.ogg) are licensed under the CC-BY-NC license. All other files are 
licensed under the Apache 2 license. See the LICENSE file for details.


    Copyright 2015 Google Inc. All rights reserved.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[play-store]: https://play.google.com/store/apps/details?id=com.google.android.apps.santatracker
[santa-web]: http://g.co/santatracker
[firebase]: https://firebase.google.com/
[fire-console]: https://firebase.google.com/console/
