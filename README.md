
**Prerequisites**
1. Install the Java SDK and set the JAVA_HOME and JDK_HOME system variables.  Add the java bin folder to the SYSTEM PATH
1. Install apache ant and set the ANT_HOME system variable. Add the ANT_HOME\bin folder to the SYSTEM PATH
1. Install python x64 v3.7.8 version and add to system PATH variable or create the next env command with that specific version of the Python interperter. 
1. Install virtual environment in source code root folder
    1. `python -m venv venv`
1. Install dependencies by executing the following script
    1. `.\InstallDeps_Win.bat` for Windows
    2. `.\InstallDepts_OSX` for Mac OSx
1. Now you should have the requirements in order to run the game engine or training scripts. See tasks for common run configurations

**Tasks** 

This project is using [Invoke] for build and run tasks. 
1. To execute from the command line first navigate to the root folder. 
2. Login to python shell within virtual environment by running 
    1. `.\venv\Scripts\activate`
    1. `invoke %task-name%`
3. Tasks names can be listed using `invoke -l`
 
    
[Invoke]: http://www.pyinvoke.org/