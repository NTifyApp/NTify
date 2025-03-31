package com.spotifyxp.injector;

import com.google.gson.Gson;
import com.spotifyxp.PublicValues;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.GraphicalMessage;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings({"DataFlowIssue", "UnusedAssignment"})
public class Injector {
    private int availableExtensions = 0;
    private int loadedExtensions = 0;
    private ArrayList<InjectionEntry> missingJars = new ArrayList<>();
    private InjectionEntry currentEntry = null;

    /**
     * Injects all extensions found inside the Extensions folder
     */
    public void autoInject() {
        if (!new File(PublicValues.appLocation, "Extensions").exists()) {
            new File(PublicValues.appLocation, "Extensions").mkdir();
        } else {
            for(File file : new File(PublicValues.appLocation, "Extensions").listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            })) {
                if(new File(file.getParentFile(), file.getName() + ".updated").exists()) {
                    file.delete();
                    new File(file.getParentFile(), file.getName() + ".updated").renameTo(file);
                }
            }
            availableExtensions = new File(PublicValues.appLocation, "Extensions").listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            }).length;
            boolean firstGoThrough = true;
            while (loadedExtensions != availableExtensions) {
                if (availableExtensions < 0) {
                    break;
                }
                if (availableExtensions == 0) {
                    break;
                }
                if (firstGoThrough) {
                    for (File file : new File(PublicValues.appLocation, "Extensions").listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".jar");
                        }
                    })) {
                        loadJarAt(file.getAbsolutePath());
                    }
                }
                for (InjectionEntry e : new ArrayList<>(missingJars)) {
                    currentEntry = e;
                    loadJarAt(e.path);
                }
                firstGoThrough = false;
            }
            Events.triggerEvent(SpotifyXPEvents.injectorAPIReady.getName());
        }
    }

    /**
     * Load extension jar file at the path
     *
     * @param path path of jar file
     */
    //ToDo: Implement dependencies in plugin.json
    @SuppressWarnings("unchecked")
    public void loadJarAt(String path) {
        if (!path.split("\\.")[path.split("\\.").length - 1].equalsIgnoreCase("jar")) return; //Invalid file
        InjectionEntry entry = new InjectionEntry();
        if (currentEntry != null) entry = currentEntry;
        entry.path = path;
        boolean foundIdentifier = false;
        boolean foundVersion = false;
        boolean foundAuthor = false;
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(path).toURI().toURL()});
            InjectorAPI.JarExtension pluginJSON = new Gson().fromJson(IOUtils.toString(Objects.requireNonNull(classLoader.getResourceAsStream("plugin.json")), Charset.defaultCharset()), InjectorAPI.JarExtension.class);
            Class<?> jarclass = classLoader.loadClass(pluginJSON.getMain());
            Object t = jarclass.newInstance();
            for (Method m : jarclass.getDeclaredMethods()) {
                if (m.getName().equals("getIdentifier")) {
                    Object o = m.invoke(t);
                    entry.identifier = o.toString();
                    foundIdentifier = true;
                }
                if (m.getName().equals("getVersion")) {
                    Object o = m.invoke(t);
                    entry.version = o.toString();
                    foundVersion = true;
                }
                if (m.getName().equals("getAuthor")) {
                    Object o = m.invoke(t);
                    entry.author = o.toString();
                    foundAuthor = true;
                }
                if (m.getName().equals("getDependencies")) {
                    Object o = m.invoke(t);
                    entry.dependencies = (ArrayList<Dependency>) o;
                }
            }
            //Support for the new plugin.json
            if(!foundVersion || !foundAuthor || !foundIdentifier) {
                if(pluginJSON.getVersion() != null) {
                    entry.version = pluginJSON.getVersion();
                    foundVersion = true;
                }
                if(pluginJSON.getAuthor() != null) {
                    entry.author = pluginJSON.getAuthor();
                    foundAuthor = true;
                }
                if(pluginJSON.getIdentifier() != null) {
                    entry.identifier = pluginJSON.getIdentifier();
                    foundIdentifier = true;
                }
            }
            //----
            entry.filename = new File(path).getName();
            if (foundVersion & foundIdentifier) {
                boolean metDependencies = true;
                ArrayList<Dependency> missing = new ArrayList<>();
                for (Dependency dependency : entry.dependencies) {
                    if (isJarInjected(dependency.identifier, dependency.author, dependency.version)) {
                        metDependencies = true;
                        missing.remove(dependency);
                    } else {
                        metDependencies = false;
                        missing.add(dependency);
                    }
                }
                if (!metDependencies) {
                    if (entry.gotOverTimes >= availableExtensions) {
                        //Dependency is missing
                        ConsoleLogging.error("Couldn't load extension with name => "
                                + entry.identifier + " version => "
                                + entry.version + " from => "
                                + entry.author + " because of these missing dependencies => "
                                + Arrays.toString(missing.toArray()));
                        missingJars.remove(entry);
                        availableExtensions--;
                        return;
                    }
                    entry.gotOverTimes++;
                    if (!missingJars.contains(path)) {
                        missingJars.add(entry);
                    }
                    classLoader.close();
                    return;
                } else {
                    missingJars.remove(path);
                }
                jarclass.getDeclaredMethod("init").invoke(t);
                entry.loaded = true;
                ConsoleLogging.info("Loaded Extension => " + entry.identifier + "-" + entry.version + "-" + entry.author);
                entry.loader = classLoader;
                loadedExtensions++;
            } else {
                classLoader.close();
            }
        } catch (JSONException jsonException) {
            ConsoleLogging.error("Failed to load extension: '" + path + "'! Invalid plugin.json");
            availableExtensions--;
            entry.failed = true;
        } catch (NullPointerException nullPointerException) {
            ConsoleLogging.error("Failed to load extension: '" + path + "'! plugin.json not found");
            availableExtensions--;
            entry.failed = true;
        } catch (InvocationTargetException e) {
            ConsoleLogging.error("Failed to load extension: '" + path + "'! Exception was thrown");
            ConsoleLogging.Throwable(e.getCause());
            availableExtensions--;
            entry.failed = true;
        } catch (Exception e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
            entry.failed = true;
            availableExtensions--;
        }
        if (!foundVersion || !foundIdentifier || !foundAuthor) {
            entry.failed = true;
            availableExtensions--;
            ConsoleLogging.error("Failed to load extension: '" + path + "'");
        } else {
            injectedJars.add(entry);
        }
    }

    public boolean isJarInjected(String name, String author) {
        for (InjectionEntry entry : injectedJars) {
            if (entry.identifier.equals(name) && entry.author.equals(author)) {
                return true;
            }
        }
        return false;
    }

    public boolean isJarInjected(String name, String author, String version) {
        for (InjectionEntry entry : injectedJars) {
            if (entry.identifier.equals(name) && entry.author.equals(author) && entry.version.equals(version)) {
                return true;
            }
        }
        return false;
    }

    public final ArrayList<InjectionEntry> injectedJars = new ArrayList<>();

    public ArrayList<InjectionEntry> getInjectedJars() {
        return injectedJars;
    }

    public static class InjectionEntry {
        public String filename = "";
        public String identifier = "";
        public String version = "";
        public String author = "";
        public ArrayList<Dependency> dependencies = new ArrayList<>();
        public boolean loaded = false;
        public boolean failed = false;
        public URLClassLoader loader = null;
        private int gotOverTimes = 0;
        private String path = "";
    }

    public static class Dependency {
        public String identifier = "";
        public String version = "";
        public String author = "";

        @Override
        public String toString() {
            return identifier + "-" + version + "-" + author;
        }
    }
}
