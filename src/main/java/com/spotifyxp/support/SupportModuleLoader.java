package com.spotifyxp.support;

import com.spotifyxp.Flags;

import java.util.ArrayList;

public class SupportModuleLoader {
    private static final ArrayList<SupportModule> supportModules = new ArrayList<>();

    public SupportModuleLoader() {
        if(Flags.linuxSupport) supportModules.add(new LinuxSupportModule());
        if(Flags.macosSupport) supportModules.add(new MacOSXSupportModule());
    }

    public void loadModules() {
        for (SupportModule module : supportModules) {
            if (module.getOSName().equals(System.getProperty("os.name"))) {
                module.run();
            }
        }
    }
}
