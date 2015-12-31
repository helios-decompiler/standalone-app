# About

Helios is an all-in-one Java reverse engineering tool. It features integration with the latest up-to-date decompilers.

There are several keybinds that can be used. To open a new file, simply use `Ctrl-O`. Arrow keys can be used to
navigate the file tree. Enter or clicking a file will open that file into the editor view.

To open a different view, simply use `Ctrl-T` and a menu will pop up where the cursor is. You can use your arrow keys
or your mouse to select a view.

If the file is a class file, you will be able to choose from a variety of decompilers and disassemblers. If the file
is not a class file, you will only be able to view the hex representation and the plain text representation.

If you want to clear the GUI, you can use `Ctrl-N` to start a new session. This will reset all open views.

If you have modified a file that you are inspecting, you can use `F5` to refresh the files. This will reload them
from disk.

To close the current opened file, use `Ctrl-W`. To close the current opened view, use `Ctrl-Shift-W`

All files are stored inside the default temporary file directory on your OS. You can delete these files safely.

The settings file is located within the `.helios` folder inside your user directory. Inside the `.helios` folder you
will find libraries which are not written in Java, but have been packaged with this program. You will also find
the addons folder, where you can install new addons.

# Addons

The addon API is currently under development. There are minimal events which you are able to hook into.
More will be added as the API progresses.

# Contributing

Thanks for wanting to help! This project is currently licensed under the Apache 2.0 license.

First, you should [sign the CLA](https://www.clahub.com/agreements/samczsun/Helios).This is for the good of
everyone - contributors, distributors, and end users.

Here are some basic conventions you should follow. Your coding style is your own but requirements may also change.
Please do not be surprised if you find your code has been reformatted.

* GUI and logic _must_ be separated. If you need to call logic from the GUI use an event. This is so the project is 
future-proofed should we ever want to switch GUI libraries.  
* If your code could throw an exception, handle it appropriately. If you want a popup to be shown, use
`ExceptionHandler.handle(exception);`  
* Opening brackets should be on the same line as the preceding statement
    
If you encounter an error while downloading the dependencies, you may need to manually install the Let's Encrypt
intermediary certificate into the Java trusted root store. You can do so using this command:

`keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit 
-importcert -file lets-encrypt-x1-cross-signed.pem`

The intermediary root CA has been included in the root of this project.

# Acknowledgements

Icons for the tree are sourced from the "Silk" icon pack, located [here](http://famfamfam.com/lab/icons/silk/)  

# FAQ

Q: Why is JD-GUI not included? I use it all the time!  
A: JD-GUI is incompatible with this project. It's a legality issue