# CRY.ME - A Flawed Messaging Application for Educational Purposes

## The CRY.ME project

The CRY.ME project consists in a secure messaging application based on the Matrix protocol containing many cryptographic vulnerabilities deliberately introduced for educational purposes. The CRY.ME application has been specified and developped by ANSSI and CryptoExperts to provide a practical security challenge especially targeting cryptography.

The application presents many different classes of vulnerabilities to identify and, whenever possible, to exploit. The scope of the vulnerabilities introduced in the CRY.ME covers many of the classical domains of cryptography.

## Disclaimer

All the code and documentation associated with CRY.ME are **flawed** and have been made specifically in the context of challenging cryptographic competencies.

It **MUST NOT BE USED** in any context other than educational purposes.
We deny any responsability when using CRY.ME as a messaging application, or any
part of the CRY.ME source code, in other contexts.

## Authors

The authors of CRY.ME are from ANSSI and CryptoExperts.

ANSSI:
  * Jérémy JEAN
  * Louiza KHATI
  * Ange MARTINELLI
  * Chrysanthi MAVROMATI

CryptoExperts:
  * Sonia BELAID
  * Ryad BENADJILA
  * Thibauld FENEUIL
  * Matthieu RIVAIN
  * Abdul Rahman TALEB

For questions or remarks regarding CRY.ME, please fill issues on this repository or send an
email to [cryme@ssi.gouv.fr](mailto:cryme@ssi.gouv.fr).

## License

The CRY.ME project is released under the Apache 2.0 license, please check
the [LICENSE](LICENSE) file at the root folder of the project.

CRY.ME heavily relies on source code from the Android element (https://github.com/vector-im/element-android)
project, the Matrix SDK project (https://github.com/matrix-org/matrix-android-sdk2), and the
Android Yubikit (https://github.com/Yubico/yubikit-android) project. These three projects
are under the Apache 2.0 license as well.

## The CRY.ME application

CRY.ME is based on Android element (https://github.com/vector-im/element-android), and it shares
many features with the original messaging application. Please check the element [user guide](https://element.io/user-guide)
for more details on how the application works.

For a basic "getting started" documentation, you can refer to the [brief howto](README_CRYME.md).

## The CRY.ME application specifications

Cryptographic specifications for the CRY.ME application can be found in the `cryme_docs` folder. We provide two documents for the specifications, as well as the security target that can be used for an evaluation to describe the scope to analyze.
The first document `crypto_specs_without_vulns` contains the specifications without any mention to the cryptographic vulnerabilities and should be the one to use by anyone who wants to give the challenge a try.
The second document `crypto_specs_with_vulns` contains the full cryptographic specifications with mentions of all the vulnerabilities voluntarily introduced in the application. It can be seen as the solution to the challenge.

**NOTE**: the documents in `cryme_docs` are in **French**. This is mainly due to CRY.ME being
the result of a French challenge. We might consider translating parts of this documentation
in the future. Anyways, any help on this translation through Pull Requests is welcome!

## The CRY.ME Android source code bundle for security analysis

CRY.ME uses `element` as a basis for the messaging application.
In `cryme_app`, all the source code of the application is present.
Beware that this source code is **annotated** with the vulnerabilities
(using comments with `CRY.ME.VULN.XX`, `XX` being a vulnerability number).

Of course, for someone wanting to explore the vulnerable source code without
any information about the vulnerabilities, these annotations are spoilers.
This is why we provide a dedicated way of bundling this source code without
the annotations. To do so, go to the `cryme_app` folder and execute:

```
cryme_app$ make app_bundle_src
[+] Bundling the source code for vulnerability analysis, please wait ...
[+] You will find your source bundle without vulnerabilities comments here: cry.me.src.bundle.tar.gz at the project root folder!
```

The compressed bundle will be created in `cry.me.src.bundle.tar.gz`: you can uncompress it and explore it.
The bundling script should only have `python3` and `tar` as dependencies, but we also provide a
**Dockerfile** (you will need `docker`):

```
cryme_app$ make app_bundle_src_docker
...
```

## The CRY.ME Android compilation

### Pre-built packages

For those who do not want to recompile the CRY.ME application from scratch, it is possible
to get the `apk` files from the following URL: 

```
curl https://www.cryptoexperts.com/cry-me/cry.me.build.tar.gz -o cry.me.build.tar.gz
```

Once downloaded, you can extract this archive in the `cryme_app` folder, which will create a `build/` subfolder.

The bundle contains `debug` and `release` `apk` both for the CRY.ME application, and for
the Yubikit "demo" in order to test your Yubikey on Android if needed.
Once untared, the CRY.ME `debug` `apk` files are in `build/cryme/gplay/debug/`and the `release` ones in
`build/cryme/gplay/release`.

The `vector-gplay-universal-debug.apk` and `vector-gplay-universal-release-unsigned.apk`
are of particular interest as they are "universal" and should be installable on physical devices
or emulators (both arm and x86 flavours) with **Android API level at least 29**.

Please be aware, however, that the release packages are **not signed**, and hence
not directly installable on target devices (which is not the case of debug packages). Signing `apk`
release packages is out of the scope of the project, please refer to the appropriate Android
resources to achieve this.

### Compilation from scratch

It is possible to compile the CRY.ME project (producing `apk` files for Android) using the
following command:

```
cryme_app$ make app_build
```

This supposes that the following dependencies are installed on your system:
`openjdk-11-jdk-headless`, `make`, `gradle`, `git`, `curl`.
Depending on your system, you may need to adapt your environment variables to launch the build:
```
cryme_app$ JAVA_HOME=/usr/lib/jvm/default-java LD_LIBRARY_PATH=LD_LIBRARY_PATH:$JAVA_HOME/lib/server/ make app_build
```

If you do not want to bother with all these dependencies, a **Docker** version is also available (you will need
`docker`):

```
cryme_app$ make app_build_docker
```

This will create a `cryme_app/build` folder with the various `debug` and `release`
`apk` files.

We have tried to make compilation as easy and transparent as possible, but you might
also want to install `Android Studio` and compile CRY.ME by your own means. If so,
please follow the steps in the [dedicated documentation](README_ANDROID_STUDIO.md).
Using `Android Studio` implies to install a rather heavy software, but allows to use
the **debugger** that can come handy when analyzing the application.

## The CRY.ME server

Due to the CRY.ME server heavy dependencies (mostly for the embedded Synapse server), we
provide a `docker-compose` file to lauch it. Launching an instance of the server is
as easy as going to the `cryme_server` folder and executing (you will need `docker` and
`docker-compose` to be installed on your system):

```
cryme_server$ SYNAPSE_SERVER_NAME=cryme.fr make
...
Starting cryme-db ... done
Starting cryme-synapse ... done
Starting cryme-nginx   ... done
Attaching to cryme-db, cryme-synapse, cryme-nginx
...
```
**NOTE**: this command supposes that your user can manage Docker with non-root user. You might
either follow [this documentation](https://docs.docker.com/engine/install/linux-postinstall/),
or use `sudo` for this `make` command.


The `SYNAPSE_SERVER_NAME` environment variable is used to provide the FQDN name of the server
(needed by Synapse). We use `cryme.fr` in the command above.
If everything goes well, you should see three containers successfully started: `cryme-db`,
`cryme-synapse` and `cryme-nginx`.

Note that we provide two files for `docker-compose`: the default one (`docker-compose.yml`)
used by the above `make` command pulls a public image from DockerHub that has been prebuilt
for a simpler usage. The second one (`docker-compose.build.yml`) can be used in case you want
to rebuild the server yourself. While this is not necessary for using the application, it might
come in handy during a deeper analysis of the application. You can force the local build of
the `cryme-synapse` docker with:

```
cryme_server$ SYNAPSE_SERVER_NAME=cryme.fr make launch_server_build_synapse
```

The users and conversation databases are handled by the `cryme-db` container: they should be
consistent across executions of the server. If you want to purge the database and
get a clean state with empty databases, you can execute:

```
cryme_server$ SYNAPSE_SERVER_NAME=cryme.fr make clean
docker-compose rm cryme-db
Going to remove cryme-db
Are you sure? [yN] y
Removing cryme-db ... done
docker-compose rm cryme-synapse
Going to remove cryme-synapse
Are you sure? [yN] y
Removing cryme-synapse ... done
docker-compose rm cryme-nginx
Going to remove cryme-nginx
Are you sure? [yN] y
Removing cryme-nginx ... done
```

Beware that with this command, you will **loose all the existing users and conversations**!

## The CRY.ME application emulation

### Overview of the emulated platform

In order to make testing CRY.ME easier, we also provide a way to easily test
the application on a single computer. We make use of a server instantiated
on localhost, and as many emulators instances communicating with it as CRY.ME
users you want to emulate. All the sequel have been tested on a **x86_64 Linux environment**
with the following dependencies installed: `docker`, `bash`, `curl`, `swig`, `pcscd`, `libpcsclite-dev`
and `yubikey-manager`.
Although any Android emulator image could theoretically be used, the ones we provide
are specifically suited to test CRY.ME for two reasons: first of all, they use
a custom kernel and system modifications allowing for USB passthough (for communicating with physical Yubikeys),
and secondly some system files have been modified for easy network lookups on
localhost (for communicating with the local server instance).

Since CRY.ME makes use of **Yubikeys** as authentication tokens, you will need
as many Yubikeys as the number of CRY.ME users you wan to emulate.

**NOTE**: emulation of CRY.ME uses images with around 12 GB. Beware of
this rather large size that will grow with the number of users when
creating multiple images!

### Launching the server instance for emulation

First of all, launch an instance of the server using the server name `cryme.fr`
(beware that this name is important to keep as it is embedded in the
emulator images DNS lookup):

```
cryme_server$ SYNAPSE_SERVER_NAME=cryme.fr make
```

You can modify the `SYNAPSE_SERVER_NAME` with the name of another server
instance running on a machine elsewhere in the network/internet, however you
should make sure that this name can be properly resolved by a DNS server (which
should be the case if the URL is indeed accessible, e.g., from a browser).

### Creating the CRY.ME users images for emulation

Then, go to the `cryme_app_emulation` folder and proceed with some
steps. First, we will create as many emulators instances as we have users
to create. For the sake of the example, we take two users (1 and 2) in the
following. This supposes that you have two **distinct** Yubikeys, each one
associated with each user: let's call them Yubikey 1 and Yubikey 2.

Create the CRY.ME user 1 emulator instance. Plug Yubikey 1 and execute the
`create_emulator_image.sh` bash script (be sure that only one Yubikey is
plugged in at a time):

```
cryme_app_emulation$ ./create_emulator_image.sh 1
...
[+] Creating image for user 1
[+] Yubikey found with serial 16146863
[+] Emulator base image not present, downloading it!
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0
100 1991M  100 1991M    0     0   119M      0  0:00:16  0:00:16 --:--:--  119M
[+] Copying and untaring the base image ...
[+] Patching the configuration files ...
[+] All should be good for the CRY.ME emulator image 1 (CRYME1), you can launch the emulator now with this user number 1!
```

Do the same with Yubikey 2 and user 2 (replacing 1 by 2 of course in the
previous command line). This might take a while as the base emulator images
must be fetched and uncompressed. You can also provide as a second argument
the Yubikey serial number to be used for the image to create: in this case,
the script does not check that the Yubikey is plugged in (but beware that the
serial number **must be valid** as during execution il will be checked against
a physical Yubikey):

```
cryme_app_emulation$ ./create_emulator_image.sh 1 18283368
[+] Creating image for user 1
[+] We are asked to use Yubikey of serial number 18283368 (forced, NOT checking if the Yubikey is present)
...
```

All the created images can be cleaned with the same `create_emulator_image.sh` script using
`clean` as a first argument (all the images will be clean without additional argument), and
the image's user number to clean as a second optional argument:

### Launching the emulation images for the users

Then, the instances can be launched using the `launch_emulator_usb.sh` bash script:

```
cryme_app_emulation$ ./launch_emulator_usb.sh 1
...
```

Of course, the Yubikey associated with the user must be plugged in. On the first launch, you
might be asked to fetch and untar the SDK using `make sdk_untar` in the `cryme_app` folder: proceed
with this (it should be done once). An emulator window should be opened.

Finally, you can install the compiled CRY.ME application in the emulator using `adb`. Supposing that the
`build` folder is created following a proper compilation, you can go to the `cryme_app` folder and
execute the `make app_install` command.

Please ensure that **only one instance** of the emulator is running when installing (so that there is no
ambiguity for `adb` to know where to install the application).

```
cryme_app$ make app_install
...
[+] Installing the CRY.ME app
element/matrix-sdk-android/platform-tools/adb install build/cryme/gplay/debug/vector-gplay-universal-debug.apk
Performing Streamed Install
Success
```

After the installation of the application on every instance for every user, you can begin to use the CRY.ME app while
launching all the instances and sign up/sign in users. Please ensure that **all the Yubikeys** associated to
the users you "emulate" are plugged in when you launch the associated images and **do not unplug** them during
your emulation session: `qemu` USB passthrough is not very resilient to hot (un)plugging.
When asked for a server, enter `cryme.fr` and accept to "trust" it (this is asked because of the self-signed
TLS certificates on the demo server). Since the Yubikey might not
be necessary after a Sign Up / Sign In, you can also force launching the emulator instance associated to a user
with `noyubi` as a second argument:

```
cryme_app_emulation$ ./launch_emulator_usb.sh 1 noyubi
[+] Launching the emulator WITHOUT the associated Yubikey (as forced by the command line)
...
```

**NOTE**: keep all the necessary Yubikeys plugged in during the whole emulation session.

### Emulation demo video

Here is a little demo for the full workflow of local CRY.ME emulation:

https://github.com/ANSSI-FR/cry-me/assets/38430857/35b3b82c-9653-4e29-9869-f57849d8b6f8

**NOTE**: in this demo video, during the last part emulating CRY.ME with two users, the two Yubikeys
were simultaneously plugged in during the whole emulation session. Please be aware that the emulator does not support
**hot (un)plugging**, so do not unplug you Yubikeys when they are needed (or you will have to launch the
instances again).
