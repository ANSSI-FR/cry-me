#!/bin/bash

ANDROID_SDK_ROOT=../cryme_app/element/matrix-sdk-android/
ANDROID_EMULATOR=$ANDROID_SDK_ROOT/emulator/emulator
ANDROID_AVD_HOME=emulator_assets/emulator_images

if [ -z "$1" ]; then
	echo "Error: please provide as first argument the user number to use!"
	exit
fi

if [ ! -z "$2" ]; then
	if [ "$2" != "noyubi" ]; then
		echo "Error: second argument must be 'noyubi'!"
		exit
	fi
	NOYUBI="noyubi"
	echo "[+] Launching the emulator WITHOUT the associated Yubikey (as forced by the command line)"
fi

## Check that the SDK is there, warn the user if not
if [ ! -f "$ANDROID_EMULATOR" ]; then
	echo "Error: $ANDROID_EMULATOR does not exist ... Please check that you indeed fetched and untared the SDK!"
	echo "   ==> Go to the cryme_app folder and execute 'make sdk_untar', and execute the current script again"
	exit
fi

if [ "$1" ] && [ -z "${1//[0-9]}" ]; then
	echo "[+] Launching image for user $1"
else
	echo "Error: expecting a number (the CRY.ME user number) as argument!"
	exit
fi


EMULATOR_IMG_NAME=CRYME$1

## Check that emulator images exist
if [ ! -d "$ANDROID_AVD_HOME/$EMULATOR_IMG_NAME.avd" ]; then
	echo "Error: $ANDROID_AVD_HOME/$EMULATOR_IMG_NAME.avd does not exist! Please create emulator images before launching this script!"
	exit
fi


### Handle the Yubikeys, or not is asked to
if [ -z $NOYUBI ]; then
	# Check that ykman is installed
	if ! command -v ykman &> /dev/null
	then
		echo "Error: yubikey-manager is not found on your system. Please install it!"
		exit
	fi

	## Restart the PCSCd deamon if it was stopped
	sudo /etc/init.d/pcscd restart

	## Get all the serials
	YUBISERIALS=(`ykman list --serials`)
	USERYUBISERIAL=(`cat $ANDROID_AVD_HOME/$EMULATOR_IMG_NAME.yubikey`)
	## Get the one corresponding serial
	for e in "${!YUBISERIALS[@]}"; do
		if [[ "${YUBISERIALS[$e]}" = "$USERYUBISERIAL" ]]; then
			i=${e};
		fi
	done

	if [ -z "$i" ]; then
		echo "Error: user $1 associated Yubikey (serial $USERYUBISERIAL) cannot be found! Please plug it!"
		exit
	fi

	## Then get the bus and id numbers (sorted so that we can associate
	## them with the Yubikeys serials)
	SORTEDYUBI=$(python3 identify_yubikeys_usb_bus.py)
	if [ -z "$SORTEDYUBI" ]; then
		echo "Error: internal error when detecting the Yubikeys ..."
		exit
        fi

	BUS=(`echo "$SORTEDYUBI" | cut -f2 -d ' ' | sed 's/^0*//'`)
	DEV=(`echo "$SORTEDYUBI" | cut -f4 -d ' ' | sed 's/://g' | sed 's/^0*//'`)
	## Get the BUS/ID number corresponding to our user
	BUS_USER=${BUS[$i]}
	DEV_USER=${DEV[$i]}

	if [ -z "$BUS_USER" ]; then
		echo "Error: internal error when detecting the Yubikeys ..."
		exit
	fi
	if [ -z "$DEV_USER" ]; then
		echo "Error: internal error when detecting the Yubikeys ..."
		exit
	fi

	echo "[+] Using Yubikey of serial $USERYUBISERIAL, USB BUS $BUS_USER / DEV $DEV_USER"

	## Stop the PCSCd deamon as it inteferes with
	## the Yubikeys passthrough
	sudo /etc/init.d/pcscd stop
fi

## Launch the emulator with the parameters
ANDROID_EMULATOR_KERNEL=emulator_assets/bzImage_glodfish_CRYME
## Activate this for more verbosity
#EMULATOR_VERBOSITY=-show-kernel -verbose
if [ -z $NOYUBI ]; then
	# Yubikey passthrough
	ANDROID_AVD_HOME=$ANDROID_AVD_HOME ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL=2 $ANDROID_EMULATOR -avd $EMULATOR_IMG_NAME -writable-system -no-cache -no-snapshot -no-snapshot-load -memory 2048 -kernel $ANDROID_EMULATOR_KERNEL $EMULATOR_VERBOSITY -qemu -usb -device usb-host,hostbus=$BUS_USER,hostaddr=$DEV_USER
else
	# NO Yubikey passthrough
	ANDROID_AVD_HOME=$ANDROID_AVD_HOME ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL=2 $ANDROID_EMULATOR -avd $EMULATOR_IMG_NAME -writable-system -no-cache -no-snapshot -no-snapshot-load -memory 2048 -kernel $ANDROID_EMULATOR_KERNEL $EMULATOR_VERBOSITY
fi
