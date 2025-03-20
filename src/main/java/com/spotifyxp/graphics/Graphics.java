package com.spotifyxp.graphics;

import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.Resources;

import java.io.InputStream;

public enum Graphics {
    DOTS("dots"),
    HEART("heart"),
    HEARTFILLED("heartfilled"),
    HISTORY("history"),
    HISTORYSELECTED("historyselected"),
    MICROPHONE("microphone"),
    MICROPHONESELECTED("microphoneselected"),
    NOTHINGPLAYING("nothingplaying"),
    PLAYERPAUSE("playerpause"),
    PLAYERPlAY("playerplay"),
    PLAYERPLAYNEXT("playerplaynext"),
    PLAYERPLAYPREVIOUS("playerplayprevious"),
    REPEAT("repeat"),
    REPEATSELECTED("repeatselected"),
    SETTINGS("settings"),
    SHUFFLE("shuffle"),
    SHUFFLESELECTED("shuffleselected"),
    USER("user"),
    VOLUMEFULL("volumefull"),
    VOLUMEHALF("volumehalf"),
    VOLUMEMUTE("volumemute"),
    ACCOUNT("account"),
    TRACK("track"),
    VIDEO("video"),
    VIDEOSELECTED("videoselected"),
    ALBUM("album"),
    PLAYLIST("playlist"),
    SHOW("podcast"),
    MVERTICAL("mvertical"),
    MVERTICALSELECTED("mverticalselected"),
    CLOSE("close"),
    REFRESH("refresh");
    final String path;

    Graphics(String resourcePath) {
        String fullPath = "/icons/" + resourcePath;
        switch (resourcePath) {
            case "historyselected":
            case "heartfilled":
            case "microphonefilled":
            case "microphoneselected":
            case "repeatselected":
            case "shuffleselected":
            case "nothingplaying":
            case "videoselected":
            case "heart":
                path = fullPath + ".svg";
                break;
            default:
                if (PublicValues.theme.isLight()) {
                    path = fullPath + "dark.svg";
                } else {
                    path = fullPath + "white.svg";
                }
                break;
        }
    }

    public String getPath() {
        return path;
    }

    public InputStream getInputStream() {
        return new Resources().readToInputStream(path);
    }
}