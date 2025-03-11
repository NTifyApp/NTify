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
import java.util.Optional;

public class Updater {

    public static class UpdateInfo implements Serializable {
        public String commit_id = "";
        public String url = "";
    }

    public static Optional<UpdateInfo> updateAvailable() throws IOException {
        GitHubAPI.Artifacts artifacts = GitHubAPI.getArtifacts();
        if(!artifacts.artifacts.get(0).workflow_run.head_sha.equals(ApplicationUtils.getFullVersion())) {
            UpdateInfo info = new UpdateInfo();
            info.commit_id = artifacts.artifacts.get(0).workflow_run.head_sha;
            info.url = artifacts.artifacts.get(0).archive_download_url;
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
                Paths.get(PublicValues.appLocation + File.separator + "SpotifyXP.jar"),
                new File(System.getProperty("java.io.tmpdir"), "SpotifyXP.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );

        // 2. Execute the copied SpotifyXP.jar and stop the current process
        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-jar",
                new File(PublicValues.tempPath, "SpotifyXP.jar").getAbsolutePath(),
                "--run-updater=" + Base64.getEncoder().encodeToString(serializedUpdateInfo)
        );
        builder.start();

        System.exit(0);
    }
}
