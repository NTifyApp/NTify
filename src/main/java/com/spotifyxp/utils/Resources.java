/*
 * Copyright [2023-2025] [Gianluca Beil]
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


public class Resources {

    /**
     * Reads the file specified from resources to s String
     * @see #readToString(Class, String)
     * @param path - The path of the file you want to read
     */
    @Deprecated
    public String readToString(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        InputStream stream = getClass().getResourceAsStream(path);
        try {
            assert stream != null;
            return IOUtils.toString(stream, Charset.defaultCharset());
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
        ConsoleLogging.Throwable(new Exception());
        return path;
    }

    /**
     * Reads the file specified from resources to s String
     * @see #readToString(Class, String)
     * @param clazz - Which class's resource folder to use
     * @param path - The path of the file you want to read
     */
    public String readToString(Class<?> clazz, String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        InputStream stream = clazz.getResourceAsStream(path);
        try {
            assert stream != null;
            return IOUtils.toString(stream, Charset.defaultCharset());
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
        ConsoleLogging.Throwable(new Exception());
        return path;
    }

    /**
     * Reads the file specified from resources to InputStream
     * @see #readToInputStream(Class, String) 
     * @param path - The path of the file you want to read
     */
    @Deprecated
    public InputStream readToInputStream(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return getClass().getResourceAsStream(path);
    }

    /**
     * Reads the file specified from resources
     * @param clazz - Which class's resource folder to use
     * @param path - The path of the file you want to read
     */
    public InputStream readToInputStream(Class<?> clazz, String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return clazz.getResourceAsStream(path);
    }
}
