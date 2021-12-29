# JavaFX 17
export PATH_TO_FX=~/sdks/javafx-sdk-17/lib

# Java 17 with Panama
#export JAVA_HOME=~/sdks/jdk-17.jdk/Contents/Home
# Java 19 with Panama
export JAVA_HOME=$HOME/projects/panama/panama-foreign/build/macosx-x86_64-server-release/images/jdk
#  PATH
export PATH=$JAVA_HOME/bin:$PATH

# PortAudio libraries
export DYLD_LIBRARY_PATH=.:../portaudio/lib/.libs/
