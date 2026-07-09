#!/usr/bin/env python3
from __future__ import annotations

import argparse
import base64
import concurrent.futures
import json
import logging
import os
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Tuple

import requests
import webbrowser
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs, quote_plus
import configparser
import re
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import xml.etree.ElementTree as ET
import warnings
from bs4 import BeautifulSoup, XMLParsedAsHTMLWarning
# (quote_plus already imported above)

LOGGER = logging.getLogger("depHelper")
logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")


# Common open source license texts - used as fallback when fetching fails
COMMON_LICENSES = {
    "MIT License": """MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.""",

    "The MIT License": """MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.""",

    "Apache License, Version 2.0": """Apache License
Version 2.0, January 2004

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction,
and distribution as defined in Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by
the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all
other entities that control, are controlled by, or are under common
control with that entity.

"You" (or "Your") shall mean an individual or Legal Entity exercising
permissions granted by this License.

"Source" form shall mean the preferred form for making modifications,
including but not limited to software source code, documentation
source, and configuration files.

"Object" form shall mean any form resulting from mechanical
transformation or translation of a Source form, including but
not limited to compiled object code, generated documentation,
and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or Object
form, made available under the License, as indicated by a copyright
notice that is included in or attached to the work.

"Derivative Works" shall mean any work, whether in Source or Object
form, that is based on (or derived from) the Work.

"Contribution" shall mean any work of authorship, including
the original Work and any Derivative Works thereof, submitted to,
and officially accepted for inclusion in, the Work by copyright owner
or by an individual or Legal Entity authorized to submit on behalf of
the copyright owner.

"Contributor" shall mean Licensor and any Legal Entity on behalf of
whom a Contribution has been received by Licensor and subsequently
incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
copyright license to reproduce, prepare Derivative Works of,
publicly display, publicly perform, sublicense, and distribute the
Work and such Derivative Works in Source or Object form.""",

    "The Apache Software License, Version 2.0": """Apache License
Version 2.0, January 2004

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction,
and distribution as defined in Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by
the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all
other entities that control, are controlled by, or are under common
control with that entity.

"You" (or "Your") shall mean an individual or Legal Entity exercising
permissions granted by this License.

"Source" form shall mean the preferred form for making modifications,
including but not limited to software source code, documentation
source, and configuration files.

"Object" form shall mean any form resulting from mechanical
transformation or translation of a Source form, including but
not limited to compiled object code, generated documentation,
and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or Object
form, made available under the License, as indicated by a copyright
notice that is included in or attached to the work.

"Derivative Works" shall mean any work, whether in Source or Object
form, that is based on (or derived from) the Work.

"Contribution" shall mean any work of authorship, including
the original Work and any Derivative Works thereof, submitted to,
and officially accepted for inclusion in, the Work by copyright owner
or by an individual or Legal Entity authorized to submit on behalf of
the copyright owner.

"Contributor" shall mean Licensor and any Legal Entity on behalf of
whom a Contribution has been received by Licensor and subsequently
incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
copyright license to reproduce, prepare Derivative Works of,
publicly display, publicly perform, sublicense, and distribute the
Work and such Derivative Works in Source or Object form.""",

    "BSD-3-Clause": """BSD 3-Clause License

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.""",

    "Modified BSD License": """BSD 3-Clause License

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.""",

    "GNU Lesser General Public License v3.0": """GNU LESSER GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

This version of the GNU Lesser General Public License incorporates
the terms and conditions of version 3 of the GNU General Public License.

When a program is linked with a library licensed under this License,
the combination of the program and the library is covered by the terms
of this License.

The GNU Lesser General Public License is a free software license.
All rights reserved. This program is free software: you can redistribute
it and/or modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.""",

    "GNU Lesser General Public License, Version 2.1": """GNU LESSER GENERAL PUBLIC LICENSE
Version 2.1, February 1999

Copyright (C) 1991, 1999 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1335 USA

Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

The GNU Lesser General Public License is a free software license.
All rights reserved. This program is free software: you can redistribute
it and/or modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 2.1 of the License,
or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.""",
}


@dataclass
class DependencyEntry:
    group_id: str = ""
    artifact_id: str = ""
    license_name: str = "No license"
    license_url: str = ""
    license_body: Optional[str] = None

    def id(self) -> str:
        return f"{self.group_id}:{self.artifact_id}"


def requests_session_with_retries(
        total_retries: int = 3, backoff_factor: float = 0.3, status_forcelist: Tuple[int, ...] = (500, 502, 504)
) -> requests.Session:
    session = requests.Session()
    retries = Retry(total=total_retries, backoff_factor=backoff_factor, status_forcelist=status_forcelist)
    adapter = HTTPAdapter(max_retries=retries)
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    session.headers.update({"User-Agent": "depHelper/1.0"})
    return session


def find_depinfo_links(directory: Path) -> List[str]:
    """Walk directory looking for .DEPINFO files; return their content (expected to be GitHub repo URLs)."""
    links: List[str] = []
    if not directory.exists():
        LOGGER.debug("Embedded directory %s does not exist, skipping", directory)
        return links
    for p in directory.rglob(".DEPINFO"):
        try:
            content = p.read_text(encoding="utf-8").strip()
            if content and content not in links:
                links.append(content)
        except Exception as exc:
            LOGGER.warning("Could not read %s: %s", p, exc)
    return links


def convert_to_repo_full_name(url: str) -> Optional[str]:
    """Convert 'https://github.com/owner/repo...' or 'owner/repo' to 'owner/repo' or return None if invalid."""
    if not url:
        return None
    original = url.strip()
    # strip common .git suffix
    if original.endswith('.git'):
        original = original[:-4]

    # git@github.com:owner/repo.git or git@github.com:owner/repo
    m = re.match(r"git@[^:]+:([^/]+/[^/]+)$", original)
    if m:
        return m.group(1)

    # ssh://git@github.com/owner/repo
    m2 = re.match(r"ssh://[^/]+/([^/]+/[^/]+)$", original)
    if m2:
        return m2.group(1)

    # https://github.com/owner/repo or http://github.com/owner/repo
    if original.startswith("https://github.com/") or original.startswith("http://github.com/"):
        parts = [p for p in original.split("/") if p]
        # parts like ['https:', 'github.com', 'owner', 'repo', ...]
        if len(parts) >= 4:
            return f"{parts[2]}/{parts[3]}"
        return None

    # already owner/repo
    if "/" in original:
        # return only first two path components
        parts = original.split("/")
        if len(parts) >= 2:
            return f"{parts[0]}/{parts[1]}"
    return None


def parse_gitmodules() -> List[str]:
    """Parse .gitmodules and return a list of repository URLs/strings (owner/repo when possible)."""
    gitmodules = Path(".gitmodules")
    repos: List[str] = []
    if not gitmodules.exists():
        return repos
    cfg = configparser.ConfigParser()
    try:
        cfg.read(gitmodules)
    except Exception as e:
        LOGGER.warning("Could not read .gitmodules: %s", e)
        return repos
    for section in cfg.sections():
        try:
            url = cfg.get(section, "url")
        except Exception:
            continue
        repo_full = convert_to_repo_full_name(url)
        if repo_full:
            if repo_full not in repos:
                repos.append(repo_full)
    return repos


def get_readable_for_submodules(token: Optional[str]) -> List[DependencyEntry]:
    """Create DependencyEntry list from git submodules (.gitmodules)."""
    session = requests_session_with_retries()
    entries: List[DependencyEntry] = []
    repos = parse_gitmodules()
    LOGGER.info("Found %d submodules in .gitmodules", len(repos))
    for repo_full in repos:
        entry = DependencyEntry()
        if "/" in repo_full:
            entry.group_id, entry.artifact_id = repo_full.split("/", 1)
        else:
            entry.group_id = repo_full
            entry.artifact_id = ""
        license_name, license_body_or_url = fetch_github_license_via_api(session, token, repo_full)
        if license_name:
            entry.license_name = license_name
        if license_body_or_url:
            if len(str(license_body_or_url).splitlines()) > 3 or str(license_body_or_url).startswith("http"):
                if str(license_body_or_url).startswith("http"):
                    entry.license_url = license_body_or_url
                else:
                    entry.license_body = license_body_or_url
            else:
                entry.license_url = license_body_or_url
        entries.append(entry)
    return entries


def fetch_github_license_via_api(session: requests.Session, token: Optional[str], repo_full: str) -> Tuple[Optional[str], Optional[str]]:
    """
    Use the GitHub REST API to fetch license metadata and content.
    Returns (license_name, license_body_or_url). license_body_or_url may be the license content or the html_url
    """
    api_url = f"https://api.github.com/repos/{repo_full}/license"
    headers = {}
    if token:
        headers["Authorization"] = f"token {token}"
    try:
        resp = session.get(api_url, headers=headers, timeout=10)
        if resp.status_code == 200:
            data = resp.json()
            license_name = data.get("license", {}).get("name") or data.get("license", {}).get("key") or "Unknown"
            # content is base64 encoded under 'content'
            content_b64 = data.get("content")
            if content_b64:
                try:
                    decoded = base64.b64decode(content_b64).decode("utf-8", errors="replace")
                    return license_name, decoded
                except Exception:
                    pass
            # fallback to html url
            html_url = data.get("html_url") or data.get("license", {}).get("url")
            return license_name, html_url
        elif resp.status_code == 404:
            LOGGER.debug("No license found via API for %s (404).", repo_full)
            return None, None
        else:
            LOGGER.warning("GitHub API returned %s for repo %s", resp.status_code, repo_full)
            return None, None
    except requests.RequestException as e:
        LOGGER.warning("Network error while fetching license for %s: %s", repo_full, e)
        return None, None


def authenticate_with_github_oauth(port: int = 41305, scope: str = "") -> Optional[str]:
    """Run a simple OAuth web flow to obtain a GitHub access token.
    Uses embedded credentials by default, or environment variables GITHUB_OAUTH_CLIENT_ID and GITHUB_OAUTH_CLIENT_SECRET if set.
    Returns the access token string or None on failure.
    """
    # Embedded default credentials (from original implementation)
    default_client_id = "Ov23litYkJBtG5B6rfXE"
    default_client_secret = "34f1e0261af4625c4e580735ff88b447f0840e2d"

    client_id = os.environ.get("GITHUB_OAUTH_CLIENT_ID", default_client_id)
    client_secret = os.environ.get("GITHUB_OAUTH_CLIENT_SECRET", default_client_secret)
    if not client_id or not client_secret:
        raise RuntimeError("OAuth credentials not found. Set GITHUB_OAUTH_CLIENT_ID and GITHUB_OAUTH_CLIENT_SECRET environment variables or use embedded defaults.")

    redirect_uri = f"http://127.0.0.1:{port}/callback"
    state = "dephelper-state"
    params = {
        "client_id": client_id,
        "redirect_uri": redirect_uri,
        "scope": scope,
        "state": state,
        "allow_signup": "false",
    }
    auth_url = f"https://github.com/login/oauth/authorize?{ '&'.join([f'{quote_plus(k)}={quote_plus(v)}' for k,v in params.items() if v is not None]) }"

    code_container = {"code": None, "error": None}

    class OAuthHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            parsed = urlparse(self.path)
            qs = parse_qs(parsed.query)
            if "code" in qs:
                code_container["code"] = qs.get("code")[0]
                self.send_response(200)
                self.send_header('Content-Type', 'text/html')
                self.end_headers()
                self.wfile.write(b"<html><body><h1>OK</h1><p>You may close this window.</p></body></html>")
                # shutdown server in a new thread to avoid blocking
                threading.Thread(target=self.server.shutdown, daemon=True).start()
            else:
                code_container["error"] = qs.get("error", [None])[0]
                self.send_response(400)
                self.end_headers()

        def log_message(self, format, *args):
            return

    httpd = HTTPServer(('127.0.0.1', port), OAuthHandler)
    LOGGER.info("Opening browser for GitHub OAuth login...")
    webbrowser.open(auth_url)

    # run server; this will block until shutdown is called by handler
    try:
        httpd.serve_forever()
    except Exception as e:
        LOGGER.debug("Local server stopped: %s", e)

    if code_container.get("code") is None:
        raise RuntimeError(f"OAuth failed or was cancelled: {code_container.get('error')}")

    code = code_container["code"]
    # exchange code for access token
    token_url = "https://github.com/login/oauth/access_token"
    headers = {"Accept": "application/json"}
    data = {"client_id": client_id, "client_secret": client_secret, "code": code, "redirect_uri": redirect_uri, "state": state}
    resp = requests.post(token_url, data=data, headers=headers, timeout=10)
    if resp.status_code != 200:
        raise RuntimeError(f"Token exchange failed: {resp.status_code} {resp.text}")
    j = resp.json()
    access_token = j.get("access_token")
    if not access_token:
        raise RuntimeError(f"No access token in response: {j}")
    return access_token


def get_readable_for_embedded(token: Optional[str], embedded_dir: Path) -> List[DependencyEntry]:
    session = requests_session_with_retries()
    entries: List[DependencyEntry] = []
    links = find_depinfo_links(embedded_dir)
    LOGGER.info("Found %d embedded .DEPINFO files", len(links))
    for link in links:
        repo_full = convert_to_repo_full_name(link)
        if not repo_full:
            LOGGER.warning("Could not parse repo from .DEPINFO content: %s", link)
            continue
        entry = DependencyEntry()
        entry.group_id, entry.artifact_id = repo_full.split("/", 1)
        license_name, license_body_or_url = fetch_github_license_via_api(session, token, repo_full)
        if license_name:
            entry.license_name = license_name
        if license_body_or_url:
            # If it's a full license body we set it; otherwise set URL
            if len(license_body_or_url.splitlines()) > 3 or license_body_or_url.startswith("http"):
                # if it's a URL we keep that in license_url, otherwise put as body
                if license_body_or_url.startswith("http"):
                    entry.license_url = license_body_or_url
                else:
                    entry.license_body = license_body_or_url
            else:
                entry.license_url = license_body_or_url
        entries.append(entry)
    return entries


def run_maven_license_plugin() -> None:
    """Run the Maven license aggregator to produce target/generated-resources/licenses.xml."""
    LOGGER.info("Running Maven license plugin to generate license metadata...")
    cmd = [
        "mvn",
        "org.codehaus.mojo:license-maven-plugin:2.0.0:aggregate-download-licenses",
        "-Dlicense.excludedScopes=system,test",
        "-Dlicense.sortByGroupIdAndArtifactId=true",
    ]
    try:
        subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        LOGGER.info("Maven license plugin completed.")
    except FileNotFoundError:
        LOGGER.error("mvn not found on PATH. Please install Maven or run with --no-maven.")
        raise
    except subprocess.CalledProcessError as e:
        LOGGER.error("Maven plugin failed (returncode=%s). stderr:\n%s", e.returncode, e.stderr.decode(errors="replace"))
        raise


def parse_maven_licenses_xml(path: Path) -> List[DependencyEntry]:
    entries: List[DependencyEntry] = []
    if not path.exists():
        LOGGER.warning("Maven licenses file not found at %s", path)
        return entries
    try:
        warnings.filterwarnings("ignore", category=XMLParsedAsHTMLWarning)
        with open(path, "r", encoding="utf-8") as f:
            parsed = BeautifulSoup(f.read(), "lxml")
        for dependency in parsed.find_all("dependency"):
            entry = DependencyEntry()
            # Extract group_id (groupid or groupId)
            g_elem = dependency.find("groupid") or dependency.find("groupId")
            entry.group_id = g_elem.text.strip() if g_elem else ""
            # Extract artifact_id (artifactid or artifactId)
            a_elem = dependency.find("artifactid") or dependency.find("artifactId")
            entry.artifact_id = a_elem.text.strip() if a_elem else ""
            # Extract license name - look in nested license/name or just name
            license_elem = dependency.find("license")
            if license_elem:
                name_elem = license_elem.find("name")
                if name_elem:
                    entry.license_name = name_elem.text.strip()
                else:
                    entry.license_name = license_elem.text.strip()
            else:
                name_elem = dependency.find("name")
                if name_elem:
                    entry.license_name = name_elem.text.strip()
            # Extract file URL
            file_elem = dependency.find("file")
            if file_elem:
                entry.license_url = file_elem.text.strip()
            entries.append(entry)
    except ET.ParseError as pe:
        LOGGER.error("Error parsing licenses.xml: %s", pe)
    except Exception as e:
        LOGGER.error("Error reading/parsing licenses.xml: %s", e)
    return entries


def download_license_bodies(entries: List[DependencyEntry], token: Optional[str], max_workers: int = 6) -> None:
    """If entries have license_url pointing to an API/html URL, attempt to download the body (concurrently)."""
    session = requests_session_with_retries()
    if token:
        session.headers.update({"Authorization": f"token {token}"})

    def fetch(e: DependencyEntry):
        if e.license_body:
            return
        url = e.license_url
        if not url:
            return
        # Try to construct valid URLs to fetch from
        urls_to_try = []

        # If it's already an HTTP(S) URL, use it
        if not (url.startswith("http://") or url.startswith("https://")):
            # If it looks like a filename from Maven, try to find license on GitHub
            if "/" in url:
                # Extract owner/repo and try raw content URL
                parts = url.split("/")
                if len(parts) >= 2:
                    # Try as raw GitHub URL
                    urls_to_try.append(f"https://raw.githubusercontent.com/{url}")
            else:
                # This is just a filename like "license-2.0.txt" or "mit-license.html"
                # Try to search GitHub for the license using group_id/artifact_id
                if e.group_id and e.artifact_id:
                    # First try direct owner/artifact repo
                    if "." not in e.group_id:
                        repo = f"{e.group_id}/{e.artifact_id}"
                        urls_to_try.append(f"https://api.github.com/repos/{repo}/license")
                    # Also try searching for artifact
                    urls_to_try.append(f"https://api.github.com/search/repositories?q={e.artifact_id}+in:name&per_page=1")

            if not urls_to_try:
                LOGGER.debug("Could not construct valid URLs from: %s", url)
                return
        else:
            urls_to_try.append(url)

        for attempt_url in urls_to_try:
            try:
                # Try to fetch the URL
                resp = session.get(attempt_url, timeout=10)
                if resp.status_code != 200:
                    LOGGER.debug("Could not download %s (status %s)", attempt_url, resp.status_code)
                    continue

                # Check content type
                ct = resp.headers.get("Content-Type", "")

                # If JSON and contains 'content', decode (GitHub API response)
                if "application/json" in ct:
                    try:
                        j = resp.json()
                        # Handle license API response
                        if "license" in j:
                            content_b64 = j.get("content")
                            if content_b64:
                                try:
                                    e.license_body = base64.b64decode(content_b64).decode("utf-8", errors="replace")
                                    return
                                except Exception:
                                    pass
                        # Handle search results - find first repo with license
                        if "items" in j:
                            for item in j.get("items", []):
                                repo_name = item.get("full_name")
                                if repo_name:
                                    # Fetch license from this repo
                                    license_url = f"https://api.github.com/repos/{repo_name}/license"
                                    try:
                                        lic_resp = session.get(license_url, timeout=10)
                                        if lic_resp.status_code == 200:
                                            lic_data = lic_resp.json()
                                            content_b64 = lic_data.get("content")
                                            if content_b64:
                                                try:
                                                    e.license_body = base64.b64decode(content_b64).decode("utf-8", errors="replace")
                                                    return
                                                except Exception:
                                                    pass
                                    except Exception:
                                        pass
                        # Legacy handling of content/body fields
                        content_b64 = j.get("content")
                        if content_b64:
                            try:
                                e.license_body = base64.b64decode(content_b64).decode("utf-8", errors="replace")
                                return
                            except Exception:
                                pass
                        # maybe body is under 'body'
                        if "body" in j:
                            e.license_body = j.get("body")
                            return
                    except Exception:
                        pass

                # Try to use plain text response
                if len(resp.text.strip()) > 0:
                    e.license_body = resp.text
                    return
            except requests.RequestException as exc:
                LOGGER.debug("Network error fetching license content %s: %s", attempt_url, exc)
                continue

    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as ex:
        list(ex.map(fetch, entries))


def fill_missing_license_bodies(entries: List[DependencyEntry]) -> None:
    """Fill in missing license bodies with common license texts based on license name."""
    for e in entries:
        if e.license_body:
            continue
        # First try to match license name
        for known_name, known_text in COMMON_LICENSES.items():
            if known_name.lower() in e.license_name.lower() or e.license_name.lower() in known_name.lower():
                e.license_body = known_text
                e.license_url = ""  # Clear URL if we have body
                LOGGER.debug("Using common license text for %s", e.license_name)
                break

        # If still no body, try to detect from license URL
        if not e.license_body and e.license_url:
            url_lower = e.license_url.lower()
            if "apache" in url_lower or "license-2.0" in url_lower:
                e.license_body = COMMON_LICENSES.get("The Apache Software License, Version 2.0", "")
                LOGGER.debug("Detected Apache license from URL: %s", e.license_url)
            elif "bsd" in url_lower or "bsd-3-clause" in url_lower:
                e.license_body = COMMON_LICENSES.get("Modified BSD License", "")
                LOGGER.debug("Detected BSD license from URL: %s", e.license_url)
            elif "mit" in url_lower:
                e.license_body = COMMON_LICENSES.get("The MIT License", "")
                LOGGER.debug("Detected MIT license from URL: %s", e.license_url)
            if e.license_body:
                e.license_url = ""  # Clear URL since we have body now


def augment_maven_entries_with_github(maven_entries: List[DependencyEntry], token: Optional[str]) -> None:
    """For maven entries that lack license info, try to find a GitHub repo and fetch its license.
    Heuristics:
      - if group_id looks like an owner (no dots), try owner/artifact
      - otherwise search GitHub repos by artifactId in name and prefer owner match or exact name
    This mutates entries in-place.
    """
    if not maven_entries:
        return
    session = requests_session_with_retries()
    headers = {}
    if token:
        headers["Authorization"] = f"token {token}"

    for e in maven_entries:
        needs = (not e.license_name or e.license_name == "No license") and (not e.license_body and not e.license_url)
        if not needs:
            continue
        repo_candidate = None
        # owner/artifact heuristic when group_id looks like a simple owner (no dots)
        if e.group_id and "." not in e.group_id and e.artifact_id:
            repo_candidate = f"{e.group_id}/{e.artifact_id}"
            LOGGER.debug("Trying candidate repo %s for %s", repo_candidate, e.id())
            lname, body_or_url = fetch_github_license_via_api(session, token, repo_candidate)
            if lname:
                LOGGER.info("Found license via candidate %s for %s: %s", repo_candidate, e.id(), lname)
                e.license_name = lname
                if body_or_url:
                    if str(body_or_url).startswith("http"):
                        e.license_url = body_or_url
                    else:
                        e.license_body = body_or_url
                continue
        # fallback: search by artifact name
        if not e.artifact_id:
            continue
        try:
            params = {"q": f"{e.artifact_id} in:name", "per_page": 5}
            resp = session.get("https://api.github.com/search/repositories", params=params, headers=headers, timeout=10)
            if resp.status_code != 200:
                LOGGER.debug("GitHub search failed for %s: %s", e.artifact_id, resp.status_code)
                continue
            data = resp.json()
            items = data.get("items", [])[:5]
            chosen = None
            for it in items:
                full = it.get("full_name")
                name = it.get("name")
                owner = it.get("owner", {}).get("login")
                if not full:
                    continue
                # prefer exact name match
                if name and name.lower() == e.artifact_id.lower():
                    chosen = full
                    break
                # prefer owner match
                if e.group_id and owner and owner.lower() == e.group_id.lower():
                    chosen = full
                    break
            if chosen:
                LOGGER.debug("Searching license for chosen repo %s (artifact %s)", chosen, e.artifact_id)
                lname, body_or_url = fetch_github_license_via_api(session, token, chosen)
                if lname:
                    LOGGER.info("Found license via search %s for %s: %s", chosen, e.id(), lname)
                    e.license_name = lname
                    if body_or_url:
                        if str(body_or_url).startswith("http"):
                            e.license_url = body_or_url
                        else:
                            e.license_body = body_or_url
        except requests.RequestException as exc:
            LOGGER.debug("GitHub search error for %s: %s", e.artifact_id, exc)


def generate_html(dependency_entries: List[DependencyEntry]) -> str:
    # Build a mapping of license_name to (body or first url)
    licenses = {}
    for e in dependency_entries:
        key = e.license_name or "No license"
        if key not in licenses:
            licenses[key] = {"body": e.license_body, "url": e.license_url}
        else:
            # prefer body over url
            if not licenses[key]["body"] and e.license_body:
                licenses[key]["body"] = e.license_body
            if not licenses[key]["url"] and e.license_url:
                licenses[key]["url"] = e.license_url

    parts = []
    parts.append("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>Thirdparty</title></head><body>")
    parts.append("<center><h1>ThirdParty</h1></center>")
    parts.append("<h2>Dependencies</h2><div>")
    for e in dependency_entries:
        # Skip entries with "No license"
        if e.license_name == "No license":
            continue
        parts.append(f"<h3>{e.group_id}:{e.artifact_id}</h3>")
        parts.append(f"<p>License: {e.license_name}</p>")
        parts.append("<hr/>")
    parts.append("</div><h2>Licenses</h2>")
    for lname, info in licenses.items():
        # Skip "No license" entries
        if lname == "No license":
            continue
        parts.append(f"<h3>{lname}</h3>")
        body = info.get("body")
        if body:
            # basic HTML-escape minimal; replace newlines with <pre>
            parts.append("<pre style='white-space: pre-wrap; background:#f6f6f6; padding: 1rem;'>")
            # Protect against inadvertent tags in license body
            parts.append(escape_html(body))
            parts.append("</pre>")
        elif info.get("url"):
            parts.append(f"<p>License text at <a href='{info['url']}'>{info['url']}</a></p>")
        else:
            parts.append("<p>No license text available.</p>")
    parts.append("</body></html>")
    return "\n".join(parts)


def escape_html(s: str) -> str:
    # minimal escaping to avoid injecting tags from license bodies
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def collect_all_entries(
        embedded_dir: Path,
        run_maven: bool,
        maven_licenses_xml: Path,
        token: Optional[str],
) -> List[DependencyEntry]:
    entries: List[DependencyEntry] = []
    # Prefer git submodules if present, otherwise fall back to embedded .DEPINFO scanning
    gitmodules = Path(".gitmodules")
    if gitmodules.exists():
        entries.extend(get_readable_for_submodules(token=token))
    else:
        entries.extend(get_readable_for_embedded(token=token, embedded_dir=embedded_dir))
    # Maven
    maven_entries: List[DependencyEntry] = []
    if run_maven:
        run_maven_license_plugin()
    maven_entries = parse_maven_licenses_xml(maven_licenses_xml)
    # If Maven didn't provide license info for some dependencies, try to find it on GitHub
    augment_maven_entries_with_github(maven_entries, token)
    entries.extend(maven_entries)
    # Deduplicate by group:artifact (preserve first)
    seen = set()
    deduped: List[DependencyEntry] = []
    for e in entries:
        sid = e.id()
        if sid not in seen:
            deduped.append(e)
            seen.add(sid)
    # Attempt to fetch license bodies where useful
    download_license_bodies(deduped, token=token)
    # Fill in missing license bodies with common license texts
    fill_missing_license_bodies(deduped)
    return deduped


def main(argv=None):
    p = argparse.ArgumentParser(description="Generate thirdparty licenses HTML")
    p.add_argument("--embedded-dir", type=Path, default=Path("src/main/java/com/spotifyxp/deps"), help="Directory to scan for .DEPINFO files")
    p.add_argument("--maven-licenses-xml", type=Path, default=Path("target/generated-resources/licenses.xml"), help="Maven generated licenses.xml")
    p.add_argument("--no-maven", dest="run_maven", action="store_false", help="Don't run the maven license plugin (default: run it)")
    p.add_argument("--output", type=Path, default=Path("src/main/resources/setup/thirdparty.html"), help="Output HTML file")
    p.add_argument("--token", type=str, default=os.environ.get("GITHUB_TOKEN"), help="GitHub token (env: GITHUB_TOKEN)")
    args = p.parse_args(argv)

    LOGGER.setLevel(logging.DEBUG)

    token = args.token

    if not token:
        try:
            token = authenticate_with_github_oauth()
            if token:
                LOGGER.info("Obtained OAuth token via web flow")
        except Exception as exc:
            LOGGER.error("OAuth flow failed: %s", exc)

    try:
        entries = collect_all_entries(
            embedded_dir=args.embedded_dir,
            run_maven=args.run_maven,
            maven_licenses_xml=args.maven_licenses_xml,
            token=token,
        )
    except Exception as exc:
        LOGGER.error("Failed to collect entries: %s", exc)
        sys.exit(2)

    html = generate_html(entries)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(html, encoding="utf-8")
    LOGGER.info("Wrote %d dependency entries to %s", len(entries), args.output)


if __name__ == "__main__":
    main()
