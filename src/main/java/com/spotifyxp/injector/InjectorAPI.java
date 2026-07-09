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
package com.spotifyxp.injector;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InjectorAPI {
    public static ArrayList<InjectorAPI.InjectorRepository> injectorRepos = new ArrayList<>();

    public InjectorAPI() {
        injectorRepos.add(new InjectorAPI.InjectorRepository("http://ntifyapp.werwolf2303.de/Extensions-Repository/repo/"));
    }

    @FunctionalInterface
    public interface ProgressRunnable {
        void run(long completeSize, long downloaded);
    }

    public static class InjectorRepository {
        private final String url;
        private Boolean isAvailable;

        public InjectorRepository(String url) {
            this.url = url;
        }

        public String getUrl() {
            return this.url;
        }

        /**
         * Performs a blocking reachability check (up to 2s) on first call, then caches the result.
         * Must not be called from the EDT.
         */
        public boolean isAvailable() {
            if (isAvailable == null) {
                try {
                    isAvailable = InetAddress.getByName(this.url.replace("https://", "").replace("http://", "").split("/")[0]).isReachable(2000);
                } catch (IOException e) {
                    isAvailable = false;
                }
                if (!isAvailable) {
                    ConsoleLogging.error("Repository with url '" + this.url + "' is not reachable");
                }
            }
            return isAvailable;
        }
    }

    public static class RepositoryExtensionLocation {
        private String location;

        public String getLocation() {
            return location;
        }
    }

    public static class Repository {
        private String name;
        private List<RepositoryExtensionLocation> extensions;

        public String getName() {
            return name;
        }

        public List<RepositoryExtensionLocation> getExtensions() {
            return extensions;
        }
    }

    public static class Dependency {
        private String name;
        private String author;
        private String location;
        private List<Dependency> dependencies;

        public String getName() {
            return name;
        }

        public String getAuthor() {
            return author;
        }

        public String getLocation() {
            return location;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }
    }

    public static class JarDependency {
        private String name;
        private String author;
        private List<JarDependency> dependencies;

        public String getName() {
            return name;
        }

        public String getAuthor() {
            return author;
        }

        public List<JarDependency> getDependencies() {
            return dependencies;
        }
    }

    public static class JarExtension {
        private String main;
        private String name;
        private String author;
        private String version;
        private String description;
        private String identifier;
        private List<JarDependency> dependencies;

        public String getMain() {
            return main;
        }

        public String getName() {
            return name;
        }

        public String getAuthor() {
            return author;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public String getIdentifier() {
            return identifier;
        }

        public List<JarDependency> getDependencies() {
            return dependencies;
        }
    }

    public static class Extension {
        private String name;
        private String author;
        private String version;
        private String description;
        private String location;
        private String identifier;
        private List<Dependency> dependencies;

        public String getName() {
            return name;
        }

        public String getAuthor() {
            return author;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation() {
            return location;
        }

        public String getIdentifier() {
            return identifier;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }
    }

    public static Repository getRepository(InjectorAPI.InjectorRepository repository) throws IOException {
        return new Gson().fromJson(get(repository.getUrl() + "/repo.json"), Repository.class);
    }

    public static List<Extension> getExtensions(InjectorAPI.InjectorRepository repository, Repository repo) throws IOException {
        ArrayList<Extension> extensions = new ArrayList<>();
        for(RepositoryExtensionLocation location : repo.getExtensions()) {
            extensions.add(new Gson().fromJson(get(repository.getUrl() + location.getLocation()), Extension.class));
        }
        return extensions;
    }

    public static Optional<Extension> getExtension(InjectorAPI.InjectorRepository repository, String name, String author) throws IOException {
        try {
            return Optional.of(new Gson().fromJson(get(repository.getUrl() + "/" + name + "-" + author + ".json"), Extension.class));
        }catch (JsonSyntaxException e) {
            return Optional.empty();
        }
    }

    private static String get(String url) throws IOException {
        return Objects.requireNonNull(PublicValues.defaultHttpClient.newCall(new Request.Builder()
                .url(url)
                .get()
                .build()).execute().body()).string();
    }

    private static String getFileName(String url) {
        return url.split("/")[url.split("/").length - 1];
    }

    public static void downloadExtension(InjectorAPI.Extension e, InjectorAPI.InjectorRepository repository, InjectorAPI.ProgressRunnable progressRunnable, String downloadPath) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) (new URL(repository.getUrl().replace("/repo", "") + e.getLocation()).openConnection());
        long completeFileSize = httpConnection.getContentLength();
        java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
        java.io.FileOutputStream fos = new java.io.FileOutputStream(downloadPath);
        java.io.BufferedOutputStream bout = new BufferedOutputStream(
                fos, 1024);
        byte[] data = new byte[1024];
        long downloadedFileSize = 0;
        int x;
        while ((x = in.read(data, 0, 1024)) >= 0) {
            downloadedFileSize += x;
            long finalDownloadedFileSize = downloadedFileSize;
            SwingUtilities.invokeLater(() -> progressRunnable.run(finalDownloadedFileSize, completeFileSize));
            bout.write(data, 0, x);
        }
        bout.close();
        in.close();
    }

    public static void downloadExtension(InjectorAPI.Extension e, InjectorAPI.InjectorRepository repository, InjectorAPI.ProgressRunnable progressRunnable) throws IOException {
        downloadExtension(e, repository,progressRunnable, PublicValues.fileslocation + "/Extensions/" + getFileName(e.getLocation()));
    }

    public static JarExtension getPluginJson(File file) throws IOException {
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{file.toURI().toURL()});
        JarExtension extension = new Gson().fromJson(IOUtils.toString(classLoader.getResourceAsStream("plugin.json"), StandardCharsets.UTF_8), JarExtension.class);
        classLoader.close();
        return extension;
    }
}
