#!/bin/sh -x
rm -rf classes
rm -rf generated/src

# Please update the following.
# The following assumes the port audio library is a folder above this directory. For its include directory.
# jextract stdio.h
jextract --source -d generated/src -t org.portaudio \
  -l portaudio \
  -I ../portaudio/include \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  foo.h

# add to classes 
jextract -d classes -t org.portaudio \
  -l portaudio \
  -I ../portaudio/include \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  foo.h
