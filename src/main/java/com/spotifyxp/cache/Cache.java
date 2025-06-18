/*
 * Copyright [2025] [Gianluca Beil]
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
package com.spotifyxp.cache;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.GraphicalMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@SuppressWarnings("unused")
public class Cache {
    private File cacheDir;
    private boolean cacheEnabled = true;

    public Cache() throws IOException {
        cacheDir = new File(PublicValues.fileslocation, "progcache");

        if(PublicValues.config.getBoolean(ConfigValues.cache_disabled.name)) {
            cacheDir = null;
            cacheEnabled = false;
        }

        if(!cacheDir.exists()) {
            if(!cacheDir.mkdir()) {
                throw new IOException("Unable to create cache directory");
            }
        }
    }

    public void remove(String id) throws IOException {
        if(!cacheEnabled) return;
        if(!new File(cacheDir, id).delete()) {
            throw new IOException("Unable to delete cache file");
        }
    }

    public void addBytes(String id, byte[] value) throws IOException {
        if(!cacheEnabled) return;
        if(new File(cacheDir, id).exists()) {
            remove(id);
        }
        Files.write(Paths.get(new File(cacheDir, id).getAbsolutePath()), value);
    }

    public boolean has(String id) {
        if(!cacheEnabled) return false;
        return new File(cacheDir, id).exists();
    }

    public byte[] getBytes(String id) throws IOException {
        if(!cacheEnabled) return null;
        if(!new File(cacheDir, id).exists()) {
            throw new IOException("Unable to find cache file");
        }
        return Files.readAllBytes(Paths.get(new File(cacheDir, id).getAbsolutePath()));
    }

    public void clear() {
        if(!cacheEnabled) return;
        for(File f : Objects.requireNonNull(cacheDir.listFiles())) {
            if(!f.delete()) {
                ConsoleLogging.error("Failed to delete cache file: " + f.getName());
            }
        }
    }
}
