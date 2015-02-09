#!/bin/sh
echo 'Running Monkey checking'
$ANDROID_HOME/platform-tools/adb shell monkey -p com.blinkboxbooks.android -v 500

$ANDROID_HOME/platform-tools/adb uninstall com.blinkboxbooks.android