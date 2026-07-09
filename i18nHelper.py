#  Copyright [2026] [Gianluca Beil]
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import argparse
import glob
import re
import yaml

translation_keys = []
duplicate_translation_keys = []
language_code = ""

# These keys are used in files that aren't parsed
IGNORED_TRANSLATION_KEYS = [
    "dialogs.about.description",
    "dialogs.about.tested",
    "dialogs.about.deps"
]

parser = argparse.ArgumentParser(description="Check for missing and unused translation keys in NTify.")
parser.add_argument("-l", "--language", nargs="?", const="en", default="en")
args = parser.parse_args()
language_code = args.language

def parse_translation_keys(obj, current_root=""):
    global translation_keys, duplicate_translation_keys
    for key, value in obj.items():
        full_key = f"{current_root}.{key}" if current_root else key
        if isinstance(value, dict):
            parse_translation_keys(value, full_key)
        else:
            if full_key in translation_keys:
                duplicate_translation_keys.append(full_key)
            else:
                translation_keys.append(full_key)

def find_keys_in_file(content):
    matches = re.findall(r'PublicValues\.language\.translate\("([^"]+)"\)', content)
    matches += re.findall(r'translationKey\s*=\s*"([^"]*)"', content)
    matches += re.findall(r'category\s*=\s*"([^"]*)"', content)
    for match in re.findall(r'GraphicalMessage\.showMessageDialog\("([^"]+)",\s*"([^"]+)"', content):
        matches += list(match)
    for match in re.findall(r'GraphicalMessage\.showConfirmDialog\("([^"]+)",\s*"([^"]+)"', content):
        matches += list(match)
    return matches

with open(f"src/main/resources/lang/{language_code}.yaml") as file:
    parse_translation_keys(yaml.load(file, Loader=yaml.FullLoader))

missing_translation_keys = []

for file in glob.glob("src/main/java/com/spotifyxp/**/*.java", recursive=True):
    content = open(file).read()
    for key in find_keys_in_file(content):
        if key not in translation_keys and key not in IGNORED_TRANSLATION_KEYS:
            missing_translation_keys.append(key)

for file in glob.glob("src/main/java/com/spotifyxp/**/*.java", recursive=True):
    content = open(file).read()
    for key in find_keys_in_file(content):
        if key in translation_keys:
            translation_keys.remove(key)

for ignored_key in IGNORED_TRANSLATION_KEYS:
    if ignored_key in translation_keys:
        translation_keys.remove(ignored_key)

if len(duplicate_translation_keys) > 0:
    print("-----------------------------")
    print(" Duplicate translation keys:\n")
    for key in duplicate_translation_keys:
        print(key)
    print("\n\n\n")

if len(missing_translation_keys) > 0:
    print("-----------------------------")
    print(" Missing translation keys:\n")
    for key in missing_translation_keys:
        print(key)

if len(missing_translation_keys) > 0 and len(translation_keys) > 0:
    print("\n\n\n")

if len(translation_keys) > 0:
    print("----------------------------")
    print(" Unused translation keys:\n")
    for key in translation_keys:
        print(key)

if len(missing_translation_keys) > 0 or len(translation_keys) > 0 or len(duplicate_translation_keys) > 0:
    exit(-1)