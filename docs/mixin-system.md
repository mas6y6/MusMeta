# Mixin System

MusMeta uses [SpongePowered Mixin](https://github.com/SpongePowered/Mixin) to let
plugins modify/observe the core application's classes at runtime. This page
explains how the system is wired up, both on the build side (Gradle) and at
runtime (class loading / transformation), so you can understand it and reuse the
pattern elsewhere.

---

## 1. What it does

A plugin ships zero or more `.mixins.json` config files. Each config declares
mixin classes (e.g. `UtilsMixin`) that target core MusMeta classes (e.g.
`com.mas6y6.musmeta.utils.Utils`) and inject code into them. When the app
boots, it reads each plugin's configs and applies the mixins to the target
classes **before** the plugin itself is instantiated.

---

## 2. Build-side wiring (Gradle)

The build does **not** run an annotation processor and has **no** dedicated
mixin Gradle task. It only supplies the mixin + ASM dependencies and lets the
runtime do the work.

### Versions

Defined centrally in `gradle.properties`:

```properties
mixin=0.8.5-SNAPSHOT
asm=9.9.1
```

The `settings.gradle` `pluginManagement` block adds the SpongePowered Maven
repo, which is where the mixin SNAPSHOTs live.

### Dependencies

- **Framework** (`plugins/framework/build.gradle`) — the runtime mixin deps go
  here as `implementation` so they land in the final app:
  ```groovy
  implementation("net.fabricmc:sponge-mixin:$mixin")     // mixin library
  implementation("org.ow2.asm:asm:$asm")                // mixin's bytecode engine
  implementation("org.ow2.asm:asm-tree:$asm")
  implementation("org.ow2.asm:asm-analysis:$asm")
  implementation("org.ow2.asm:asm-commons:$asm")
  implementation("org.ow2.asm:asm-util:$asm")
  ```
  (Guava and Gson are also pulled in because mixin needs them.)

- **Plugin template** (`plugin-template/build.gradle`) — a standalone skeleton for
  external plugins. Mixin + ASM + the framework are declared as `compileOnly`; the
  framework comes from `mavenLocal()` (published via
  `./gradlew :plugins:framework:publishToMavenLocal`). The plugin jar is *not*
  self-contained for mixin use; it relies on the framework providing mixin at
  runtime:
  ```groovy
  compileOnly "com.mas6y6:framework:$frameworkVersion"
  compileOnly "net.fabricmc:sponge-mixin:$mixinVersion"
  compileOnly "org.ow2.asm:asm:$asmVersion"
  compileOnly "org.ow2.asm:asm-tree:$asmVersion"
  compileOnly "org.slf4j:slf4j-api:$slf4jVersion"
  ```

- **Root app** (`build.gradle`) just depends on the framework
  (`implementation project(':plugins:framework')`), which transitively ships mixin.

The `publishPlugin` task in the template copies the built jar into the plugins
directory (`~/.musmeta/plugins` by default, overridable with `musmeta.plugins`).
The template lives outside the root Gradle build, so its jar is only synced when
you run `publishPlugin` from inside `plugin-template`.

---

## 3. Declaring mixins in a plugin

A plugin declares its mixin configs in `plugin.json`:

```json
{
  "id": "hello-plugin",
  "main": "com.example.musmeta.HelloPlugin",
  "mixins": ["hello.mixins.json"],
  "dependencies": []
}
```

The `mixins` field is a `List<String>` on
`PluginDescriptor` (`plugins/framework/.../plugin/api/PluginDescriptor.java`),
defaulting to an empty list if absent.

Each referenced config is a normal SpongePowered mixin config placed in the
plugin's resources, e.g. `plugin-template/src/main/resources/hello.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.example.musmeta.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": ["ExampleMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

The mixin classes live in the package named by `"package"`. Example
`ExampleMixin` (`plugin-template/.../mixin/ExampleMixin.java`) injects a log
at the `RETURN` of `Utils.isRunningAsRoot(...)`:

```java
@Mixin(targets = "com.mas6y6.musmeta.utils.Utils")
public abstract class ExampleMixin {

    @Inject(method = "isRunningAsRoot", at = @At("RETURN"), remap = false)
    private static void hello$afterIsRunningAsRoot(CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info("Utils.isRunningAsRoot() intercepted ...");
    }
}
```

---

## 4. Runtime wiring (class loading)

Mixin works by transforming class bytecode before the JVM defines the class.
For that, MusMeta builds a custom classloader chain.

### KnotClassLoader

`com.mas6y6.musmeta.launch.KnotClassLoader` is a custom `ClassLoader` created in
`Launcher.main(...)` and set as the thread's context classloader. It:

- Loads `com.mas6y6.musmeta.*` classes itself so it can intercept and transform
  their bytecode.
- Uses a single `findLoadedClass` result so each class is defined exactly once.
- Excludes the `launch.` and `plugin.mixin.` packages from its own loading
  (`KNOT_EXCLUDED_PREFIXES`), letting the system classloader handle them.
- Exposes `installTransformer(LaunchTransformer)` to register the mixin
  transformer.

### LaunchTransformer bridge

`LaunchTransformer` is a functional interface
(`byte[] transform(String name, byte[] bytes)`), the seam between the mixin
transformer and the classloader. `MixinLaunchBridge` adapts the SpongePowered
`MixinTransformer.transformClassBytes` into that seam via reflection.

---

## 5. The mixin host runtime

MusMeta implements the SpongePowered `IMixinService` SPI so mixin can run
without a Minecraft-style launch. All classes live in
`com.mas6y6.musmeta.plugin.mixin` in the framework:

- **`PluginMixinManager`** — the singleton orchestrator:
  - `boot()` → `MixinService.boot()` + `MixinBootstrap.init()`.
  - `registerConfig(configFile, pluginLoader)` → temporarily sets the plugin's
    classloader as context, then `Mixins.addConfiguration(configFile)`.
  - `finish()` → creates the `MixinTransformer` via reflection, wraps it in a
    `MixinLaunchBridge`, and installs it into `KnotClassLoader`.
- **`MusMetaMixinService`** — `IMixinService` impl registered via
  `META-INF/services/org.spongepowered.asm.service.IMixinService`. Provides a
  class provider and bytecode provider, and aggregates all loaders (app context,
  the framework itself, and every registered plugin loader).
- **`MusMetaClassProvider`** & **`MusMetaBytecodeProvider`** — locate classes /
  read ASM `ClassNode`s from across all loaders.
- **`PluginClassLoaderRegistry`** — a thread-safe list of plugin classloaders the
  service can search.
- **`MusMetaBlackboard`** — an `IGlobalPropertyService` (mixin's global
  property map), registered via
  `META-INF/services/org.spongepowered.asm.service.IGlobalPropertyService`.

The key trick is that mixin needs to resolve classes/bytecode/resources across
*many* classloaders (app + framework + each plugin), so the service iterates all
of them instead of assuming a single classpath.

---

## 6. Boot ordering

`PluginManager.boot()` (`.../plugin/PluginManager.java`):

1. Topologically sorts plugins (respecting `dependencies`).
2. Scans for any plugin declaring a non-empty `mixins` list.
3. If any does:
   - `PluginMixinManager.boot()`
   - `registerConfig(config, pluginLoader)` for every config of every plugin
   - `PluginMixinManager.finish()` to install the transformer
4. Only **then** instantiates / boots / enables the plugins.

This ordering matters: mixins must be active before plugin code runs so target
classes (which plugins might also use) are already transformed.

---

## 7. Reusing the pattern elsewhere

To host SpongePowered Mixin in another non-Minecraft project, you need roughly
the same pieces:

1. **A transforming classloader** that reads class bytes, passes them through a
   transformer, then `defineClass`-es them (like `KnotClassLoader`).
2. **An `IMixinService` implementation** (like `MusMetaMixinService`)
   registered via `META-INF/services`, providing a class provider and bytecode
   provider that can see all relevant classloaders.
3. **Service metadata**: a `META-INF/services` registration and a blackboard
   (`IGlobalPropertyService`).
4. **Config registration + transformer install**: call `MixinService.boot()` +
   `MixinBootstrap.init()`, add configs with `Mixins.addConfiguration(...)`,
   then create the transformer and install it on your classloader (see
   `PluginMixinManager`).
5. **Dependencies**: mixin + the ASM stack at runtime, and `compileOnly` in any
   module that only writes mixins.

The `GotoPhase`/DEFAULT-phase advance and reflection around `MixinTransformer`
exist because the mixin transformer is not a public API — mirror
`PluginMixinManager.createTransformer()` / `advanceToDefaultPhase()`.
