#!/bin/bash -x

# Quick helper script for sending a pull request - you will need to change 'twright' to your git user name

MESSAGE=$@
if [ -z $MESSAGE ];
then
    MESSAGE="quick commit"
fi

git add .
git commit -am "$MESSAGE"
git branch -D lazy
git branch lazy
git push twright lazy -f
sleep 3
open https://gitserver/twright/Android-blinkboxbooks/pull/new/lazy

