# Compiling and Installing Element Using Android Studio

### REMARK: The version of Android Studio necessary for the compilation is Android Studio Arctic Fox | 2020.3.1 Patch 4. If this version is not used, you might get compilation errors which you will need to resolve on your own.

### Step 1: git clone

First, you need to clone the git repository. In the following, **PATH** wil refer to the `cryme_app` path (or the path of the `cry.me.bundle` untared bundle).

### Step 2: specifying SDK location in Android Studio

In order to be able to compile the application, you need a working SDK. The people from Matrix implemented their own matrix-sdk which is sufficient to compile the application. To specify the path to matrix-sdk:

- Open Android Studio
- Go to “Customize” → “All settings” → “System Settings” → “Android SDK”
- At the top of the menu you should be able to see “Android SDK location”. Click on “Edit” and specify the following path
    - “PATH/element/matrix-sdk-android”
        - PATH is of course replaced by the absolute path to your local `cryme_app` folder.
- When you specify the path, Android Studio will ask you to install missing components and other stuff. Click on “Next” and let it install what’s missing by default.


### Step 3.A: Accept Licenses

In order to avoid further compilation errors, before importing the project, you need to accept some sdk related licenses. To do this:

- Go to “Customize” → “All settings” → “System Settings” → “Android SDK” → “SDK Tools”
- Check the box next to “Google Play Licensing Libraries”, and click on “Apply” to install it. (Accept the licenses along the way).


### Step 3.B: Installing NDK and CMAKE

You need to install cmake and NDK in order for olm-sdk to compile properly:

- Go again to “Customize” → “All settings” → “System Settings” → “Android SDK” → “SDK Tools”
- Check the box next to “CMake” and "NDK", and click on “Apply” to install them.


### Step 3.C: Specifying paths to NDK and SDK

You need to:

- create the file `local.properties` in the `element` folder
- write the following lines in the file:
```
sdk.dir=FULL_PATH/element/matrix-sdk-android
```
``
ndk.dir=FULL_PATH/element/matrix-sdk-android/ndk/VERSION
``

where you replace the `FULL_PATH` variable by the full path to the `element` folder of the project, and you replace `VERSION` variable depending on the NDK version that was installed on your computer.




### Step 4: Importing Element Project to Android Studio and Compiling it

You are now ready to import the project ! On the main menu in “Projects”, click on “Open”, and choose the project in “element”, i.e with the path “PATH/element”.

The compilation of the project should start on its own everytime you open the project. A “build” process is executed and you can check the progress on the bottom windows in “build” and “Problems”.

To explicitly build the project, click on the hammer sign at the top of the menu bar which points to “Make Project”.

If everything goes well, you should see in the “build” terminal the phrase “BUILD WAS SUCCESSFUL”-ish.

### Step 5: Running the application on the phone

Once the compilation succeeds, you are ready to install the application on the phone. To do so, you need to activate the developer options on the phone. On samsung devices, you should go to `settings -> about phone (or about device) -> software information` and click on `build number` 7 times. 

Once the developer options are activated, they will appear in the `settings`of the phone. Go in the `developer options`, and activate "USB debugging". Once done, you can connect the phone to your computer and android studio is supposed to detect it immediatly (if not, please deactivate and reactive "USB debugging"). Then, you just need to click on the "run" button to install the application. Enjoy !

### Some Compilation Errors that you might encounter

- If you encounter the following compilation error “**Unsupported Modules Detected: Compilation is not supported for following modules: . Unfortunately you can't have non-Gradle Java modules and Android-Gradle modules in one project.”**, then do the following:
    - Close Android Studio
    - In a terminal, go to the project folder “element”
    - execute “rm -rf .idea”
    - delete all “*.iml” files if any
    - Reopen Android Studio and compile, this should solve the problem

- If you encounter the following compilation error **“WARNING: It is not fully supported to define distributionSha256Sum in gradle-wrapper.properties. Using an incorrect value may freeze or crash Android Studio.”**, then do the following:
    - Close Android Studio
    - Open the file “element/gradle/wrapper/gradle-wrapper.properties
    - Delete the line “distributionSha256Sum=c9490e938b221daf0094982288e4038deed954a3f12fb54cbf270ddf4e37d879” from the file and save it
    - Reopen Android Studio and compile, this should solve the problem
