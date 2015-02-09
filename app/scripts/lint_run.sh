#!/bin/sh
echo 'Running lint checking'
mkdir build/lint
$ANDROID_HOME/tools/lint --quiet ../ --html build/lint/index.html --classpath build/classes


