# Brief Guide to Using the application

### First screen (Sign-up, Sign-in)

When you first open the application, you get the first screen with two options:

* Get Started: to create a new account
* I already have an account: to login to an already existing user on the server

Whatever the option you choose, you will be asked on the next screen to **connect to a trusted matrix server** where you need to input the server URL to which you would like to connect. If the server URL is valid, you will be taken to the next screen to input your credentials (username, password, and yubikey pin).

**Remark:** you can change your Yubikey pin code anytime. The application does not depend on it. However, at sign-up, the management key on the yubikey must be the one set by default. The application will need to upload a new authentication key on the Yubikey and uses the default management key to do so.

### Settings & Sign-out

When logged in, on the upper left side of the home screen, you will find a button of **3 horizontal lines**, when you click on it, you find a list of different spaces you belong to, as well as the **Settings** and **Sign out** buttons. 

In order to sign out from your account, click the **Sign out** button. If you haven't already set a backup encryption key, you will be prompted to do so before signing out in order to preserve your conversation history.

For profile settings, click your avatar at the top side of the screen which takes you to a page where you can manage your profile (avatar, display name, etc ...). You can also **Deactivate Your account** through this page.

For other settings, click the **Settings** button at the bottom side of the screen.

### Backup Encryption

In order to create backup encryption, you can either:

* click on the **Sign out** button where you will be prompted to do so if you haven't got any backup encryption key setup yet,
* or click on the **Settings** button, then go to **Security & Privacy** and finally scroll down and click on **Set up Secure Backup**

**Remark:** You can always change the password set for your encrypted backup by choosing the second option.

### Device Verification & Retrieving Encrypted Backup

When you log in to your account another time, you will be prompted through a notification to verify your device in order to retrieve your encrypted messages and account keys. If you have already set up backup encryption, this is done by simply inputting the password you use for the backup. If you haven't done it through the prompt, you can always retrieve your encrypted backup by going to **Settings** -> **Security & Privacy** -> **Set up Secure Backup** and inputting your backup password.

If you haven't set up backup encryption, the prompt will ask you to verify your new device with one of your former trusted devices that are currently logged in. During this step, you will perform the device verification protocol between your old and new devices. If you haven't got any trusted connected devices, you will **not** be able to retrieve your previous encrypted data on your new session anymore, and you will have to reset your keys.



### Create a new conversation

To create a conversation, from the home screen, click on the button that has a message icon with a plus sign (bottom right corner of the screen), then add as many users as you would like to the conversation and click on **create** at the top right corner. 

On the home screen, you can see all the rooms you belong to through either:

* in **Direct Messages** section where you can see one-to-one conversations and/or conversations you created, 
* or in **Rooms** section (by clicking on the hashtag-like button on the bottom right corner of the screen), where you can generally see group conversations you were added to.

### Room Settings

In a room, you can click on the three dots button at the top right corner of the screen to access room settings. In the settings, you can invite other users to the room, modify room profile settings, and allow end-to-end encryption if not allowed by default.

### Other User Device Verification

In order to trust another user's new device, you have to perform the device verification protocol with it. In order to do so, click on the user profile with which you would like to perform verification (you can do this by going to the room settings, checking the list of members of the room, and then clicking on the corresponding user).

If you already trust the user's device, you will see the trusting message in the **Security** section. If not, you will be able to click on the **Verify** button to launch the verification process.
