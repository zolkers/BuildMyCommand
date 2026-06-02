# Minecraft Adapter Architecture

BuildMyCommand targets Minecraft through a family of adapter modules instead of one hard dependency.

## Module Layout

- `modules/adapters/brigadier`: platform-neutral Mojang Brigadier projection bridge.
- `modules/adapters/minecraft/common`: version-neutral invocation, source mapping, runtime capabilities, backend profiles, registration plans, the native command adapter, and Minecraft configuration helpers for the Brigadier adapter.
- `modules/adapters/minecraft/paper`: Paper Brigadier/lifecycle profile and invocation normalization.
- `modules/adapters/minecraft/spigot`: Bukkit/Spigot `CommandExecutor` and `TabCompleter` profile.
- `modules/adapters/minecraft/bungee`: BungeeCord proxy command and tab-complete profile.
- `modules/adapters/minecraft/velocity`: Velocity command API profile.
- `modules/adapters/minecraft/fabric`: Fabric `CommandRegistrationCallback` profile.
- `modules/adapters/minecraft/forge`: Forge `RegisterCommandsEvent` profile.
- `modules/adapters/minecraft/neoforge`: NeoForge `RegisterCommandsEvent` profile.

The platform modules keep native Paper, Bukkit, BungeeCord, Velocity, Fabric, Forge, and NeoForge jars at their boundaries. Shared command tree projection lives in `adapters/brigadier`, never in platform modules or `core`.

## Neutral Model

`MinecraftInvocation` preserves the information that platform APIs expose differently:

- raw input with slash state
- normalized input without a leading slash
- invoked label or alias
- args array excluding the label
- normalized cursor
- current argument prefix

`MinecraftCommandBridge<S>` accepts either raw command strings or a `MinecraftInvocation`, maps the native source through `MinecraftSourceMapper<S>`, and delegates to `CommandFramework`.

`MinecraftNativeCommandAdapter<S>` is the first-class adapter for platform APIs that expose a root label plus an args array, such as Bukkit/Spigot/Paper command executors, BungeeCord commands, and Velocity simple commands. It registers every root label exported by the framework, including aliases from `ban|block` or `@Alias`, and executes through the core dispatcher. This path honors BuildMyCommand parsing, permissions, option aliases, route aliases, and `caseInsensitiveLiterals()` / `caseInsensitiveOptions()` once the platform has routed the command to the adapter.

`BrigadierCommandAdapter<N>` projects the BuildMyCommand graph into Mojang Brigadier nodes and can register directly into any `CommandDispatcher<N>` through `adapter.registration().register(dispatcher)`. It keeps BuildMyCommand as the source of truth: Brigadier executors call back into `CommandFramework.dispatch`, and Brigadier suggestions call back into the framework suggestion engine. Minecraft platforms create it through `MinecraftBrigadierAdapters` so the generic adapter is configured with Minecraft platform metadata.

`MinecraftBackendProfile` and `MinecraftBackendProfiles` describe capabilities and edge cases per backend. These profiles are tested so adapters do not silently forget platform-specific constraints.

Spigot, Paper, BungeeCord, and Velocity expose native command adapter factories. Paper, Velocity, Fabric, Forge, NeoForge, Sponge, and Minestom can consume the shared Brigadier adapter through their native registration hooks instead of duplicating parsing, permission checks, suggestions, or dispatch behavior.

`CommandFramework.builder().caseInsensitiveLiterals()` and `caseInsensitiveOptions()` make the core dispatcher tolerant once input reaches BuildMyCommand. Native Brigadier literal nodes compare the input with the literal exactly while parsing, so a plain `literal(...)` projection cannot make arbitrary-cased literals parse on its own. Use the native adapter path whenever the platform can route `label + args[]` to BuildMyCommand. Use Brigadier projection when the platform requires a native command tree for client-visible syntax, native completions, and loader registration. A future hybrid strategy can register a Brigadier catch-all argument under each root label, but that is deliberately separate because it changes the shape of the native command tree.

## Adapter Selection

- `MinecraftNativeCommandAdapter`: best for Spigot, Bukkit, Paper fallback command map, BungeeCord, and Velocity simple/raw commands. It preserves the framework as the real parser and is the path for case-insensitive command policies.
- `BrigadierCommandAdapter`: best for Paper Brigadier, Velocity Brigadier, Fabric, Forge, NeoForge, Sponge, and Minestom when a loader wants a real Brigadier tree. It exports literals, arguments, permissions, aliases, and suggestions, but Brigadier literal matching remains exact before execution reaches the framework.
- Hybrid registration: reserve for platforms where both native tree visibility and framework-level permissive matching are required. The adapter layer should expose it explicitly instead of hiding it behind `brigadierBridge(...)`.

## Backend Edge Cases

- Paper: Brigadier cursor ranges, lifecycle re-registration, permission-filtered suggestions.
- Spigot: `label + args[]` reconstruction, trailing empty tab argument, alias label preservation, coarse plugin.yml permissions.
- BungeeCord: command objects bind root labels, tab completion lacks a label parameter, message-only results, unregister-before-reregister reloads.
- Velocity: proxy command ownership, aliases through metadata, Brigadier/Simple/Raw command trade-offs.
- Fabric: dedicated environment flag, command tree caching, Brigadier `.requires(...)` permission filtering.
- Forge and NeoForge: event-bus registration, reload rebuilds, version-specific source/context signatures.

## References

- Paper command API: https://docs.papermc.io/paper/dev/api/command-api/
- Paper registration lifecycle: https://docs.papermc.io/paper/dev/command-api/basics/registration/
- Spigot `CommandExecutor`: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/command/CommandExecutor.html
- Spigot `TabCompleter`: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/command/TabCompleter.html
- BungeeCord `Command`: https://javadoc.io/static/net.md-5/bungeecord-api/1.16-R0.2/net/md_5/bungee/api/plugin/Command.html
- Velocity command API: https://docs.papermc.io/velocity/dev/command-api/
- Fabric command basics: https://docs.fabricmc.net/develop/commands/basics
- Forge `RegisterCommandsEvent`: https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraftforge/event/RegisterCommandsEvent.html
- NeoForge `RegisterCommandsEvent`: https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/neoforged/neoforge/event/RegisterCommandsEvent.html
