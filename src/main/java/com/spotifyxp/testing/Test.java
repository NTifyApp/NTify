package com.spotifyxp.testing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test {
    public static void main(String[] args) throws Exception {
        String data = IOUtils.toString(Files.newInputStream(Paths.get("searchv2.txt")));

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        JsonReader reader = new JsonReader(new StringReader(data));
        reader.setLenient(false);
        UnofficialSpotifyAPI.SearchV2Response response = UnofficialSpotifyAPI.SearchV2Response.fromJsonObject(gson.fromJson(reader, JsonObject.class));

        ensureNoNulls(response, "");
    }

    public static void ensureNoNulls(Object obj, String path) throws IllegalAccessException {
        if (obj == null) {
            throw new IllegalStateException("Missing required data at: " + path);
        }

        // Skip primitive types and common "leaf" types
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return;
        }

        // Handle Collections (Lists, etc.)
        if (obj instanceof Collection) {
            int i = 0;
            for (Object item : (Collection<?>) obj) {
                ensureNoNulls(item, path + "[" + (i++) + "]");
            }
            return;
        }

        // Handle Maps
        if (obj instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                ensureNoNulls(entry.getValue(), path + "['" + entry.getKey() + "']");
            }
            return;
        }

        // Recursively check all declared fields in the class
        for (Field field : clazz.getDeclaredFields()) {
            // Skip synthetic fields (like 'this$0' in inner classes)
            if (field.isSynthetic()) continue;
            if (!field.isAccessible()) continue; // Private methods are nullable

            Object value = field.get(obj);
            String currentPath = path.isEmpty() ? field.getName() : path + "." + field.getName();

            if (value == null && field.getAnnotations().length == 0) {
                throw new IllegalStateException("Field '" + currentPath + "' is null/missing in JSON.");
            }

            // Recursively check the value
            ensureNoNulls(value, currentPath);
        }
    }
}
