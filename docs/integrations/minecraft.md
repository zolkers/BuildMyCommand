<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Minecraft And Fabric

Minecraft integrations are built around Brigadier-style command trees, but your command code should stay framework-first.

For Fabric client-side mods, the clean pattern is:

1. Build one `CommandFramework`.
2. Register annotated command classes.
3. Register the framework through the Fabric adapter.
4. Wrap `FabricClientCommandSource` in your own `CommandSource`.

## Dependencies

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.3.5"

dependencies {
    implementation("io.github.zolkers:buildmycommand-api:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-core:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-annotations:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-adapters-minecraft-fabric:$buildMyCommandVersion")
}
```


Maven:

```xml
<properties>
    <buildmycommand.version>0.3.5</buildmycommand.version>
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
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-adapters-minecraft-fabric</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
</dependencies>
```

## Command Class

```java
@Command("wecc")
@CaseInsensitive
public final class PingCommand {
    @SubRoute("ping")
    @Description("Client command smoke test")
    @Middleware(ReplyResultMiddleware.class)
    CommandResult ping(@RouteCtx CommandContext ctx) {
        return Results.success("pong from client");
    }

    @SuggestAliases(false)
    @SubRoute("bang|b <target:String>")
    @Description("Client command smoke test")
    @Middleware(ReplyResultMiddleware.class)
    CommandResult bang(@RouteCtx CommandContext ctx) {
        return Results.success("bang to this noob " + ctx.arg("target", String.class));
    }

    @Suggest("target")
    SuggestionSet onlinePlayers(SuggestionContext ctx) {
        return ctx.unwrapSource(FabricClientCommandSource.class)
            .map(FabricClientCommandSource::getClient)
            .filter(client -> client.getConnection() != null)
            .map(client -> SuggestionSet.of(client.getConnection().getOnlinePlayers()
                .stream()
                .map(player -> player.getProfile().name())
                .toList()).filteringCurrentToken())
            .orElseGet(SuggestionSet::empty);
    }
}
```

The important part: `@Suggest("target")` links to `<target:String>` in `bang|b <target:String>`. It does not need to match `ping`; the provider is applied only to routes that actually contain `target`.

## Source Wrapper For QoL

Do not put Fabric reply/permission/native unwrap code in every command. Put it in one source wrapper.

```java
package fr.edgn.wec.commands;

import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandSource;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class ModCommandSource implements CommandSource {
    private final String name;
    private final boolean elevated;
    private final Consumer<CommandMessage> reply;
    private final Object nativeSource;

    private ModCommandSource(String name, boolean elevated, Consumer<CommandMessage> reply, Object nativeSource) {
        this.name = Objects.requireNonNull(name, "name");
        this.elevated = elevated;
        this.reply = Objects.requireNonNull(reply, "reply");
        this.nativeSource = Objects.requireNonNull(nativeSource, "nativeSource");
    }

    public static ModCommandSource client(FabricClientCommandSource source) {
        String playerName = source.getPlayer() == null
            ? "client"
            : source.getPlayer().getName().getString();
        return new ModCommandSource(
            playerName,
            true,
            message -> {
                Component component = Component.literal(message.text());
                switch (message.level()) {
                    case ERROR -> source.sendError(component);
                    case SUCCESS, INFO -> source.sendFeedback(component);
                }
            },
            source
        );
    }

    @Override
    public Optional<String> name() {
        return Optional.of(name);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(nativeSource)) {
            return Optional.of(type.cast(nativeSource));
        }
        return Optional.empty();
    }

    @Override
    public void reply(CommandMessage message) {
        reply.accept(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return permission == null || permission.isBlank() || elevated;
    }
}
```

Why this is worth documenting:

| Feature | Benefit |
| --- | --- |
| `reply(CommandMessage)` | Commands return framework results; Fabric rendering stays in one place. |
| `unwrap(FabricClientCommandSource.class)` | Suggestions can reach client state safely. |
| `name()` | Logs/help/middleware can identify the source. |
| `hasPermission(...)` | `@Permission` and `@Require` work even in client-only mods. |

For client-only mods, `elevated = true` is often fine. For multiplayer/server-sensitive features, replace it with your own policy.

## Registration Shape

Keep a small registry class in your mod:

```java
public final class ModCommands {
    private final CommandFramework framework = CommandFramework.create();

    public ModCommands() {
        AnnotationCommandScanner.register(framework.registry(), new PingCommand());
    }

    public CommandFramework framework() {
        return framework;
    }
}
```

Then connect it through the Fabric adapter and convert Fabric sources with `ModCommandSource.client(source)`.

## Compatibility Notes

| Platform | Notes |
| --- | --- |
| Fabric 1.16.5 style | Uses Fabric command API v1 compatibility path. |
| Modern Fabric | Uses Fabric command API v2 registration. |
| Brigadier-based runtimes | Use the Brigadier adapter underneath. |
| Client-only mods | Prefer a client source wrapper and client command registration. |

The command framework should not own your mod state. Use `CommandSource.unwrap(...)`, `SuggestionContext`, and middleware to reach platform state when needed.

## Brigadier Argument Types

Minecraft serializes the Brigadier command tree to the client. That means every `ArgumentType`
present in the registered tree must be a vanilla/client-known argument type, or a custom argument
type registered on both sides by the mod. BuildMyCommand adapters keep the public tree safe by using
vanilla Brigadier string argument types and then delegating parsing, custom route types, permissions,
middleware, and suggestions back to the framework.

This is intentional:

| BuildMyCommand feature | Minecraft tree representation |
| --- | --- |
| `<target:String>` | Vanilla `StringArgumentType.string()`. |
| `<query:String...>` | Vanilla `StringArgumentType.greedyString()`. |
| Custom DSL type such as `<item:Material>` | Vanilla string argument in Brigadier, parsed by the registered BuildMyCommand `ArgumentParser<Material>`. |
| `@Suggest` / parser suggestions | Brigadier suggestion provider that calls the framework. |
| Hidden aliases and fallback dispatch | Internal vanilla greedy string tunnel, not a custom `ArgumentType`. |

Do not replace this with an anonymous/custom Brigadier `ArgumentType` in a Minecraft adapter unless
you also register its serializer on the client and server. Fabric documents the same constraint for
custom command arguments: unknown argument types make the client reject the command tree.
