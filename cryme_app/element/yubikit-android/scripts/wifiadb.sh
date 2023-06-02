#! /usr/bin/env bash

#set -x

#adb tcpip 5555

IP=$(adb shell "ip addr show wlan0 | grep -e wlan0$ | cut -d\" \" -f 6 | cut -d/ -f 1")
echo ${IP}

adb connect ${IP}:5555
