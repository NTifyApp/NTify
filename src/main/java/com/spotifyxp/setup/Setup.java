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
package com.spotifyxp.setup;

import com.spotifyxp.Flags;
import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.de.werwolf2303.javasetuptool.components.AcceptComponent;
import com.spotifyxp.deps.de.werwolf2303.javasetuptool.components.InstallProgressComponent;
import com.spotifyxp.deps.mslinks.ShellLink;
import com.spotifyxp.deps.mslinks.ShellLinkHelper;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.utils.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

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
        if(Flags.linuxSupport) {
            if (PublicValues.osType == libDetect.OSType.Linux) {
                return buildLinux();
            }
        }
        if(Flags.macosSupport) {
            if (PublicValues.osType == libDetect.OSType.MacOS) {
                return buildMacOS();
            }
        }
        return buildWindows();
    }

    public InstallProgressComponent buildMacOS() {
        if(Flags.macosSupport) {
            InstallProgressComponent macos = new InstallProgressComponent();
            try {
                macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(PublicValues.appLocation)
                        .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
                macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(new Resources().readToInputStream("ntify.ico"))
                        .setTo(PublicValues.appLocation + File.separator + "ntify.ico")
                        .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM));
                String jarPath = Initiator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(jarPath)
                        .setTo(PublicValues.appLocation + File.separator + "NTify.jar")
                        .setType(InstallProgressComponent.FileOperationTypes.COPY));
                macos.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setCustom(() -> {
                            MacOSAppUtil util = new MacOSAppUtil(ApplicationUtils.getName());
                            util.setIcon("ntify.icns");
                            util.setExecutableLocation(PublicValues.appLocation + "/NTify.jar");
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
        } else return null;
    }

    public InstallProgressComponent buildWindows() {
        InstallProgressComponent win = new InstallProgressComponent();
        try {
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(PublicValues.appLocation)
                    .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(new Resources().readToInputStream("ntify.ico"))
                    .setTo(PublicValues.appLocation + "/ntify.ico")
                    .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM));
            String jarPath = Initiator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setFrom(jarPath)
                    .setTo(PublicValues.appLocation + "/NTify.jar")
                    .setType(InstallProgressComponent.FileOperationTypes.COPY));
            win.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                    .setCustom(() -> {
                        try {
                            ShellLink shellLink = new ShellLink();
                            shellLink.setIconLocation(PublicValues.appLocation + "/ntify.ico");
                            shellLink.setCMDArgs("--setup-complete");
                            ShellLinkHelper helper = new ShellLinkHelper(shellLink);
                            helper.setLocalTarget("C", PublicValues.appLocation.replace("C:\\", "") + "/NTify.jar");
                            helper.saveTo(System.getProperty("user.home") + "/Desktop/NTify.lnk");
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
        if(Flags.linuxSupport) {
            InstallProgressComponent linux = new InstallProgressComponent();
            try {
                linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(PublicValues.fileslocation)
                        .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
                linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(PublicValues.appLocation)
                        .setType(InstallProgressComponent.FileOperationTypes.CREATEDIR));
                linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(new Resources().readToInputStream("ntify.ico"))
                        .setTo(PublicValues.appLocation + File.separator + "ntify.ico")
                        .setType(InstallProgressComponent.FileOperationTypes.COPYSTREAM));
                String jarPath = Initiator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setFrom(jarPath)
                        .setTo(PublicValues.appLocation + File.separator + "NTify.jar")
                        .setType(InstallProgressComponent.FileOperationTypes.COPY));
                linux.addFileOperation(new InstallProgressComponent.FileOperationBuilder()
                        .setCustom(() -> {
                            LinuxAppUtil util = new LinuxAppUtil(ApplicationUtils.getName());
                            util.setVersion(ApplicationUtils.getVersion());
                            util.setComment("Listen to Spotify");
                            util.setPath(PublicValues.appLocation);
                            util.setExecutableLocation("java -jar NTify.jar --setup-complete");
                            util.setIconlocation(PublicValues.appLocation + "/ntify.ico");
                            util.setCategories("Java", "Audio");
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
        }else return null;
    }
}
