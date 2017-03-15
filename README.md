# Standalone App
## About

Helios is an all-in-one Java reverse engineering tool. It features integration with the latest up-to-date decompilers.

If you want to download the latest version, check out the FAQ down below

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

# Acknowledgements
## Icons
Icons for the tree are sourced from the "Silk" icon pack, located [here](http://famfamfam.com/lab/icons/silk/)

## Performance
![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.

YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/index.jsp)
and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.
