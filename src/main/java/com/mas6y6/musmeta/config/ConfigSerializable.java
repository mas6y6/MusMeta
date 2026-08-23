package com.mas6y6.musmeta.config;

/**
 * Interface for custom classes that can be serialized to and deserialized from configuration
 * using a {@link ConfigBuilder} or {@link SubConfig}.
 * <p>
 * Implementing classes should implement {@link #serialize(ConfigBuilder)} (or {@link #serialize(SubConfig)})
 * to write their state into the builder, and provide a deserialization mechanism such as:
 * <ul>
 *     <li>A constructor accepting {@code (ConfigBuilder)} or {@code (SubConfig)}</li>
 *     <li>A static {@code configDeserialize(ConfigBuilder)} or {@code configDeserialize(SubConfig)} method</li>
 *     <li>A static {@code fromConfig(ConfigBuilder)} or {@code fromConfig(SubConfig)} method</li>
 *     <li>A static {@code deserialize(ConfigBuilder)} or {@code deserialize(SubConfig)} method</li>
 * </ul>
 */
public interface ConfigSerializable {

    /**
     * Serializes this object's state into the provided {@link ConfigBuilder}.
     *
     * @param builder the builder to populate with configuration key-values
     */
    void serialize(ConfigBuilder builder);

    /**
     * Serializes this object's state into the provided {@link SubConfig}.
     *
     * @param config the subconfig to populate
     */
    default void serialize(SubConfig config) {
        serialize(new ConfigBuilder(config));
    }
}
