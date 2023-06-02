#!/bin/bash

## Folders we will need
SDK_PATH=`realpath ../cryme_app/element/matrix-sdk-android/`
AVD_PATH=`realpath emulator_assets/emulator_images`
EMULATOR_BASE_IMAGE_TARGZ=Nexus6_api29_CRYME.avd.tar.gz
EMULATOR_BASE_IMAGE=emulator_assets/Nexus6_api29_CRYME.avd.tar.gz
ANDROID_QEMU_IMG=$SDK_PATH/emulator/qemu-img

if [ "$1" = "clean" ]; then
	if [ ! -z "$2" ]; then
		# We are asked to clean a specific image
		if [ -z "${2//[0-9]}" ]; then
			echo "[+] Cleaning user $2 image in $AVD_PATH ..."
			if [ ! -d "$AVD_PATH/CRYME$2.avd" ]; then
				echo "Error: image for user $2 does not exist in $AVD_PATH"
				exit
			fi
			rm -rf $AVD_PATH/CRYME$2*
		else
			echo "Error: expecting a number (the CRY.ME user number) as second argument of 'clean'!"
		fi
	else
		echo "[+] Cleaning all the images in $AVD_PATH ..."
		rm -rf $AVD_PATH/
	fi
	exit
fi

if [ "$1" ] && [ -z "${1//[0-9]}" ]; then
	echo "[+] Creating image for user $1"
else
	echo "Error: expecting a number (the CRY.ME user number) as argument!"
	exit
fi

IMG_NAME=CRYME$1

if [ -d "$AVD_PATH/$IMG_NAME.avd" ]; then
	echo "Error: image $IMG_NAME seems to already exist in $AVD_PATH! Please remove it if you wan to create a new one!"
	exit
fi

if [ ! -d "$AVD_PATH" ]; then
	mkdir -p "$AVD_PATH"
fi

###### Yubikeys management
# We are given a serial number to use
if [ ! -z "$2" ]; then
	if [ ! -z "${2//[0-9]}" ]; then
		echo "Error: please provide a serial number for the Yubikey to use as second argument of user $1 creation!"
		exit
	fi
	echo "[+] We are asked to use Yubikey of serial number $2 (forced, NOT checking if the Yubikey is present)"
	YUBI_TO_USE="$2"
else
	# Check that ykman is installed
	if ! command -v ykman &> /dev/null
	then
		echo "Error: yubikey-manager is not found on your system. Please install it!"
		exit
	fi

	if [ -z "$1" ]; then
        	echo "Error: please provide as first argument the user number or 'clean'!"
	        exit
	fi
	## Get the bus numbers
	YUBIKEYS=(`ykman list --serials`)

	## If zero Yubikey is plugged, ask for one
	if [ ${#YUBIKEYS[@]} -eq 0 ]; then
		echo "Error: no Yubikey has been found! Please plug one!"
		exit
	fi
	if [ ${#YUBIKEYS[@]} -gt 1 ]; then
		echo "Error: more than one Yubikey has been found! Please plug only one!"
		exit
	fi
	YUBI_TO_USE=${YUBIKEYS[0]}
	echo "[+] Yubikey found with serial $YUBI_TO_USE"
fi

# Check that this serial is not already used
YUBISERIALS=(`cat $AVD_PATH/CRYME*.yubikey 2> /dev/null`)
for e in "${!YUBISERIALS[@]}"; do
        if [[ "${YUBISERIALS[$e]}" = "$YUBI_TO_USE" ]]; then
                echo "Error: Yubikey with serial $YUBI_TO_USE seems already associated with an existing user. Please use another Yubikey!" && exit;
        fi
done

######
function get_emulator_image {
    URL=$1
    DEST_PATH=$2
    curl $1 -o $2
}
if [ ! -f "$EMULATOR_BASE_IMAGE" ]; then
	echo "[+] Emulator base image not present, downloading it!"
	get_emulator_image https://www.cryptoexperts.com/cry-me/Nexus6_api29_CRYME.avd.tar.gz $EMULATOR_BASE_IMAGE
fi

echo "[+] Copying and untaring the base image ..."
cp $EMULATOR_BASE_IMAGE $AVD_PATH
cd $AVD_PATH && tar xf $EMULATOR_BASE_IMAGE_TARGZ 2>/dev/null && mv Nexus6_api29_CRYME.avd "$IMG_NAME.avd"
cd $AVD_PATH && rm $EMULATOR_BASE_IMAGE_TARGZ

echo "[+] Patching the configuration files ..."
#
cd "$AVD_PATH/$IMG_NAME.avd" && sed -i "s|SDK_PATH|$SDK_PATH|g" hardware-qemu.ini
cd "$AVD_PATH/$IMG_NAME.avd" && sed -i "s|AVD_PATH|$AVD_PATH|g" hardware-qemu.ini
cd "$AVD_PATH/$IMG_NAME.avd" && sed -i "s|IMG_NAME|$IMG_NAME|g" hardware-qemu.ini
#
cd "$AVD_PATH/$IMG_NAME.avd" && sed -i "s|SDK_PATH|$SDK_PATH|g" config.ini
cd "$AVD_PATH/$IMG_NAME.avd" && sed -i "s|AVD_PATH|$AVD_PATH|g" config.ini
cd "$AVD_PATH/$IMG_NAME.avd" && sed -i "s|IMG_NAME|$IMG_NAME|g" config.ini
#
cd "$AVD_PATH/" && echo "vd.ini.encoding=UTF-8" > "$AVD_PATH/$IMG_NAME.ini"
cd "$AVD_PATH/" && echo "path=$AVD_PATH/$IMG_NAME.avd" >> "$AVD_PATH/$IMG_NAME.ini"
cd "$AVD_PATH/" && echo "arget=android-29" >> "$AVD_PATH/$IMG_NAME.ini"
#
cd "$AVD_PATH/" && echo "$YUBI_TO_USE" > "$AVD_PATH/$IMG_NAME.yubikey"
# Create
#
cd "$AVD_PATH/$IMG_NAME.avd" && $ANDROID_QEMU_IMG rebase -b "$SDK_PATH/system-images/android-29/google_apis/x86/system.img" -F raw -u system.img.qcow2
cd "$AVD_PATH/$IMG_NAME.avd" && $ANDROID_QEMU_IMG rebase -b "$SDK_PATH/system-images/android-29/google_apis/x86/vendor.img" -F raw -u vendor.img.qcow2

echo "[+] All should be good for the CRY.ME emulator image $1 ($IMG_NAME), you can launch the emulator now with this user number $1!"
