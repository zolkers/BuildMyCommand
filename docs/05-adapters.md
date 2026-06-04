# Adapters

Adapters connect BuildMyCommand to a host runtime. The core framework does not know about Minecraft, terminals, Discord, or IDEs.

## Dependencies

Every application starts with the framework modules:

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.1.0"

dependencies {
    implementation("io.github.zolkers:api:$buildMyCommandVersion")
    implementation("io.github.zolkers:core:$buildMyCommandVersion")
    implementation("io.github.zolkers:annotations:$buildMyCommandVersion")
}
```

Then add the adapter that matches your runtime.

| Runtime | Artifact |
| --- | --- |
| Generic adapter contracts | `io.github.zolkers:adapters-core:$buildMyCommandVersion` |
| Brigadier command trees | `io.github.zolkers:adapters-brigadier:$buildMyCommandVersion` |
| Terminal/CLI apps | `io.github.zolkers:adapters-terminal:$buildMyCommandVersion` |
| Discord text/slash command export | `io.github.zolkers:adapters-discord:$buildMyCommandVersion` |
| Minecraft shared native bridge | `io.github.zolkers:adapters-minecraft-common:$buildMyCommandVersion` |
| Fabric | `io.github.zolkers:adapters-minecraft-fabric:$buildMyCommandVersion` |
| Forge | `io.github.zolkers:adapters-minecraft-forge:$buildMyCommandVersion` |
| NeoForge | `io.github.zolkers:adapters-minecraft-neoforge:$buildMyCommandVersion` |
| Paper | `io.github.zolkers:adapters-minecraft-paper:$buildMyCommandVersion` |
| Spigot | `io.github.zolkers:adapters-minecraft-spigot:$buildMyCommandVersion` |
| BungeeCord | `io.github.zolkers:adapters-minecraft-bungee:$buildMyCommandVersion` |
| Velocity | `io.github.zolkers:adapters-minecraft-velocity:$buildMyCommandVersion` |
| Minestom | `io.github.zolkers:adapters-minecraft-minestom:$buildMyCommandVersion` |
| Sponge | `io.github.zolkers:adapters-minecraft-sponge:$buildMyCommandVersion` |

Use one platform adapter in application code. Use `adapters-core` directly when you are building your own adapter or integration library.

## Adapter Responsibilities

| Responsibility | Meaning |
| --- | --- |
| Input | Convert native command input into framework input. |
| Source | Wrap the native sender/player/client as `CommandSource`. |
| Dispatch | Call `CommandFramework.dispatch(...)` or suggestion APIs. |
| Render | Convert `CommandResult`/`CommandMessage` back to the native platform. |
| Registration | Expose commands to the native command system if needed. |

## CommandSource

Most integration quality comes from a good `CommandSource` implementation.

```java
public final class AppSource implements CommandSource {
    private final Object nativeSource;

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        return type.isInstance(nativeSource) ? Optional.of(type.cast(nativeSource)) : Optional.empty();
    }

    @Override
    public boolean hasPermission(String permission) {
        return permission == null || permission.isBlank();
    }
}
```

Use `unwrap(...)` for platform-specific suggestion logic:

```java
@Suggest("target")
SuggestionSet targets(SuggestionContext ctx) {
    return ctx.unwrapSource(MyNativeSource.class)
        .map(nativeSource -> SuggestionSet.of(nativeSource.players()).filteringCurrentToken())
        .orElseGet(SuggestionSet::empty);
}
```

## IAdapter

The adapter module exposes a generic `IAdapter` contract so adapters follow the same shape. A good adapter should make these behaviors explicit:

| Contract | Should answer |
| --- | --- |
| Source conversion | What native source becomes `CommandSource`? |
| Input conversion | How is input text/cursor captured? |
| Result rendering | How are success/failure/info messages displayed? |
| Suggestions | Does the platform support cursor-aware suggestions? |
| Permissions | Native permission API, custom policy, or source wrapper? |
| Case policy | Does the platform force lowercase/literal rules? |

## Custom Adapter With SimpleCommandAdapter

For most custom runtimes, start with `SimpleCommandAdapter`. It lets you provide mapping lambdas without writing a class for every method in `IAdapter`.

```kotlin
dependencies {
    implementation("io.github.zolkers:api:$buildMyCommandVersion")
    implementation("io.github.zolkers:core:$buildMyCommandVersion")
    implementation("io.github.zolkers:adapters-core:$buildMyCommandVersion")
}
```

```java
record ChatUser(String id, String name) {
}

record ChatInput(String raw, int cursor) {
}

record ChatReply(int status, Optional<String> message) {
}

CommandFramework framework = CommandFramework.create();
AnnotationCommandScanner.register(framework.registry(), new ChatCommands());

CommandPlatform platform = new CommandPlatform("chat", "Chat", true, true, true);

SimpleCommandAdapter<ChatUser, ChatInput, ChatReply> adapter =
    SimpleCommandAdapter.<ChatUser, ChatInput, ChatReply>builder(framework, platform)
        .sourceMapper(user -> new CommandSource() {
            @Override
            public Optional<String> id() {
                return Optional.of(user.id());
            }

            @Override
            public Optional<String> name() {
                return Optional.of(user.name());
            }
        })
        .inputMapper((user, input, runtime, mapper) -> new CommandInput(
            mapper.map(user),
            input.raw(),
            input.raw().startsWith("!") ? input.raw().substring(1) : input.raw(),
            input.cursor(),
            input.raw().startsWith("!") ? "!" : "",
            runtime.platform()
        ))
        .renderer(result -> new ChatReply(
            result.status() == CommandResult.Status.SUCCESS ? 200 : 400,
            result.reply()
        ))
        .build();

ChatReply reply = adapter.execute(new ChatUser("42", "Ada"), new ChatInput("!ping", 5));
List<String> suggestions = adapter.suggest(new ChatUser("42", "Ada"), new ChatInput("!p", 2), 2);
```

The adapter now handles dispatch, rendering, and suggestions through the same framework contracts as the built-in adapters.

## Custom Adapter Class

If your runtime needs registration, async APIs, lifecycle hooks, or native result objects, implement `CommandAdapter` directly. `CommandAdapter` already implements the common `IAdapter` methods; you provide runtime metadata, source mapping, input mapping, and rendering.

```java
public final class MyRuntimeAdapter implements CommandAdapter<MySender, MyInvocation, MyRenderedResult> {
    private final AdapterRuntime runtime;
    private final AdapterConfig config;

    public MyRuntimeAdapter(CommandFramework framework) {
        CommandPlatform platform = new CommandPlatform("my-runtime", "My Runtime", true, true, true);
        this.runtime = new AdapterRuntime(framework, platform);
        this.config = AdapterConfig.of("my-runtime", "My Runtime", AdapterCapabilities.from(platform));
    }

    @Override
    public AdapterRuntime runtime() {
        return runtime;
    }

    @Override
    public AdapterConfig config() {
        return config;
    }

    @Override
    public AdapterRenderer<MyRenderedResult> renderer() {
        return result -> MyRenderedResult.from(result);
    }

    @Override
    public CommandSource mapSource(MySender sender) {
        return new MyCommandSource(sender);
    }

    @Override
    public CommandInput mapInput(MySender sender, MyInvocation invocation) {
        return new CommandInput(
            mapSource(sender),
            invocation.rawInput(),
            invocation.normalizedInput(),
            invocation.cursor(),
            invocation.prefix(),
            runtime.platform()
        );
    }
}
```

Register commands using your native runtime API, then call `execute(...)` and `suggest(...)` from the native callbacks.

## Custom Adapter Checklist

1. Keep native APIs at the edge.
2. Wrap native users/senders in `CommandSource`.
3. Route all replies through `CommandMessage`.
4. Preserve cursor offsets for suggestions.
5. Do not swallow unknown native commands.
6. Respect `CommandFramework.caseInsensitiveLiterals()` and `caseInsensitiveOptions()` if the native runtime lets you.
7. Preserve `@SuggestAliases(false)` by delegating suggestions to the framework or filtering exported alias metadata.
8. Add smoke tests for unknown command, incomplete command, success, failure, permissions, aliases, hidden aliases, and suggestions.

For Minecraft/Fabric, see [Minecraft/Fabric](06-minecraft.md).
