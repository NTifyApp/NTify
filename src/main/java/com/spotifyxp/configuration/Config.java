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
package com.spotifyxp.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.theming.ThemeLoader;
import com.spotifyxp.utils.GraphicalMessage;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
    JsonObject properties;
    JsonObject modifiedAtRuntime;

    public Config() {
        properties = new JsonObject();
        PublicValues.themeLoader = new ThemeLoader();
        if (!new File(PublicValues.configfilepath).exists()) {
            for (ConfigValues value : ConfigValues.values()) {
                putConfigValue(properties, value);
            }
            if (!new File(PublicValues.fileslocation).exists()) {
                if (!new File(PublicValues.fileslocation).mkdir()) {
                    GraphicalMessage.sorryErrorExit("Failed creating important directory");
                }
            }
            if (!new File(PublicValues.fileslocation).exists()) {
                if (!new File(PublicValues.fileslocation).mkdir()) {
                    GraphicalMessage.sorryErrorExit("Failed creating important directory");
                }
            }
            try {
                if (!new File(PublicValues.configfilepath).createNewFile()) {
                    ConsoleLogging.error(PublicValues.language.translate("configuration.error.failedcreateconfig"));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            try {
                Files.write(Paths.get(PublicValues.configfilepath), properties.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                GraphicalMessage.sorryErrorExit("Failed creating important directory");
            }
        }
        try {
            properties = JsonParser.parseString(IOUtils.toString(Files.newInputStream(Paths.get(PublicValues.configfilepath)), Charset.defaultCharset())).getAsJsonObject();
            modifiedAtRuntime = JsonParser.parseString(IOUtils.toString(Files.newInputStream(Paths.get(PublicValues.configfilepath)), Charset.defaultCharset())).getAsJsonObject();
        } catch (IOException e) {
            GraphicalMessage.sorryErrorExit("Failed creating important directory");
        }
    }

    private void putConfigValue(JsonObject where, ConfigValues value) {
        switch (value.type) {
            case BOOLEAN:
                where.addProperty(value.name, (Boolean) value.defaultValue);
                break;
            case STRING:
                where.addProperty(value.name, (String) value.defaultValue);
                break;
            case INT:
                where.addProperty(value.name, (Integer) value.defaultValue);
                break;
            case CUSTOM:
                ((CustomConfigValue<?>) value.defaultValue).writeDefault();
        }
    }

    JsonObject getProperties() {
        return properties;
    }

    /**
     * Checks the config for errors<br>
     * If there are any they will be replaced with their default value
     */
    @SuppressWarnings("DuplicatedCode")
    public void checkConfig() {
        //Checks config for invalid values
        boolean foundInvalid = false;
        for (ConfigValues value : ConfigValues.values()) {
            if(value.defaultValue instanceof CustomConfigValue) {
                if (!properties.has(value.name)) {
                    try {
                        putConfigValue(properties, value);
                        ConsoleLogging.warning("Key '" + value.name + "' not found! Creating...");
                        foundInvalid = true;
                    } catch (NullPointerException e) {
                        ConsoleLogging.error("Failed creating key '" + value.name + "'!");
                    }
                    continue;
                }
                if (!(ConfigValueTypes.parse(properties.get(value.name)) == ((CustomConfigValue<?>)value.defaultValue).internalType())) {
                    ConsoleLogging.warning("Key '" + value.name + "' has the wrong value type: '" + ConfigValueTypes.parse(properties.get(value.name)) + "'! Resetting to default value...");
                    putConfigValue(properties, value);
                    foundInvalid = true;
                }
                if (!((CustomConfigValue<?>)value.defaultValue).check()) {
                    ConsoleLogging.warning("Key '" + value.name + "' has an invalid value! Resetting to default value...");
                    ((CustomConfigValue<?>)value.defaultValue).writeDefault();
                    foundInvalid = true;
                }
                continue;
            }
            //Handle some values that need extra checking
            if (!properties.has(value.name)) {
                try {
                    putConfigValue(properties, value);
                    ConsoleLogging.warning("Key '" + value.name + "' not found! Creating...");
                    foundInvalid = true;
                } catch (NullPointerException e) {
                    ConsoleLogging.error("Failed creating key '" + value.name + "'!");
                }
                continue;
            }
            if (!(ConfigValueTypes.parse(properties.get(value.name)) == value.type)) {
                ConsoleLogging.warning("Key '" + value.name + "' has the wrong value type: '" + ConfigValueTypes.parse(properties.get(value.name)) + "'! Resetting to default value...");
                putConfigValue(properties, value);
                foundInvalid = true;
            }
        }
        if (foundInvalid) {
            try {
                Files.write(Paths.get(PublicValues.configfilepath), properties.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                GraphicalMessage.sorryErrorExit("Failed creating important directory");
            }
        }
    }

    /**
     * Writes a new entry with the name and value to the config file
     *
     * @param name  Name of entry
     * @param value Value of entry
     */
    public void write(String name, Object value) {
        if (value instanceof Boolean) {
            properties.addProperty(name, (Boolean) value);
        } else if (value instanceof String) {
            properties.addProperty(name, (String) value);
        } else if (value instanceof Integer) {
            properties.addProperty(name, (Integer) value);
        } else if (value instanceof Double) {
            properties.addProperty(name, (Double) value);
        } else if (value instanceof Float) {
            properties.addProperty(name, (Float) value);
        } else if (value instanceof Long) {
            properties.addProperty(name, (Long) value);
        } else if (value instanceof Character) {
            properties.addProperty(name, (Character) value);
        }
    }

    public void save() {
        try {
            Files.write(Paths.get(PublicValues.configfilepath), modifiedAtRuntime.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.sorryErrorExit("Failed to write config");
        }
    }

    /**
     * Returns the value of the given entry inside the config as JsonElement
     *
     * @param name name of the entry
     * @return Object
     */
    public JsonElement getElement(String name) {
        JsonElement ret = properties.get(name);
        if (ret == null) {
            return null;
        }
        return ret;
    }

    /**
     * Returns the value of the given entry inside the config as String
     *
     * @param name name of the entry
     * @return String
     */
    public String getString(String name) {
        String ret = properties.get(name).getAsString();
        if (ret == null) {
            ret = "";
        }
        return ret;
    }

    /**
     * Returns the value of the given entry inside the config as Boolean
     *
     * @param name name of the entry
     * @return Boolean
     */
    public Boolean getBoolean(String name) {
        JsonElement value = properties.get(name);
        if (value == null) return null;
        return value.getAsBoolean();
    }

    /**
     * Returns the value of the given entry inside the config as Integer
     *
     * @param name name of the entry
     * @return Integer
     */
    public int getInt(String name) {
        JsonElement value = properties.get(name);
        if (value == null) return -1;
        return value.getAsInt();
    }
}
