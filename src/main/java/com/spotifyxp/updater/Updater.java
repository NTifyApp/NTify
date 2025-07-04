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
package com.spotifyxp.updater;

import com.spotifyxp.PublicValues;
import com.spotifyxp.api.GitHubAPI;
import com.spotifyxp.utils.ApplicationUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class Updater {

    public static class UpdateInfo implements Serializable {
       public GitHubAPI.Release release;
    }

    public static Optional<UpdateInfo> updateAvailable() throws IOException {
        List<GitHubAPI.Release> releases = GitHubAPI.getReleases();
        if(!ApplicationUtils.getVersion().equals(releases.get(0).tag_name)) {
            UpdateInfo info = new UpdateInfo();
            info.release = releases.get(0);
            return Optional.of(info);
        }
        return Optional.empty();
    }

    public static void invoke(UpdateInfo info) throws IOException, URISyntaxException {
        byte[] serializedUpdateInfo;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(info);
            serializedUpdateInfo = bos.toByteArray();
        }

        // 1. Copy SpotifyXP into tmp
        Files.copy(
                Paths.get(PublicValues.appLocation + File.separator + "NTify.jar"),
                new File(System.getProperty("java.io.tmpdir"), "NTify.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );

        // 2. Execute the copied NTify.jar and stop the current process
        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-jar",
                new File(PublicValues.tempPath, "NTify.jar").getAbsolutePath(),
                "--run-updater=" + Base64.getEncoder().encodeToString(serializedUpdateInfo)
        );
        builder.start();

        System.exit(0);
    }
}
