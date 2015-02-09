#!/bin/bash

curl 'https://authserver/clients' -H "Authorization:Bearer $access_token" | python -m json.tool

