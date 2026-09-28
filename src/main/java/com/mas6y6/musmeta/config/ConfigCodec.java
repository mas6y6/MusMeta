package com.mas6y6.musmeta.config;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;


public interface ConfigCodec<T> {

    void encode(T value, ConfigBuilder builder);

    T decode(ConfigBuilder builder);

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
