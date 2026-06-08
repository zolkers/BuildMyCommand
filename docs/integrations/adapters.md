<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Adapters

Adapters connect BuildMyCommand to a host runtime. The core framework does not know about Minecraft, terminals, Discord, or IDEs.

## Dependencies

Every application starts with the framework modules:

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.3.6"

dependencies {
    implementation("io.github.zolkers:buildmycommand-api:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-core:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-annotations:$buildMyCommandVersion")
}
```


Maven:

```xml
<properties>
    <buildmycommand.version>0.3.6</buildmycommand.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-api</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-core</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-annotations</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
</dependencies>
```

Then add the adapter that matches your runtime.

| Runtime | Artifact |
| --- | --- |
| Generic adapter contracts | `io.github.zolkers:buildmycommand-adapters-core:$buildMyCommandVersion` |
| Brigadier command trees | `io.github.zolkers:buildmycommand-adapters-brigadier:$buildMyCommandVersion` |
| Terminal/CLI apps | `io.github.zolkers:buildmycommand-adapters-terminal:$buildMyCommandVersion` |
| Discord text/slash command export | `io.github.zolkers:buildmycommand-adapters-discord:$buildMyCommandVersion` |
| Minecraft shared native bridge | `io.github.zolkers:buildmycommand-adapters-minecraft-common:$buildMyCommandVersion` |
| Fabric | `io.github.zolkers:buildmycommand-adapters-minecraft-fabric:$buildMyCommandVersion` |
| Forge | `io.github.zolkers:buildmycommand-adapters-minecraft-forge:$buildMyCommandVersion` |
| NeoForge | `io.github.zolkers:buildmycommand-adapters-minecraft-neoforge:$buildMyCommandVersion` |
| Paper | `io.github.zolkers:buildmycommand-adapters-minecraft-paper:$buildMyCommandVersion` |
| Spigot | `io.github.zolkers:buildmycommand-adapters-minecraft-spigot:$buildMyCommandVersion` |
| BungeeCord | `io.github.zolkers:buildmycommand-adapters-minecraft-bungee:$buildMyCommandVersion` |
| Velocity | `io.github.zolkers:buildmycommand-adapters-minecraft-velocity:$buildMyCommandVersion` |
| Minestom | `io.github.zolkers:buildmycommand-adapters-minecraft-minestom:$buildMyCommandVersion` |
| Sponge | `io.github.zolkers:buildmycommand-adapters-minecraft-sponge:$buildMyCommandVersion` |

Use one platform adapter in application code. Use `adapters-core` directly when you are building your own adapter or integration library.


Maven adapter example:

```xml
<dependency>
    <groupId>io.github.zolkers</groupId>
    <artifactId>buildmycommand-adapters-minecraft-fabric</artifactId>
    <version>${buildmycommand.version}</version>
</dependency>
```

## Adapter Responsibilities

| Responsibility | Meaning |
| --- | --- |
| Input | Convert native command input into framework input. |
| Source | Wrap the native sender/player/client as `CommandSource`. |
| Dispatch | Call `CommandFramework.dispatch(...)` or suggestion APIs. |
| Render | Convert `CommandResult`/`CommandMessage` back to the native platform. |
| Registration | Expose commands to the native command system if needed. |

## CommandSource

Most integration quality comes from a good `CommandSource` mapping. This is the platform boundary for every runtime, not only Minecraft.

You do not have to name a class `ModCommandSource`, and you do not always need a dedicated class. What is required is that the adapter can turn the native sender/session/player/client into a `CommandSource`.

| Question | Answer |
| --- | --- |
| Is a custom `CommandSource` implementation required? | Yes, somewhere, unless the platform adapter already provides one. |
| Is a dedicated class required? | No. A class, record, lambda-backed anonymous implementation, or adapter-provided factory can all work. |
| When should I create a named class? | When your project has multiple commands, permissions, native suggestions, middleware, or platform-specific replies. |
| When is an anonymous implementation enough? | Small tools, tests, examples, or simple apps with no platform state. |
| Should command classes know Fabric, Bukkit, Discord, etc.? | Usually no. Keep platform APIs in the source wrapper, adapter, middleware, or suggestion providers. |

Recommended shape for real projects:

| Layer | Owns |
| --- | --- |
| Native platform | Actual sender/player/session object. |
| Adapter | Registration, input mapping, suggestion bridge, and result rendering. |
| `CommandSource` wrapper | Identity, permission policy, native unwrap, reply routing. |
| Command class | Framework DSL, business logic, `CommandContext`. |

Minimal generic wrapper:

```java
public final class AppSource implements CommandSource {
    private final Object nativeSource;
    private final Consumer<CommandMessage> reply;

    public AppSource(Object nativeSource, Consumer<CommandMessage> reply) {
        this.nativeSource = Objects.requireNonNull(nativeSource, "nativeSource");
        this.reply = Objects.requireNonNull(reply, "reply");
    }

    @Override
    public void reply(CommandMessage message) {
        reply.accept(message);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.isInstance(nativeSource) ? Optional.of(type.cast(nativeSource)) : Optional.empty();
    }

    @Override
    public boolean hasPermission(String permission) {
        return permission == null || permission.isBlank();
    }
}
```

Small projects can inline the same idea:

```java
CommandSource source = new CommandSource() {
    @Override
    public void reply(CommandMessage message) {
        System.out.println(message.text());
    }

    @Override
    public boolean hasPermission(String permission) {
        return permission == null || permission.isBlank();
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        return Optional.empty();
    }
};
```

The shape stays the same for every platform:

| Runtime | Native source example | Wrapper responsibility |
| --- | --- | --- |
| Fabric client | `FabricClientCommandSource` | Send chat feedback/errors, expose client state through `unwrap(...)`. |
| Paper/Spigot | `CommandSender` or `Player` | Delegate permissions to Bukkit, send messages to sender. |
| BungeeCord/Velocity | Proxy command sender | Delegate proxy permissions and route messages through proxy APIs. |
| Minestom/Sponge | Platform sender/player object | Adapt platform permission and message systems. |
| Discord | User/member/interaction object | Reply through interaction/channel APIs and expose guild/member state. |
| Terminal/CLI | Shell session/user object | Print replies to stdout/stderr and provide local permissions. |
| Custom app | Any session/request/user object | Keep framework commands independent from native APIs. |

The methods have clear meanings:

| Method | Purpose |
| --- | --- |
| `reply(CommandMessage)` | Central place that renders success/info/error messages to the platform. |
| `hasPermission(String)` | Exact permission policy used by `@Permission`, `@Require`, help filtering, and suggestions. |
| `permissions()` | Optional enumerable permission set used by regex permissions. |
| `hasPermissionMatching(Pattern)` | Optional native regex/pattern permission matcher. |
| `unwrap(Class<T>)` | Safe escape hatch for platform-specific suggestions, middleware, or advanced integrations. |
| `id()` / `name()` | Optional identity for logs, cooldowns, help, metrics, or audit middleware. |
| `metadata(String)` | Optional place for adapter-specific state when a direct native unwrap is not ideal. |

Commands should usually depend only on `CommandContext`, `CommandSource`, and framework types. Platform APIs should be reached through `unwrap(...)` only in focused places such as suggestion providers or middleware.

Regex permissions use the same source boundary:

```java
@Override
public Set<String> permissions() {
    return Set.copyOf(effectivePermissions);
}
```

or, when the native platform has a better permission store:

```java
@Override
public boolean hasPermissionMatching(Pattern pattern) {
    return effectivePermissions().stream().anyMatch(permission -> pattern.matcher(permission).matches());
}
```

If a source only implements `hasPermission(String)`, exact permissions and
`@Require` work normally, but regex permissions cannot discover matching nodes.

## Source Wrapper Recipes

Use the same contract everywhere, but choose the implementation details from the host platform.

| Platform family | `reply(CommandMessage)` | `hasPermission(String)` | `unwrap(Class<T>)` |
| --- | --- | --- | --- |
| Fabric client | `sendFeedback` for success/info, `sendError` for errors. | Client-only mods may return `true`; multiplayer tools should use your own policy. | Return `FabricClientCommandSource` so suggestions can read client/player/network state. |
| Paper/Spigot | `CommandSender.sendMessage(...)`, with your preferred component conversion. | `permission.isBlank() \|\| sender.hasPermission(permission)`. | Return `CommandSender` and `Player` when applicable. |
| BungeeCord/Velocity | Proxy sender messaging APIs. | Native proxy permission API. | Return proxy sender/player/connection objects. |
| Minestom/Sponge | Platform audience/message APIs. | Native permission service or your own permission registry. | Return native sender/player/server objects. |
| Discord | Interaction response, follow-up, or channel message. | Guild role/member policy, bot owner checks, or custom ACL. | Return interaction/member/channel/client objects. |
| Terminal | `stdout` for success/info, `stderr` for errors. | Local user policy, config file, or always true for trusted tools. | Return shell/session/config objects if needed. |
| HTTP/WebSocket | Response object, event sink, or session queue. | Auth claims, roles, scopes, or tenant ACL. | Return request/session/user principal objects. |

For a public adapter, prefer exposing a small factory so users do not copy boilerplate:

```java
public final class MyPlatformSources {
    private MyPlatformSources() {
    }

    public static CommandSource of(MyNativeSender sender) {
        return new MyPlatformCommandSource(sender);
    }
}
```

Then application code can stay clean:

```java
adapter.register(dispatcher, nativeSource -> MyPlatformSources.of(nativeSource));
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
    implementation("io.github.zolkers:buildmycommand-api:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-core:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-adapters-core:$buildMyCommandVersion")
}
```


Maven:

```xml
<dependency>
    <groupId>io.github.zolkers</groupId>
    <artifactId>buildmycommand-adapters-core</artifactId>
    <version>${buildmycommand.version}</version>
</dependency>
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

For Minecraft/Fabric, see [Minecraft/Fabric](minecraft.md).
