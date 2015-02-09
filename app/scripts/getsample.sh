#!/bin/bash

curl 'https://qaserver/service/my/library/samples' -H "Authorization:Bearer $access_token" -d "{\"isbn\" : \"$1\"}" -X POST -H 'Content-Type:application/vnd.blinkboxbooks.data.v1+json'

