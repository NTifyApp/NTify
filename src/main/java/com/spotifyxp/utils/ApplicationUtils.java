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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ApplicationUtils {
    private static JsonObject object = null;
    private static final String ErrorMessage = "Check Application.json";

    private static void fetch() throws IOException {
        object = JsonParser.parseString(IOUtils.toString(Initiator.class.getResourceAsStream("/Application.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public static String getVersion() throws IOException {
        if(PublicValues.updaterDisabled || Initiator.class.getResourceAsStream("/commit_id.txt") == null) {
            return "Debug Build";
        }
        return IOUtils.toString(Initiator.class.getResourceAsStream("/commit_id.txt"), StandardCharsets.UTF_8).substring(0, 7);
    }

    /**
     * Application name is no longer stored in Application.json, so this method is deprecated and will be removed in a future release.
     * @return NTify
     */
    @Deprecated
    public static String getName() throws IOException {
        return "NTify";
    }

    public static String getFullVersion() throws IOException {
        if(PublicValues.updaterDisabled || Initiator.class.getResourceAsStream("/commit_id.txt") == null) {
            return "";
        }
        return IOUtils.toString(Initiator.class.getResourceAsStream("/commit_id.txt"), StandardCharsets.UTF_8);
    }

    public static String getReleaseCandidate() throws IOException {
        if (object == null) {
            fetch();
        }
        if (object.has("ReleaseCandidate")) {
            return object.get("ReleaseCandidate").getAsString();
        } else {
            return ErrorMessage;
        }
    }

    public static String getUserAgent() throws IOException {
        String osSpecifier = System.getProperty("os.name").contains("mac") ? "Macintosh" :
                System.getProperty("os.name").contains("win") ? "Windows" : "Linux";
        ; //Macintosh
        String osName = System.getProperty("os.name"); //Mac OS X
        String osVersion = System.getProperty("os.version"); //10.15
        String browserSpecifier = "Java"; //Java
        String browserDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")); //21012024
        String applicationVersion = getVersion(); //2.0.2
        return "Mozilla/5.0 (" + osSpecifier + "; " + osName + " " + osVersion + ") " + browserSpecifier + "/" + browserDate + " NTify/" + applicationVersion;
    }
}
