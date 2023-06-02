ifeq ($(FORCE_REBUILD),1)
NOCACHE=--no-cache
RECREATE=--force-recreate
else
NOCACHE=
RECREATE=
endif
ifeq ($(VERBOSE),1)
BUILDKIT_PROGRESS=plain
endif

sdk_download:
	@# Download the SDK if necessary
	@if [ -f element/matrix-sdk-android/matrix-sdk.tar.gz ]; then echo "[+] SDK already donwloaded. Use 'make force_sdk_download' if you want to force the download!"; fi
	@if [ ! -f element/matrix-sdk-android/matrix-sdk.tar.gz ]; then bash download_sdk.sh element/matrix-sdk-android/matrix-sdk.tar.gz; fi

force_sdk_download:
	@bash download_sdk.sh element/matrix-sdk-android/matrix-sdk.tar.gz;

sdk_clean:
	@echo "[+] Cleaning the SDK"
	@cd element/matrix-sdk-android/ && rm -rf build-tools/ cmake/ emulator/ fonts/ licenses/ ndk/ patcher/ platforms/ platform-tools/ skins/ sources/ system-images/ tools/
	@rm -rf element/matrix-sdk-android/matrix-sdk.tar.gz

force_sdk_untar: sdk_download
	@echo "[+] Untaring the SDK (FORCED) ... Please wait!"
	@cd element/matrix-sdk-android/ && (tar xf matrix-sdk.tar.gz 2>/dev/null)

sdk_untar: sdk_download
	@echo "[+] Untaring the SDK ... Please wait!"
	@if [ ! -d "element/matrix-sdk-android/ndk" ]; then cd element/matrix-sdk-android/ && (tar xf matrix-sdk.tar.gz 2>/dev/null); fi

create_local_properties: sdk_untar
	@cd element && echo "sdk.dir="`pwd`"/matrix-sdk-android/" > local.properties
	@cd element && echo "ndk.dir="`pwd`"/matrix-sdk-android/ndk/25.2.9519653/" >> local.properties
	@cd element && cp local.properties yubikit-android/
	@cd element && cp local.properties yubikit-android/AndroidDemo/
	# Clean stuff
	@find ./ -iname "__pycache__" -type d | xargs -0 rm -rf

yubikit_build: create_local_properties
	@mkdir -p element/vector/yubikit_aar/
	cd element/yubikit-android/ && make yubikit_build
	cp element/yubikit-android/core/build/libs/core-2.0.1-SNAPSHOT.jar element/vector/yubikit_aar/core-2.0.1.jar
	cp element/yubikit-android/piv/build/libs/piv-2.0.1-SNAPSHOT.jar element/vector/yubikit_aar/piv-2.0.1.jar
ifeq ($(DEBUG),1)
	cp element/yubikit-android/android/build/outputs/aar/android-debug.aar element/vector/yubikit_aar/android-2.0.0.aar
else
	cp element/yubikit-android/android/build/outputs/aar/android-release.aar element/vector/yubikit_aar/android-2.0.0.aar
endif

yubikit_clean: create_local_properties
	cd element/yubikit-android && make yubikit_clean

app_build: yubikit_build
ifeq ($(DEBUG),1)
	cd element && ./gradlew $(GRADLE_OPTIONS) :vector:assembleGplayDebug -x :vector:lint -x :vector:test -x :vector:lintVitalAnalyzeGplayRelease
else
	cd element && ./gradlew $(GRADLE_OPTIONS) :vector:assembleGplay -x :vector:lint -x :vector:test -x :vector:lintVitalAnalyzeGplayRelease
endif
	mkdir -p ./build
	cp -r element/yubikit-android/AndroidDemo/build/outputs/apk/ ./build/yubikit/
	cp -r element/vector/build/outputs/apk/ ./build/cryme/

app_build_docker:
	DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=$(BUILDKIT_PROGRESS) docker build $(NOCACHE) --file Dockerfile.compile --output . .

app_clean: yubikit_clean
	rm -f element/vector/yubikit_aar/android-2.0.0.aar element/vector/yubikit_aar/core-2.0.1.jar element/vector/yubikit_aar/piv-2.0.1.jar
	cd element && ./gradlew $(GRADLE_OPTIONS) clean
	rm -rf build

app_bundle_src:
	@echo "[+] Bundling the source code for vulnerability analysis, please wait ..."
	@python3 bundle_cryme_app.py element/ ../cry.me.src.bundle.tar.gz
	@echo "[+] You will find your source bundle without vulnerabilities comments here: cry.me.src.bundle.tar.gz at the project root folder!"

app_bundle_src_docker:
	DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=$(BUILDKIT_PROGRESS) docker build $(NOCACHE) --file Dockerfile.bundle --output ../ ../

app_install: sdk_untar
	@echo "[+] Installing the CRY.ME app"
	element/matrix-sdk-android/platform-tools/adb install build/cryme/gplay/debug/vector-gplay-universal-debug.apk
