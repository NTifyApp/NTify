import shutil, errno
import os
import fnmatch
import fileinput

def copyDirectory(src, dst):
    try:
        shutil.copytree(src, dst)
    except OSError as exc:
        if exc.errno in (errno.ENOTDIR, errno.EINVAL):
            shutil.copy(src, dst)
        else: raise

def removeDirectory(src):
    try:
        shutil.rmtree(src)
    except OSError as exc:
        raise

def mass_replace(directory, file_pattern, search_string, replace_string):
    for root, dirs, files in os.walk(directory):
        for filename in fnmatch.filter(files, file_pattern):
            filepath = os.path.join(root, filename)
            with fileinput.FileInput(filepath, inplace=True, backup=False) as file:
                for line in file:
                    print(line.replace(search_string, replace_string), end='')

def doMPRISJava():
    def copyMPRISJava():
        if os.path.exists("src/main/java/com/spotifyxp/deps/org"): removeDirectory("src/main/java/com/spotifyxp/deps/org")
        copyDirectory("deps/mpris-java/src/main/java/org", "src/main/java/com/spotifyxp/deps/org")
        mass_replace("src/main/java/com/spotifyxp/deps/org/mpris", "*.java", "import org.mpris", "import com.spotifyxp.deps.org.mpris")
        mass_replace("src/main/java/com/spotifyxp/deps/org/mpris", "*.java", "package org.mpris", "package com.spotifyxp.deps.org.mpris")
    if os.path.exists("src/main/java/com/spotifyxp/deps/org"):
        inp = input("Overwrite mpris-java? [Y/N]")
        if inp.lower().__eq__("y"):
            copyMPRISJava()
        elif inp.lower().__eq__(""):
            copyMPRISJava()
        elif inp.lower().__eq__("n"):
            return
        else:
            doMPRISJava()
    else:
        copyMPRISJava()

def doJavaSetupTool():
    def copyJavaSetupTool():
        if os.path.exists("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool"): removeDirectory("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool")
        copyDirectory("deps/JavaSetupTool/src/main/java/de/werwolf2303/javasetuptool", "src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool")
        mass_replace("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool", "*.java", "import de.werwolf2303.javasetuptool", "import com.spotifyxp.deps.de.werwolf2303.javasetuptool")
        mass_replace("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool", "*.java", "package de.werwolf2303.javasetuptool", "package com.spotifyxp.deps.de.werwolf2303.javasetuptool")
        mass_replace("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool", "*.form", "bind-to-class=\"", "bind-to-class=\"com.spotifyxp.deps.")
        mass_replace("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool", "*.form", "de.werwolf2303.javasetuptool.swingextensions.JImagePanel", "com.spotifyxp.deps.de.werwolf2303.javasetuptool.swingextensions.JImagePanel")
    if os.path.exists("src/main/java/com/spotifyxp/deps/de/werwolf2303/javasetuptool"):
        inp = input("Overwrite JavaSetupTool? [Y/N]")
        if inp.lower().__eq__("y"):
            copyJavaSetupTool()
        elif inp.lower().__eq__(""):
            copyJavaSetupTool()
        elif inp.lower().__eq__("n"):
            return
        else:
            doJavaSetupTool()
    else:
        copyJavaSetupTool()

def doVlcj():
    def copyVlcj():
        if os.path.exists("src/main/java/com/spotifyxp/deps/uk"): removeDirectory("src/main/java/com/spotifyxp/deps/uk")
        if os.path.exists("src/main/resources/vlc"): removeDirectory("src/main/resources/vlc")
        copyDirectory("deps/vlcj/src/main/java/uk", "src/main/java/com/spotifyxp/deps/uk")
        copyDirectory("deps/vlcj/src/main/resources/vlc", "src/main/resources/vlc")
        mass_replace("src/main/java/com/spotifyxp/deps/uk", "*.java", "import uk.co.caprica", "import com.spotifyxp.deps.uk.co.caprica")
        mass_replace("src/main/java/com/spotifyxp/deps/uk", "*.java", "import static uk.co.caprica", "import static com.spotifyxp.deps.uk.co.caprica")
        mass_replace("src/main/java/com/spotifyxp/deps/uk", "*.java", "package uk.co.caprica", "package com.spotifyxp.deps.uk.co.caprica")
    if os.path.exists("src/main/java/com/spotifyxp/deps/uk"):
        inp = input("Overwrite vlcj? [Y/N]")
        if inp.lower().__eq__("y"):
            copyVlcj()
        elif inp.lower().__eq__(""):
            copyVlcj()
        elif inp.lower().__eq__("n"):
            return
        else:
            doVlcj()
    else:
        copyVlcj()

def doInit():
    doMPRISJava()
    doJavaSetupTool()
    doVlcj()

doInit()