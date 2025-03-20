package com.spotifyxp.injector;

import java.util.ArrayList;

public interface InjectorInterface {
    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default String getIdentifier() {return "";}

    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default String getVersion() {return "";}

    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default String getAuthor() {return "";}

    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default ArrayList<Injector.Dependency> getDependencies() {
        return new ArrayList<>();
    }

    void init();
}
