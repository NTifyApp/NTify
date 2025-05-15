import os.path
import subprocess
import sys
import zipfile
from init import doInit


JAR_PATH = "target/NTify.jar"

FLAGS_PATH = "src/main/java/com/spotifyxp/Flags.java"

flags = {}

for arg in sys.argv[1:]:
    split_val = arg.split("=")
    flags[split_val[0]] = split_val[1]

doInit()

available_flags = {}

print("Writing flags")
fopen = open(FLAGS_PATH, "r+")
fdata = fopen.read()
new_file = ""
for line in fdata.split("\n"):
    if line.__contains__("public static final boolean"):
        var_name = line.split("boolean ")[1].split(" = ")[0]
        if flags.__contains__(var_name):
            line = line.split(" = ")[0]
            line += " = " + str(flags[var_name]).lower() + ";"
        available_flags[var_name] = line.split(" = ")[1].replace(";", "").strip() == "true"
    new_file += line + "\n"
fopen.seek(0)
fopen.write(new_file)
fopen.close()

print(available_flags)

subprocess.call("mvn package", stdin=subprocess.PIPE, stderr=subprocess.STDOUT, shell=True)

print("Stripping class files")

mac_ignored_files = [
    "MacOSXSupportModule",
    "MacOSAppUtil",
    "spotifyxp.icns"
]

mac_ignored_directories = [
    "com/dd/plist/"
]

linux_ignored_files = [
    "LinuxSupportModule",
    "LinuxAppUtil"
]

linux_ignored_directories = [
    "org/mpris",
    "org/freedesktop/dbus",
    "jnr",
    "META-INF/versions/11/org/freedesktop/dbus"
]

video_support_ignored_files = [
]

video_support_ignored_directories = [
    "jni",
    "com/spotifyxp/deps/uk/co/caprica/vlcj/"
]

generally_ignored_directories = [
    "jni/x86_64-SunOS",
    "jni/sparcv9-Linux",
    "jni/x86_64-DragonFlyBSD",
    "jni/ppc64-Linux",
    "jni/ppc64le-Linux",
    "jni/ppc-AIX",
    "jni/aarch64-FreeBSD",
    "jni/x86_64-OpenBSD",
    "jni/i386-SunOS",
    "jni/mips64el-Linux",
    "jni/x86_64-FreeBSD",
    "jni/sparcv9-SunOS",
    "jni/ppc64-AIX",
    "jni/s390x-Linux",
    "example",
    "META-INF/proguard",
    "META-INF/native-image/org.xerial/sqlite-jdbc",
    "META-INF/maven"
]

def array_contains(array, file, isFile = True) -> bool:
    for entry in array:
        if isFile:
            if os.path.basename(file).startswith(entry):
                return True
        else:
            if file.startswith(entry):
                return True
    return False

with zipfile.ZipFile(JAR_PATH, "r") as jar_read:
    all_files = jar_read.namelist()
    stripped_files = []
    for file in all_files:
        if available_flags["macosSupport"] == False:
            if array_contains(mac_ignored_files, file):
                continue
            if array_contains(mac_ignored_directories, file, False):
                continue
        if available_flags["linuxSupport"] == False:
            if array_contains(linux_ignored_files, file):
                continue
            if array_contains(linux_ignored_directories, file, False):
                continue
        if available_flags["videoPlaybackSupport"] == False:
            if array_contains(video_support_ignored_files, file):
                continue
            if array_contains(video_support_ignored_directories, file, False):
                continue
        if array_contains(generally_ignored_directories, file, False):
            continue
        stripped_files.append(file)
    temp_zip_path = "target/NTify.jar.temp"
    with zipfile.ZipFile(temp_zip_path, "w", zipfile.ZIP_DEFLATED) as temp_zip:
        for file in stripped_files:
            file_info = jar_read.getinfo(file)
            with jar_read.open(file_info) as source:
                temp_zip.writestr(file_info, source.read())
    os.remove(JAR_PATH)
    os.rename(temp_zip_path, JAR_PATH)
