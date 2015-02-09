#!/bin/bash
vagrant ssh-config | ssh -F /dev/stdin default 'source /home/vagrant/.bash_profile;cd /vagrant;./gradlew clean build'