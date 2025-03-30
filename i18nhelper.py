import json
import os
import sys
import re

root = "src/main/java"
langf = "src/main/resources/lang/"

def replace_last(string, old, new):
    old_idx = string.rfind(old)
    return string[:old_idx] + new + string[old_idx+len(old):]

if sys.argv[1].__eq__("-skeleton"):
    rawen = open(langf + "en" + ".json", "r")
    print("Generating i18n Skeleton for SpotifyXP...")
    if os.path.isfile(os.path.join(langf, "skeleton.json")):
        os.remove(langf + "skeleton.json")
    f = open(langf + "skeleton.json", "a")
    c = 0
    p = json.loads(rawen.read())
    for key in p:
        p[key] = ""
    length = len(replace_last(json.dumps(p).replace("{", "", 1), "}", "").split("\","))
    for li in replace_last(json.dumps(p).replace("{", "", 1), "}", "").split("\","):
        if(c == 0):
            li = "{\n" + replace_last(json.dumps(p).replace("{", "", 1), "}", "").split("\",")[0] + "\","
        else:
            if(c == length-1):
                li = li + "\n}"
            else:
                li = li + "\","
        f.write(li + "\n")
        c = c+1
    f.close()
    print("Done!")
    sys.exit()



langfile = open(langf + sys.argv[1] + ".json", "r")

files = []

def recursive(path):
    for filename in os.listdir(path):
        f = os.path.join(path, filename)
        # checking if it is a file
        if os.path.isfile(f):
            files.append(f)
        else:
            recursive(f)

recursive(root)

used = []

for f in files:
    if not str(f).lower().endswith(".java"):
        continue
    file = open(f, "r")
    while True:
        line = file.readline()
        if not line:
            break
        if (file.name.endswith("ConfigValues.java")):
            regex = r'"([^"]*)"'
            parameters = re.findall(regex, line)
            if len(parameters) > 0:
                for parameter in parameters:
                    used.append(parameter)
            continue
        if not line.__contains__("PublicValues.language.translate"):
            continue
        if not line.__contains__("translate(\""):
            continue
        if not line.__contains__(","):
            if(used.__contains__(line.split("translate(\"")[1].split("\"")[0])):
                continue
            used.append(line.split("translate(\"")[1].split("\"")[0])
            continue
        for found in line.split(","):
            if not line.__contains__("translate(\""):
                continue
            if(found.__contains__("PublicValues.language.translate")):
                if (used.__contains__(found.split("translate(\"")[1].split("\"")[0])):
                    continue
                used.append(found.split("translate(\"")[1].split("\"")[0])

    file.close()

parsed = json.loads(langfile.read())

unused = []

for key in parsed:
    found = False
    for u in used:
        if u.__eq__(key):
            found = True
            break
    if not found:
        unused.append(key)

unused.remove("ui.about.deps")
unused.remove("ui.about.description")
unused.remove("ui.about.tested")

for u in unused:
    print("Unused: " + u)
print("Found " + str(len(unused)) + " unused translations")

if len(unused) == 0:
    exit(0)

inp = input("Delete those? (y/n) ")

if(inp.__eq__("y")):
    for u in unused:
        del parsed[u]
    f = open(langf + sys.argv[1] + ".json", "w")
    c = 0
    length = len(replace_last(json.dumps(parsed).replace("{", "", 1), "}", "").split("\","))
    for li in replace_last(json.dumps(parsed).replace("{", "", 1), "}", "").split("\","):
        if(c == 0):
            li = "{\n" + replace_last(json.dumps(parsed).replace("{", "", 1), "}", "").split("\",")[0] + "\","
        else:
            if(c == length-1):
                li = li + "\n}"
            else:
                li = li + "\","
        f.write(li + "\n")
        c = c+1
    f.close()
else:
    print("Nothing was deleted")





