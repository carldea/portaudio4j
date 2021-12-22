# Examples of PortAudio called by Java's Panama APIs JEP 412.
At the moment this is purely exploratory (still kicking the tires) as it progresses 
in the future it may be a port of accessing PortAudio completely in Java.

Go to: http://portaudio.com/docs/v19-doxydocs/tutorial_start.html

# Getting started
1. Download Java 17 JDK with jextract tool here: https://jdk.java.net/panama/
2. Set your JAVA_HOME and PATH
   * Note: If installed in $HOME/sdks directory you can do the following:
     This assumes you're at **Step 6** below
     ```shell
     $ cd ~/projects/portaudio4j
     $ source setup.sh
     ``` 
3. Set your LD_LIBRARY_PATH or DYLD_LIBRARY_PATH
4. Clone and build PortAudio library: git@github.com:PortAudio/portaudio.git
5. Compile and run the first example paex_saw.c https://github.com/PortAudio/portaudio/blob/master/examples/paex_saw.c
6. Clone portaudio4j (this) project at: git@github.com:carldea/portaudio4j.git
7. Jextract header includes
8. Compile and run Java port of the example.

## Clone PortAudio
Create a projects folder that contains both PortAudio and later portaudio4j
```shell
$ cd ~
$ mkdir ~/projects
$ cd ~/projects
$ git clone git@github.com:PortAudio/portaudio.git
$ cd portaudio
```

## Create port audio library
```shell
$ ./configure && make
```

## Compile the first example called pae_saw.c
```shell
$ cd examples

# MacOS you'll need to include frameworks 
$ gcc -Wall \
   -I ../include \
   -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
   paex_saw.c \
   -o paex_saw ../lib/.libs/libportaudio.a \
   -framework CoreServices \
   -framework CoreFoundation \
   -framework AudioUnit \
   -framework AudioToolbox \
   -framework CoreAudio
```

## Run compiled example app.
```shell
$ ./paex_saw
```
# Run as the Java Panama version
Assuming the Port Audio library is build from the prior step you'll want to point the library path to the directory.
```shell
# MacOS
$ export DYLD_LIBRARY_PATH=.:~/projects/portaudio/lib/.libs/
# Linux
$ export LD_LIBRARY_PATH=.:~/projects/portaudio/lib/.libs/

$ cd ~/projects
$ git clone git@github.com:carldea/portaudio4j.git
$ cd portaudio4j
``` 

## Run `jextract` tool against foo.h containing stdio.h, math.h, portaudio.h
JExtract only works against one header (include) file so a trick is to create a foo.h containing the multiple includes.
```shell
$ ./jextract_foo.h.sh
```

## Compile Java PaexSaw.java
Will create Java classes locally.
```shell
$ ./compile_paex_saw.java.sh

# Or the following:
$ javac -cp .:classes \
   -d . \
   --add-modules jdk.incubator.foreign \
   src/PaexSaw.java
```

## Run PaexSaw.java
Will run the example PaexSaw.java
```shell
$ ./run_paex_saw.java.sh

# Or the following:
java -cp .:classes \
  --enable-native-access=ALL-UNNAMED \
  --add-modules jdk.incubator.foreign \
  PaexSaw
```
# Known issues:
1. The Panama version (this file), is too loud and may have some timing issue as the sound doesn't sound the same as the high pitch of the native C version.
2. This version is for MacOS and Linux at the moment. 
3. One setting is different. To have the same settings for SAMPLE_RATE 
   the example would be too loud!!! So, it's currently at 22050 hz.
   
