import os
import sys
import subprocess

license_file = open("license.txt", "r")
license_text = license_file.read()

exclude_directories = ["src/main/java/com/spotifyxp/deps"]


def read_folder(path, files):
    for file in os.listdir(path):
        if os.path.isfile(os.path.join(path, file)):
            files.append(os.path.join(path, file))
        else:
            if os.path.join(path, file) in exclude_directories:
                return
            read_folder(os.path.join(path, file), files)


def get_git_modification_years(file_path):
    """
    Gets the earliest and latest modification years for a given file or directory
    in a Git repository.

    Args:
        file_path (str): The path to the file or directory within the Git repository.

    Returns:
        tuple: A tuple containing two strings: (earliest_year, latest_year).
               Returns (None, None) if no commits are found or an error occurs.
    """
    try:
        earliest_year_cmd = [
            "git", "log", "--reverse", "--date=format:%Y", "--pretty=format:%ad",
            "--", file_path
        ]
        earliest_year_result = subprocess.run(
            earliest_year_cmd,
            capture_output=True,
            text=True,
            check=True
        )
        earliest_year = earliest_year_result.stdout.strip().split('\n')[0] if earliest_year_result.stdout.strip() else None
    except subprocess.CalledProcessError as e:
        print(f"Error getting earliest year for '{file_path}': {e}")
        print(f"Git stderr: {e.stderr}")
        return None, None
    except IndexError:
        earliest_year = None
    try:
        latest_year_cmd = [
            "git", "log", "-1", "--date=format:%Y", "--pretty=format:%ad",
            "--", file_path
        ]
        latest_year_result = subprocess.run(
            latest_year_cmd,
            capture_output=True,
            text=True,
            check=True
        )
        latest_year = latest_year_result.stdout.strip() if latest_year_result.stdout.strip() else None
    except subprocess.CalledProcessError as e:
        print(f"Error getting latest year for '{file_path}': {e}")
        print(f"Git stderr: {e.stderr}")
        return None, None

    return earliest_year, latest_year


files = []
read_folder("src/main/java/com/spotifyxp", files)

for file in files:
    file_open = open(file, "r")
    file_content = file_open.read()
    if not file_content.strip().startswith("/*"):
        earliest, latest = get_git_modification_years(file)
        year_string = ""
        file_write = open(file, "w")
        if str(earliest) == latest:
            year_string = f"{latest}"
        else:
            year_string = f"{earliest}-{latest}"
        print("Working on " + file)
        file_write.write(license_text.replace("YEAR", year_string) + "\n" + file_content)
        file_write.close()
