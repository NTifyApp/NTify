import errno
import fileinput
import fnmatch
import os
import shutil
import sys
import subprocess


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

def doVlcj(interactive):
    def copyVlcj():
        if os.path.exists("src/main/java/uk"): removeDirectory("src/main/java/uk")
        copyDirectory("deps/vlcj/src/main/java/uk", "src/main/java/uk")
    if os.path.exists("src/main/java/com/spotifyxp/deps/uk"):
        if interactive:
            inp = input("Overwrite vlcj? [Y/N]")
            if inp.lower().__eq__("y"):
                copyVlcj()
            elif inp.lower().__eq__(""):
                copyVlcj()
            elif inp.lower().__eq__("n"):
                return
            else:
                doVlcj(interactive)
        else:
            copyVlcj()
    else:
        copyVlcj()

def doInit(interactive=False):
    subprocess.Popen(
        executable="mvn",
        args=["clean", "package"],
        shell=True,
        cwd="deps/mpris-java"
    ).wait()
    subprocess.Popen(
        executable="mvn",
        args=["clean", "package"],
        shell=True,
        cwd="deps/JavaSetupTool"
    ).wait()
    doVlcj(interactive)
    subprocess.Popen(
        executable="mvn",
        args=["clean", "package"],
        shell=True,
        cwd="deps/librespot-java"
    ).wait()
    subprocess.Popen(
        args=["./gradlew", "build"],
        cwd="deps/mslinks"
    ).wait()



if __name__ == '__main__':
    doInit(True)
