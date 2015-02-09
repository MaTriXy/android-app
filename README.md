# Blinkbox books Android App
___
You can browse, download and read eBooks on your Android device
  

##Setting Up
___

1. Download Android studio, Google’s preferred IDE, from the Android Tools website via the Canary Channel to ensure downloading the latest build:(<http://tools.android.com/download/studio/canary>)    

2. Select “Latest Build”, download the appropriate file and install

3. Locate the project repository you wish to contribute to on your machine (see the “*GitHub flow*” document found on *Confluence* for specific details)

4. Open Android Studio

5. Check for updates (ensuring latest version of Android Studio)
   
6. Select '*Import Project...*' and locate the desired project

7. Respond to warnings regarding SDK requirements - open *SDK Manager* and install required files

##Gradle, Wrappers and Builds##
___
- Gradle is Android Studio's Build System of choice  
  
- Using a Gradle Wrapper is the preferred way of starting a Gradle Build - (The project-specified Gradle version will be automatically downloaded and used to run the build)
___


1. Locate the project repository on your machine

        $ cd path/to/repository/
    
2. Modify the permission for the gradlew file (for Mac users and only required once)
    
        $ chmod +x gradlew  
        
          
3. Start a Gradle build via the wrapper
    
        $ ./gradlew 
       
4. Build all variants of the application
    
        $ ./gradlew build
        
        
5. Reveal a list of all available build tasks for the project
     
        $ ./gradlew tasks
        
        
##Dependency Management
___
- The project's dependencies are explicitly defined in the ***build.gradle*** files
   
- Dependencies are grouped into named sets, or ***configurations***
- Run specific tasks with Gradle
    
        $ ./gradlew <build task>
        
- Refresh the state of dependencies and validate whether the current build will be able to run within your environment

        $ ./gradlew --refresh-dependencies
        
- Refer to the Gradle documentation for further details: (<http://www.gradle.org/docs/current/userguide/userguide.html>)
- Gradle command line guide: (<http://www.gradle.org/docs/current/userguide/gradle_command_line.html>)


Developed By
============
* Eric YUAN - <mbaeric@gmail.com>
* Chirag Patel <chiguu@gmail.com>
* Tim Wright <timrlw@gmail.com>
* Jamie Higgins <jamie.higgins@gmail.com>
* Tom Hall <tom.j.hall.uk@gmail.com>
* Tomasz Szymaniec <tomek.szymaniec@gmail.com>

License
=======
The MIT License (MIT)

Copyright (c) 2015 blinkbox Books ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.