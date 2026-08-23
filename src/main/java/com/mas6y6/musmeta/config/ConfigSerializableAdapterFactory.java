package com.mas6y6.musmeta.config;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;

/**
 * TypeAdapterFactory for classes implementing {@link ConfigSerializable} or registered with {@link ConfigCodec}.
 * Encodes objects into {@link ConfigBuilder} JSON representations and decodes them via constructors,
 * static factory methods, record components, or registered codecs.
 */
public class ConfigSerializableAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (!ConfigSerializable.class.isAssignableFrom(rawType) && !ConfigManager.hasCodec(rawType)) {
            return null;
        }

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                ConfigBuilder builder = new ConfigBuilder();
                if (value instanceof ConfigSerializable serializable) {
                    serializable.serialize(builder);
                } else if (ConfigManager.hasCodec(rawType)) {
                    ConfigCodec<T> codec = (ConfigCodec<T>) ConfigManager.getCodec(rawType);
                    codec.encode(value, builder);
                }

                TypeAdapters.JSON_ELEMENT.write(out, builder.toJsonObject());
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                JsonElement element = JsonParser.parseReader(in);
                ConfigBuilder builder;
                if (element.isJsonObject()) {
                    builder = ConfigBuilder.from(element.getAsJsonObject());
                } else if (element.isJsonPrimitive()) {
                    builder = new ConfigBuilder();
                    builder.set("value", element.getAsString());
                } else {
                    builder = new ConfigBuilder();
                }

                return deserialize(rawType, builder);
            }
        };
    }

    public static <T> T deserialize(Class<?> rawType, String value) {
        ConfigBuilder builder = new ConfigBuilder();
        builder.set("value", value);
        return deserialize(rawType, builder);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(Class<?> rawType, ConfigBuilder builder) {
        if (builder == null) {
            return null;
        }

        if (rawType.isInterface() || Modifier.isAbstract(rawType.getModifiers())) {
            throw new JsonParseException("Cannot instantiate abstract type or interface " + rawType.getName() + " directly.");
        }

        // 1. Registered Codec
        ConfigCodec<?> codec = ConfigManager.getCodec(rawType);
        if (codec != null) {
            return (T) ((ConfigCodec<Object>) codec).decode(builder);
        }

        // 2. Constructor(ConfigBuilder)
        try {
            Constructor<?> constructor = rawType.getDeclaredConstructor(ConfigBuilder.class);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(builder);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to construct " + rawType.getName() + " with (ConfigBuilder) constructor", e);
        }

        // 3. Constructor(SubConfig)
        try {
            Constructor<?> constructor = rawType.getDeclaredConstructor(SubConfig.class);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(builder.toSubConfig());
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to construct " + rawType.getName() + " with (SubConfig) constructor", e);
        }

        // 4. Static configDeserialize(ConfigBuilder) or configDeserialize(SubConfig)
        try {
            Method method = rawType.getDeclaredMethod("configDeserialize", ConfigBuilder.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static configDeserialize(ConfigBuilder) on " + rawType.getName(), e);
        }

        try {
            Method method = rawType.getDeclaredMethod("configDeserialize", SubConfig.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder.toSubConfig());
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static configDeserialize(SubConfig) on " + rawType.getName(), e);
        }

        // 5. Static fromConfig(ConfigBuilder) or fromConfig(SubConfig)
        try {
            Method method = rawType.getDeclaredMethod("fromConfig", ConfigBuilder.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static fromConfig(ConfigBuilder) on " + rawType.getName(), e);
        }

        try {
            Method method = rawType.getDeclaredMethod("fromConfig", SubConfig.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder.toSubConfig());
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static fromConfig(SubConfig) on " + rawType.getName(), e);
        }

        // 6. Static deserialize(ConfigBuilder) or deserialize(SubConfig)
        try {
            Method method = rawType.getDeclaredMethod("deserialize", ConfigBuilder.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static deserialize(ConfigBuilder) on " + rawType.getName(), e);
        }

        try {
            Method method = rawType.getDeclaredMethod("deserialize", SubConfig.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder.toSubConfig());
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static deserialize(SubConfig) on " + rawType.getName(), e);
        }

        // 7. Static valueOf(ConfigBuilder) or valueOf(SubConfig)
        try {
            Method method = rawType.getDeclaredMethod("valueOf", ConfigBuilder.class);
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (T) method.invoke(null, builder);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to invoke static valueOf(ConfigBuilder) on " + rawType.getName(), e);
        }

        // 8. Java Record
        if (rawType.isRecord()) {
            try {
                RecordComponent[] components = rawType.getRecordComponents();
                Class<?>[] paramTypes = new Class<?>[components.length];
                Object[] args = new Object[components.length];
                for (int i = 0; i < components.length; i++) {
                    paramTypes[i] = components[i].getType();
                    String name = components[i].getName();
                    args[i] = builder.get(name, components[i].getGenericType());
                }
                Constructor<?> constructor = rawType.getDeclaredConstructor(paramTypes);
                constructor.setAccessible(true);
                return (T) constructor.newInstance(args);
            } catch (Exception e) {
                throw new JsonParseException("Failed to construct record " + rawType.getName() + " from ConfigBuilder", e);
            }
        }

        // 9. No-arg constructor + optional instance deserialize(ConfigBuilder)
        try {
            Constructor<?> noArg = rawType.getDeclaredConstructor();
            noArg.setAccessible(true);
            Object instance = noArg.newInstance();

            Method method = null;
            try {
                method = rawType.getDeclaredMethod("configDeserialize", ConfigBuilder.class);
            } catch (NoSuchMethodException e) {
                try {
                    method = rawType.getDeclaredMethod("deserialize", ConfigBuilder.class);
                } catch (NoSuchMethodException ignored) {
                }
            }

            if (method != null && !Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                Object result = method.invoke(instance, builder);
                if (result != null && rawType.isInstance(result)) {
                    return (T) result;
                }
                return (T) instance;
            }
            return (T) instance;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new JsonParseException("Failed to instantiate " + rawType.getName() + " with no-arg constructor", e);
        }

        // 10. Fallback for string-based constructor if value property present
        if (builder.has("value")) {
            String strVal = builder.getString("value");
            try {
                Constructor<?> strCtor = rawType.getDeclaredConstructor(String.class);
                strCtor.setAccessible(true);
                return (T) strCtor.newInstance(strVal);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new JsonParseException("Failed to construct " + rawType.getName() + " with (String) constructor", e);
            }
        }

        throw new JsonParseException("Cannot deserialize " + rawType.getName() + " from ConfigBuilder. " +
                "Please provide a (ConfigBuilder) constructor, static configDeserialize/fromConfig/deserialize method, or register a ConfigCodec.");
    }
}
