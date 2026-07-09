/*
 * Copyright [2026] [Gianluca Beil]
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

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.GraphicalMessage;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Config {
    /**
     * Defines a config value
     * <br>
     * An empty array in possibleValues is treated as "Any" meaning that any value is possible
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface HiddenConfigValue {
        String id();
        String category();
        String translationKey() default "";
        Class<? extends ConfigValueProvider> allowedValues() default DefaultConfigValueProvider.class;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface CheckBox {
        String id();
        String category();
        String translationKey() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Text {
        String id();
        String category();
        String translationKey() default "";
        int characterLimit() default Integer.MAX_VALUE;
        boolean allowEmpty() default true;
        Class<? extends ConfigValueProvider> allowedValues() default DefaultConfigValueProvider.class;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Numbers {
        String id();
        String category();
        String translationKey() default "";
        int min() default 0;
        int max() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Dropdown {
        String id();
        String category();
        String translationKey() default "";
        Class<? extends ConfigValueProvider<String>> values();
        Class<? extends ConfigValueProvider<?>> mapping() default DefaultIntMappingProvider.class;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface CustomComponent {
        String id();

        String category();

        String translationKey() default "";

        Class<? extends CustomComponentCallback> component();

        Class<? extends ConfigValueProvider<String>> values() default DefaultConfigValueProvider.class;

        Class<? extends ConfigValueProvider<?>> mapping() default DefaultIntMappingProvider.class;
    }

    private static class DefaultIntMappingProvider implements ConfigValueProvider<Integer> {
        @Override
        public List<Integer> values() {
            return Collections.emptyList();
        }
    }

    private static class DefaultConfigValueProvider implements ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            return new ArrayList<>();
        }
    }

    public interface ConfigValueProvider<Type> {
        List<Type> values();
    }

    public interface CustomComponentCallback {
        JComponent component();
        void onSave(JComponent component) throws NoSuchFieldException;
    }

    /**
     * This class prevents config values from changing at runtime
     */
    public static class RuntimeConfig<Type> {
        private final String configPath;
        private final Object configInstance;
        private final Gson gson;
        private final Class<Type> clazz;
        private final Object defaultInstance;
        private final Object unmodifiedInstance;

        protected RuntimeConfig(String configPath, Class<Type> clazz, Gson gson) throws IOException, IllegalAccessException, InstantiationException, NoSuchFieldException {
            this.configPath = configPath;
            this.gson = gson;
            this.clazz = clazz;

            Object clazzInstance = clazz.newInstance();
            Path pathToConfig = Paths.get(configPath);

            gson = gson.newBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(clazz, new ConfigDeserializer())
                    .registerTypeAdapter(clazz, new ConfigSerializer())
                    .create();

            if (!new File(configPath).exists()) {
                if (!new File(configPath).getParentFile().exists()) {
                    if (!new File(configPath).getParentFile().mkdir()) {
                        GraphicalMessage.sorryErrorExit("Failed creating important directory");
                    }
                }
                try {
                    if (!new File(configPath).createNewFile()) {
                        ConsoleLogging.error("Can't create config file");
                    }
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
                try {
                    Files.write(pathToConfig, gson.toJson(clazzInstance).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    ConsoleLogging.Throwable(e);
                    GraphicalMessage.sorryErrorExit("Failed to write config");
                }
            }

            this.configInstance = gson.fromJson(
                    new JsonReader(new FileReader(pathToConfig.toString())),
                    clazz
            );

            this.unmodifiedInstance = gson.fromJson(
                    new JsonReader(new FileReader(pathToConfig.toString())),
                    clazz
            );

            defaultInstance = clazz.newInstance();
        }

        private class ConfigSerializer implements JsonSerializer<Object> {
            @Override
            public JsonElement serialize(Object src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
                JsonObject object = new JsonObject();

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);

                    String key = getConfigId(field);
                    if (key == null) {
                        // Field was not annotated. I just assume that it's not a config value
                        continue;
                    }

                    Object value;
                    try {
                        value = field.get(src);
                    } catch (IllegalAccessException e) {
                        ConsoleLogging.error("Failed to get config value for: " + field.getName());
                        continue;
                    }

                    fillJsonObjectWith(object, key, field, value);
                }

                return object;
            }
        }

        private class ConfigDeserializer implements JsonDeserializer<Object> {
            @Override
            public Object deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {

                JsonObject obj = json.getAsJsonObject();
                Object instance;
                try {
                    instance = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);

                    String key = getConfigId(field);
                    if (key == null) {
                        // Field was not annotated. I just assume that it's not a config value
                        continue;
                    }

                    if (obj.has(key)) {
                        try {
                            Object value = context.deserialize(obj.get(key), field.getType());
                            field.set(instance, value);
                        } catch (Exception e) {
                            throw new JsonParseException("Failed to set field: " + field.getName(), e);
                        }
                    }
                }

                return instance;
            }
        }

        private String getConfigId(Field field) {
            for(Annotation annotation : field.getAnnotations()) {
                if (annotation.annotationType().getCanonicalName().startsWith("com.spotifyxp.configuration.Config")) {
                    // All annotations have an id. The best way is to get the id via reflect
                    try {
                        return (String) annotation.annotationType().getMethod("id").invoke(annotation);
                    }catch (Exception e) {
                        ConsoleLogging.error("Failed to get config id for field: " + field.getName());
                        ConsoleLogging.Throwable(e);
                    }
                }
            }

            return null;
        }

        public void write(String name, Object value, boolean catchException) {
            try {
                clazz.getField(name).set(configInstance, value);
                save();
            } catch (IllegalAccessException | NoSuchFieldException e) {
                if (!catchException) throw new RuntimeException(e);

                ConsoleLogging.Throwable(e);
            }
        }

        public void write(String name, Object value) {
            write(name, value, true);
        }

        public void save() {
            try {
                JsonObject configJSON = new JsonObject();
                for (Field field : clazz.getDeclaredFields()) {
                    Object value = field.get(configInstance);
                    Annotation[] annotations = field.getAnnotations();
                    if (annotations.length == 0) {
                        // Field was not annotated. I just assume that it's not a config value
                        continue;
                    }
                    Annotation annotationInstance = annotations[0];
                    String id = (String) annotations[0].annotationType().getMethod("id").invoke(annotationInstance);
                    fillJsonObjectWith(configJSON, id, field, value);
                }
                Files.write(Paths.get(configPath), gson.newBuilder().setPrettyPrinting().create().toJson(configJSON).getBytes(StandardCharsets.UTF_8));
            } catch (IOException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                ConsoleLogging.Throwable(e);
                GraphicalMessage.sorryErrorExit("Failed to write config");
            }
        }

        private void fillJsonObjectWith(JsonObject json, String id, Field field, Object value) {
            if (field.getType().equals(Boolean.class) || field.getType().equals(Boolean.TYPE)) {
                json.addProperty(id, (Boolean) value);
            } else if (field.getType().equals(String.class)) {
                json.addProperty(id, (String) value);
            } else if (field.getType().equals(Integer.class) || field.getType().equals(Integer.TYPE)) {
                json.addProperty(id, (Integer) value);
            }
        }

        public List<String> getAllowedValuesFor(String name) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, UnsupportedOperationException {
            Map<Class<?>, String> allowedValueFieldMap = new HashMap<Class<?>, String>() {{
                put(HiddenConfigValue.class, "allowedValues");
                put(Text.class, "allowedValues");
                put(Dropdown.class, "values");
            }};

            Field field = clazz.getField(name);
            Annotation[] annotations = field.getAnnotations();
            if (annotations.length == 0)
                throw new UnsupportedOperationException("Config value was not annotated");

            Annotation annotationInstance = annotations[0];
            Class<? extends Annotation> annotationType = annotationInstance.annotationType();
            String methodName = allowedValueFieldMap.get(annotationType);
            if (methodName == null)
                throw new UnsupportedOperationException("Unsupported annotation: " + annotationType);
            Method method = annotationType.getMethod(methodName);
            method.setAccessible(true);
            Class<?> providerClass = (Class<?>) method.invoke(annotationInstance);
            Constructor<?> constructor = providerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            ConfigValueProvider provider = (ConfigValueProvider) constructor.newInstance();
            return provider.values();
        }

        @SuppressWarnings("unchecked")
        public <ReturnType> ReturnType getDefaultFor(String name) throws NoSuchFieldException, IllegalAccessException {
            return (ReturnType) clazz.getField(name).get(defaultInstance);
        }

        @SuppressWarnings("unchecked")
        public Type getFields() {
            return (Type) unmodifiedInstance;
        }

        public List<?> getMappingValuesFor(String name) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            Map<Class<?>, String> allowedValueFieldMap = new HashMap<Class<?>, String>() {{
                put(Dropdown.class, "mapping");
            }};

            Field field = clazz.getField(name);
            Annotation[] annotations = field.getAnnotations();
            if (annotations.length == 0)
                throw new UnsupportedOperationException("Config value was not annotated");

            Annotation annotationInstance = annotations[0];
            Class<? extends Annotation> annotationType = annotationInstance.annotationType();
            String methodName = allowedValueFieldMap.get(annotationType);
            if (methodName == null)
                throw new UnsupportedOperationException("Unsupported annotation: " + annotationType);
            Method method = annotationType.getMethod(methodName);
            method.setAccessible(true);
            Class<?> providerClass = (Class<?>) method.invoke(annotationInstance);
            Constructor<?> constructor = providerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            ConfigValueProvider provider = (ConfigValueProvider) constructor.newInstance();
            return provider.values();
        }
    }

    public Config() {
    }

    public static <Type> RuntimeConfig<Type> newInstance(String configPath, Class<Type> configValues, @Nullable Gson gson) throws IOException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        return new RuntimeConfig<>(configPath, configValues, gson == null ? new Gson() : gson);
    }
}
