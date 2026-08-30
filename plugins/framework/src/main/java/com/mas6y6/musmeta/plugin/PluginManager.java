package com.mas6y6.musmeta.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mas6y6.musmeta.plugin.api.Plugin;
import com.mas6y6.musmeta.plugin.api.PluginContext;
import com.mas6y6.musmeta.plugin.api.PluginDescriptor;
import com.mas6y6.musmeta.plugin.api.PluginState;
import com.mas6y6.musmeta.plugin.mixin.PluginMixinManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("musmeta.plugin");

    private static final String PLUGIN_DESCRIPTOR = "plugin.json";

    private final Path pluginsDirectory;
    private final Gson gson = new Gson();
    private final List<PluginContainer> containers = new ArrayList<>();
    private boolean booted;
    private boolean shutDown;

    public PluginManager(Path pluginsDirectory) {
        this.pluginsDirectory = pluginsDirectory;
    }

    public static Path defaultPluginsDirectory() {
        String configured = System.getProperty("musmeta.plugins");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.home"), ".musmeta", "plugins");
    }

    public Path pluginsDirectory() {
        return pluginsDirectory;
    }

    public List<PluginContainer> containers() {
        return List.copyOf(containers);
    }

    public boolean hasPlugins() {
        return !containers.isEmpty();
    }

    public void discover() {
        try {
            Files.createDirectories(pluginsDirectory);
        } catch (IOException e) {
            LOGGER.error("Cannot create plugins directory {}: {}", pluginsDirectory, e.getMessage());
            return;
        }

        List<Path> jars;
        try (Stream<Path> paths = Files.list(pluginsDirectory)) {
            jars = paths
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            LOGGER.error("Cannot scan plugins directory {}: {}", pluginsDirectory, e.getMessage());
            return;
        }

        for (Path jar : jars) {
            loadPlugin(jar);
        }
        resolveDependencies();
    }

    private void loadPlugin(Path jar) {
        try {
            PluginDescriptor descriptor = readDescriptor(jar);
            PluginClassLoader classLoader = new PluginClassLoader(descriptor.id(), jar, PluginManager.class.getClassLoader());
            containers.add(new PluginContainer(descriptor, jar, classLoader));
            LOGGER.info("Discovered plugin '{}' v{} from {}", descriptor.id(), descriptor.version(), jar.getFileName());
        } catch (Exception e) {
            LOGGER.error("Skipping invalid plugin {}: {}", jar.getFileName(), e.getMessage());
        }
    }

    private PluginDescriptor readDescriptor(Path jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry(PLUGIN_DESCRIPTOR);
            if (entry == null) {
                throw new PluginLoadException("missing " + PLUGIN_DESCRIPTOR);
            }
            try (InputStreamReader reader = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                try {
                    return gson.fromJson(reader, PluginDescriptor.class);
                } catch (JsonParseException e) {
                    throw new PluginLoadException("malformed " + PLUGIN_DESCRIPTOR, e);
                }
            }
        }
    }

    private void resolveDependencies() {
        Map<String, PluginContainer> byId = new HashMap<>();
        for (PluginContainer container : containers) {
            byId.put(container.id(), container);
        }
        for (PluginContainer container : containers) {
            for (String dependencyId : container.descriptor().dependencies()) {
                PluginContainer dependency = byId.get(dependencyId);
                if (dependency == null) {
                    LOGGER.error("Plugin '{}' depends on unknown plugin '{}'; skipping plugin.",
                            container.id(), dependencyId);
                    removeContainer(container);
                    break;
                }
                try {
                    container.classLoader().link(dependency.source());
                } catch (IOException e) {
                    LOGGER.error("Cannot link '{}' to dependency '{}': {}",
                            container.id(), dependencyId, e.getMessage());
                }
            }
        }
    }

    private void removeContainer(PluginContainer container) {
        containers.remove(container);
        try {
            container.classLoader().close();
        } catch (IOException ignored) {
        }
    }

    public void boot() {
        if (booted) {
            return;
        }
        booted = true;
        try {
            if (containers.isEmpty()) {
                LOGGER.info("No plugins to boot (plugins directory: {}).", pluginsDirectory);
                return;
            }

            List<PluginContainer> ordered = topologicalSort();

            boolean needMixins = false;
            for (PluginContainer container : ordered) {
                if (!container.descriptor().mixins().isEmpty()) {
                    needMixins = true;
                    break;
                }
            }

            if (needMixins) {
                PluginMixinManager mixinManager = PluginMixinManager.getInstance();
                mixinManager.boot();
                for (PluginContainer container : ordered) {
                    for (String config : container.descriptor().mixins()) {
                        mixinManager.registerConfig(config, container.classLoader());
                    }
                }
                mixinManager.finish();
            }

            for (PluginContainer container : ordered) {
                instantiate(container);
            }
            for (PluginContainer container : ordered) {
                runBoot(container);
            }
            for (PluginContainer container : ordered) {
                enable(container);
            }
        } catch (Throwable t) {
            LOGGER.error("Plugin boot failed: {}", t.getMessage(), t);
        }
    }

    public void shutdown() {
        if (!shutDown) {
            shutDown = true;
        }
        for (PluginContainer container : containers) {
            try {
                Plugin plugin = container.plugin();
                if (plugin != null) {
                    plugin.onDisable();
                    container.setState(PluginState.DISABLED);
                }
            } catch (Throwable t) {
                LOGGER.error("Failed to disable plugin '{}': {}", container.id(), t.getMessage());
            } finally {
                try {
                    container.classLoader().close();
                } catch (IOException ignored) {
                }
            }
        }
        containers.clear();
        LOGGER.info("All plugins shut down.");
    }

    private void instantiate(PluginContainer container) {
        String main = container.descriptor().main();
        if (main == null || main.isBlank()) {
            return;
        }
        try {
            Class<?> mainClass = Class.forName(main, true, container.classLoader());
            if (!Plugin.class.isAssignableFrom(mainClass)) {
                throw new PluginLoadException("main class is not a " + Plugin.class.getName() + " subclass");
            }
            Plugin instance = (Plugin) mainClass.getDeclaredConstructor().newInstance();
            instance.setContext(new PluginContext(container.descriptor(), dataDirectory(container.id())));
            container.setPlugin(instance);
            container.setState(PluginState.LOADED);
        } catch (Exception e) {
            container.setState(PluginState.FAILED);
            LOGGER.error("Failed to instantiate plugin '{}': {}", container.id(), e.getMessage());
        }
    }

    private Path dataDirectory(String pluginId) {
        Path data = pluginsDirectory.getParent().resolve("data").resolve(pluginId);
        try {
            Files.createDirectories(data);
        } catch (IOException e) {
            LOGGER.error("Cannot create data directory {}: {}", data, e.getMessage());
        }
        return data;
    }

    private void runBoot(PluginContainer container) {
        Plugin plugin = container.plugin();
        if (plugin == null) {
            return;
        }
        try {
            plugin.onBoot();
            LOGGER.info("Plugin '{}' booted.", container.id());
        } catch (Throwable t) {
            container.setState(PluginState.FAILED);
            LOGGER.error("Plugin '{}' failed during onBoot: {}", container.id(), t.getMessage(), t);
        }
    }

    private void enable(PluginContainer container) {
        Plugin plugin = container.plugin();
        if (plugin == null || container.state() == PluginState.FAILED) {
            return;
        }
        try {
            plugin.onEnable();
            container.setState(PluginState.ENABLED);
            LOGGER.info("Plugin '{}' enabled.", container.id());
        } catch (Throwable t) {
            container.setState(PluginState.FAILED);
            LOGGER.error("Plugin '{}' failed during onEnable: {}", container.id(), t.getMessage(), t);
        }
    }

    private List<PluginContainer> topologicalSort() {
        Map<String, PluginContainer> byId = new HashMap<>();
        for (PluginContainer container : containers) {
            byId.put(container.id(), container);
        }
        List<PluginContainer> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        for (PluginContainer container : containers) {
            visitDependencies(container.id(), byId, visited, ordered, stack);
        }
        return ordered;
    }

    private void visitDependencies(String id, Map<String, PluginContainer> byId, Set<String> visited,
                                   List<PluginContainer> ordered, Deque<String> stack) {
        if (visited.contains(id)) {
            return;
        }
        if (stack.contains(id)) {
            LOGGER.error("Plugin dependency cycle detected at '{}'; cycle ignored.", id);
            return;
        }
        stack.push(id);
        PluginContainer container = byId.get(id);
        if (container != null) {
            for (String dependencyId : container.descriptor().dependencies()) {
                if (byId.containsKey(dependencyId)) {
                    visitDependencies(dependencyId, byId, visited, ordered, stack);
                }
            }
        }
        stack.pop();
        visited.add(id);
        if (container != null) {
            ordered.add(container);
        }
    }
}