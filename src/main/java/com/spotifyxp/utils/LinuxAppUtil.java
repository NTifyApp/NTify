/*
 * Copyright [2023-2024] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.utils;

import com.spotifyxp.logging.ConsoleLogging;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LinuxAppUtil {
    String appName;
    String iconlocation;
    String executablepath;
    String ver;
    String com;
    String pa;
    String cat;

    public LinuxAppUtil(String name) {
        appName = name;
    }

    public void setIconlocation(String location) {
        iconlocation = location;
    }

    public void setExecutableLocation(String location) {
        executablepath = location;
    }

    public void setVersion(String version) {
        this.ver = version;
    }

    public void setComment(String comment) {
        com = comment;
    }

    public void setPath(String path) {
        pa = path;
    }

    public void setName(String name) {
        appName = name;
    }

    public void setCategories(String... category) {
        StringBuilder buffer = new StringBuilder();
        for (String s : category) {
            buffer.append(s).append(";");
        }
        cat = buffer.toString();
    }

    public static class DesktopEntry {
        public final String header = "[Desktop Entry]";
        public final String type = "Application";
        public String version;
        public String name;
        public String comment;
        public String path;
        public String exec;
        public String icon;
        public final boolean terminal = false;
        public String categories;
    }

    public DesktopEntry create() {
        DesktopEntry entry = new DesktopEntry();
        entry.categories = cat;
        entry.version = ver;
        entry.exec = executablepath;
        entry.icon = iconlocation;
        entry.comment = com;
        entry.name = appName;
        entry.path = pa;
        StringBuilder builder = new StringBuilder();
        builder.append(entry.header).append("\n");
        builder.append("Type=").append(entry.type).append("\n");
        builder.append("Version=").append(entry.version).append("\n");
        builder.append("Name=").append(entry.name).append("\n");
        builder.append("Comment=").append(entry.comment).append("\n");
        builder.append("Path=").append(entry.path).append("\n");
        builder.append("Exec=").append(entry.exec).append("\n");
        builder.append("Icon=").append(entry.icon).append("\n");
        builder.append("Terminal=").append(entry.terminal).append("\n");
        builder.append("Categories=").append(entry.categories);
        if (new File(System.getProperty("user.home") + "/.local/share/applications").exists()) {
            try {
                Files.copy(IOUtils.toInputStream(builder.toString(), Charset.defaultCharset()), new File(System.getProperty("user.home") + "/.local/share/applications", entry.name + ".desktop").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
        } else {
            if (new File("/usr/share/applications").exists()) {
                try {
                    Files.copy(IOUtils.toInputStream(builder.toString(), Charset.defaultCharset()), new File("/usr/share/applications", entry.name + ".desktop").toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    try {
                        Files.copy(IOUtils.toInputStream(builder.toString(), Charset.defaultCharset()), new File(System.getProperty("user.home") + "/Schreibtisch", entry.name + ".desktop").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e2) {
                        try {
                            Files.copy(IOUtils.toInputStream(builder.toString(), Charset.defaultCharset()), new File(System.getProperty("user.home") + "/Desktop", entry.name + ".desktop").toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e3) {
                            GraphicalMessage.bug("Cant create shortcut! All methods failed");
                        }
                    }
                }
            } else {
                GraphicalMessage.bug("No applications folder found!");
            }
        }
        return entry;
    }
}
