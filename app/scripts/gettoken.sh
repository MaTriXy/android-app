#!/bin/bash

json=`curl -s 'https://authserver/oauth2/token' -H 'Content-Type:application/x-www-form-urlencoded' -X POST -d 'grant_type=password&username=blinkbox_android_unittests@gmail.com&password=password&client_id=urn:blinkbox:zuul:client:1908&client_secret=secret'`
access_token=`echo $json | python -m json.tool | grep access_token | cut -d \" -f 4`
export access_token
echo $access_token

