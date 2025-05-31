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

import com.spotifyxp.PublicValues;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ApplicationUtils {
    private static JSONObject object = null;
    private static final String ErrorMessage = "Check Application.json";

    private static void fetch() {
        object = new JSONObject(new Resources().readToString("Application.json"));
    }

    public static String getName() {
        if (object == null) {
            fetch();
        }
        if (object.has("Name")) {
            return object.getString("Name");
        } else {
            return ErrorMessage;
        }
    }

    public static String getVersion() {
        if(PublicValues.updaterDisabled || new Resources().readToInputStream("commit_id.txt") == null) {
            return "Debug Build";
        }
        return new Resources().readToString("commit_id.txt").substring(0, 7);
    }

    public static String getFullVersion() {
        if(PublicValues.updaterDisabled || new Resources().readToInputStream("commit_id.txt") == null) {
            return "";
        }
        return new Resources().readToString("commit_id.txt");
    }

    public static String getReleaseCandidate() {
        if (object == null) {
            fetch();
        }
        if (object.has("ReleaseCandidate")) {
            return object.getString("ReleaseCandidate");
        } else {
            return ErrorMessage;
        }
    }

    public static String getUserAgent() {
        String osSpecifier = System.getProperty("os.name").contains("mac") ? "Macintosh" :
                System.getProperty("os.name").contains("win") ? "Windows" : "Linux";
        ; //Macintosh
        String osName = System.getProperty("os.name"); //Mac OS X
        String osVersion = System.getProperty("os.version"); //10.15
        String browserSpecifier = "Java"; //Java
        String browserDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")); //21012024
        String applicationName = getName(); //SpotifyXP
        String applicationVersion = getVersion(); //2.0.2
        return "Mozilla/5.0 (" + osSpecifier + "; " + osName + " " + osVersion + ") " + browserSpecifier + "/" + browserDate + " " + applicationName + "/" + applicationVersion;
    }
}
