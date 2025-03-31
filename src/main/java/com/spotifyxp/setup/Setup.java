package com.spotifyxp.setup;

import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.de.werwolf2303.javasetuptool.RunnableWEC;
import com.spotifyxp.deps.de.werwolf2303.javasetuptool.components.AcceptComponent;
import com.spotifyxp.deps.de.werwolf2303.javasetuptool.components.InstallProgressComponent;
import com.spotifyxp.deps.mslinks.ShellLink;
import com.spotifyxp.deps.mslinks.ShellLinkHelper;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.utils.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class Setup {

    @SuppressWarnings("all")
    public Setup() throws IOException {
        SplashPanel.frame.setVisible(false);
        AcceptComponent thirdparty = new AcceptComponent(new Resources().readToString("setup/thirdparty.html"));
        com.spotifyxp.deps.de.werwolf2303.javasetuptool.Setup setup = new com.spotifyxp.deps.de.werwolf2303.javasetuptool.Setup.Builder()
                .setProgramImage(new Resources().readToInputStream("setup.png"))
                .setProgramName(ApplicationUtils.getName())
                .setProgramVersion(ApplicationUtils.getVersion())
                .setOnFinish(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                })
                .addComponents(thirdparty, getForSystem())
                .build();
        setup.open();
        while (true) {
            try {
                Thread.sleep(99);
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    public InstallProgressComponent getForSystem() {
        if (PublicValues.osType == libDetect.OSType.Linux) {
            return buildLinux();
        }
        if (PublicValues.osType == libDetect.OSType.MacOS) {
            return buildMacOS();
        }
        return buildWindows();
    }

    public InstallProgressComponent buildMacOS() {
        InstallProgressComponent macos = new InstallProgressComponent();
        try {
            macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(PublicValues.appLocation)
                    .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
            macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(new Resources().readToInputStream("spotifyxp.ico"))
                    .setTo(PublicValues.appLocation + File.separator + "spotifyxp.ico")
                    .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM));
            String jarPath = Initiator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(jarPath)
                    .setTo(PublicValues.appLocation + File.separator + "SpotifyXP.jar")
                    .setType(InstallProgressComponent.FileOperationTypes.COPY));
            macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setCustom(() -> {
                        MacOSAppUtil util = new MacOSAppUtil("SpotifyXP");
                        util.setIcon("spotifyxp.icns");
                        util.setExecutableLocation(PublicValues.appLocation + "/SpotifyXP.jar");
                        try {
                            util.create();
                            return true;
                        } catch (Exception e) {
                            ConsoleLogging.Throwable(e);
                            return false;
                        }
                    })
                    .setType(InstallProgressComponent.FileOperationTypes.CUSTOM));
        } catch (URISyntaxException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
        return macos;
    }

    public void addVideoPlaybackSupport(InstallProgressComponent component) {
        try {
            if (PublicValues.architecture == ArchitectureDetection.Architecture.x86) {
                if(new Resources().readToInputStream("vlc/files.txt") == null) return;
                File path = new File(PublicValues.appLocation, "vlc");
                if(!path.exists()) {
                    if(!path.mkdir()) {
                        ConsoleLogging.error("Failed to create vlc directory");
                        return;
                    }
                }
                for (String file : IOUtils.toString(new Resources().readToInputStream("vlc/files.txt"), StandardCharsets.UTF_8).split("\n")) {
                    File realPath = new File(path, file);
                    File dirPath = realPath.getParentFile();
                    component.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                                    .setCustom(new RunnableWEC() {
                                        @Override
                                        public boolean run() {
                                            if(!dirPath.exists()) {
                                                return dirPath.mkdirs();
                                            }
                                            return true;
                                        }
                                    })
                            .setType(InstallProgressComponent.FileOperationTypes.CUSTOM));
                    if(file.contains(".")) component.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                            .setFrom(getClass().getResourceAsStream("/vlc/" + file))
                            .setTo(realPath.getAbsolutePath())
                            .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM)
                    );
                }
            }
        }catch (Exception e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public InstallProgressComponent buildWindows() {
        InstallProgressComponent win = new InstallProgressComponent();
        try {
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(PublicValues.appLocation)
                    .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(new Resources().readToInputStream("spotifyxp.ico"))
                    .setTo(PublicValues.appLocation + "/spotifyxp.ico")
                    .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM));
            String jarPath = Initiator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(jarPath)
                    .setTo(PublicValues.appLocation + "/SpotifyXP.jar")
                    .setType(InstallProgressComponent.FileOperationTypes.COPY));
            addVideoPlaybackSupport(win);
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setCustom(() -> {
                        try {
                            ShellLink shellLink = new ShellLink();
                            shellLink.setIconLocation(PublicValues.appLocation + "/SpotifyXP.ico");
                            shellLink.setCMDArgs("--setup-complete");
                            ShellLinkHelper helper = new ShellLinkHelper(shellLink);
                            helper.setLocalTarget("C", PublicValues.appLocation.replace("C:\\", "") + "/SpotifyXP.jar");
                            helper.saveTo(System.getProperty("user.home") + "/Desktop/SpotifyXP.lnk");
                            return true;
                        } catch (Exception e) {
                            ConsoleLogging.Throwable(e);
                            return false;
                        }
                    }).setType(InstallProgressComponent.FileOperationTypes.CUSTOM));
        } catch (URISyntaxException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
        return win;
    }

    public InstallProgressComponent buildLinux() {
        InstallProgressComponent linux = new InstallProgressComponent();
        try {
            linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(PublicValues.fileslocation)
                    .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
            linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(PublicValues.appLocation)
                    .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
            linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(new Resources().readToInputStream("spotifyxp.ico"))
                    .setTo(PublicValues.appLocation + File.separator + "spotifyxp.ico")
                    .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM));
            String jarPath = Initiator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(jarPath)
                    .setTo(PublicValues.appLocation + File.separator + "SpotifyXP.jar")
                    .setType(InstallProgressComponent.FileOperationTypes.COPY));
            linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setCustom(() -> {
                        LinuxAppUtil util = new LinuxAppUtil("SpotifyXP");
                        util.setVersion(ApplicationUtils.getVersion());
                        util.setComment("Listen to Spotify");
                        util.setPath(PublicValues.appLocation);
                        util.setExecutableLocation("java -jar SpotifyXP.jar --setup-complete");
                        util.setIconlocation(PublicValues.appLocation + "/spotifyxp.ico");
                        util.setCategories("Java", "Music");
                        try {
                            util.create();
                            return true;
                        } catch (Exception e) {
                            ConsoleLogging.Throwable(e);
                            return false;
                        }
                    }).setType(InstallProgressComponent.FileOperationTypes.CUSTOM));
        } catch (URISyntaxException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
        return linux;
    }
}
