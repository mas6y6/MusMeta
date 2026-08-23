package com.mas6y6.musmeta.config;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A Codec interface for bidirectional conversion between a custom class {@code T}
 * and a {@link ConfigBuilder}.
 *
 * @param <T> the type of object handled by this codec
 */
public interface ConfigCodec<T> {

    /**
     * Encodes the given object into the config builder.
     *
     * @param value   the object to encode
     * @param builder the target config builder
     */
    void encode(T value, ConfigBuilder builder);

    /**
     * Decodes an object from the config builder.
     *
     * @param builder the source config builder
     * @return the decoded object instance
     */
    T decode(ConfigBuilder builder);

    /**
     * Creates a codec from an encoder and decoder function.
     */
    static <T> ConfigCodec<T> of(BiConsumer<T, ConfigBuilder> encoder, Function<ConfigBuilder, T> decoder) {
        Objects.requireNonNull(encoder, "Encoder cannot be null");
        Objects.requireNonNull(decoder, "Decoder cannot be null");
        return new ConfigCodec<>() {
            @Override
            public void encode(T value, ConfigBuilder builder) {
                encoder.accept(value, builder);
            }

            @Override
            public T decode(ConfigBuilder builder) {
                return decoder.apply(builder);
            }
        };
    }

    /**
     * Creates a builder to construct a {@link ConfigCodec}.
     */
    static <T> CodecBuilder<T> builder(Function<ConfigBuilder, T> factory) {
        return new CodecBuilder<>(factory);
    }

    class CodecBuilder<T> {
        private final Function<ConfigBuilder, T> factory;
        private BiConsumer<T, ConfigBuilder> encoder = (val, builder) -> {};

        public CodecBuilder(Function<ConfigBuilder, T> factory) {
            this.factory = Objects.requireNonNull(factory, "Factory cannot be null");
        }

        public CodecBuilder<T> withEncoder(BiConsumer<T, ConfigBuilder> encoder) {
            this.encoder = Objects.requireNonNull(encoder, "Encoder cannot be null");
            return this;
        }

        public ConfigCodec<T> build() {
            return ConfigCodec.of(encoder, factory);
        }
    }
}
