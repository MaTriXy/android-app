#!/bin/sh

adb shell su -c 'cp /data/data/com.blinkboxbooks.android/databases/bbb.db /data/local/tmp/bbb.db && chmod 777 /data/local/tmp/bbb.db'
adb pull /data/local/tmp/bbb.db
sqlite3 -cmd .dump bbb.db 

