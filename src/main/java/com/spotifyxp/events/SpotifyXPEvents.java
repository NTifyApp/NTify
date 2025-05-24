package com.spotifyxp.events;

public enum SpotifyXPEvents {
    queueUpdate("queueUpdate", "Fires when the player queue updates"),
    queueAdvance("queueAdvance", "Fires when the player queue advances"),
    queueRegress("queueRegress", "Fires when the player queue regresses"),
    playerLockRelease("playerLockRelease", "Fires when the player finished processing the metadata"),
    playerSeekedForwards("playerseekedforwards", "Fires when the user seeked the track forwards"),
    playerSeekedBackwards("playerseekedbackwards", "Fires when the user seeked the track backwards"),
    onFrameReady("frameReady", "Fires when the main JFrame finished building itself (before opening)"),
    onFrameVisible("frameVisible", "Fires when the main JFrame is visible"),
    trackNext("trackNext", "Fires when next track plays"),
    trackLoad("trackLoad", "Fires when the next track loads"),
    trackLoadFinished("trackLoadFinished", "Fires when the track loading is finished"),
    injectorAPIReady("injectorAPIReady", "Fires when the injector api class has finished initializing"),
    internetConnectionDropped("internetConDrop", "Fires when the internet connection drops"),
    internetConnectionReconnected("internetConRec", "Fires when the internet gets reconnected"),
    apikeyrefresh("apikeyrefresh", "Fires when the api key refreshes"),
    addtoqueue("addToQueue", "Fires when a track should be added to the queue"),
    recalculateSizes("recalcSizes", "Fires when the sizes of the JComponent's should be recalculated"),
    playerresume("playerresume", "Fires when the player resumes"),
    playerpause("playerpause", "Fires when the player is paused");

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private final String name;
    private final String description;

    SpotifyXPEvents(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
