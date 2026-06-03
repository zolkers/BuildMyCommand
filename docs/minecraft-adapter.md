# Minecraft Adapter Architecture

BuildMyCommand targets Minecraft through a family of adapter modules instead of one hard dependency.

## Module Layout

- `modules/adapters/brigadier`: platform-neutral Mojang Brigadier projection bridge.
- `modules/adapters/minecraft/common`: version-neutral invocation, source mapping, runtime capabilities, backend profiles, registration plans, the native command adapter, and Minecraft configuration helpers for the Brigadier adapter.
- `modules/adapters/minecraft/paper`: Paper Brigadier/lifecycle profile and invocation normalization.
- `modules/adapters/minecraft/spigot`: Bukkit/Spigot `CommandExecutor` and `TabCompleter` profile.
- `modules/adapters/minecraft/bungee`: BungeeCord proxy command and tab-complete profile.
- `modules/adapters/minecraft/velocity`: Velocity command API profile.

The platform modules keep native Paper, Bukkit, BungeeCord, and Velocity jars at their boundaries when those APIs add lifecycle, ownership, or fallback behavior. Fabric, Forge, and NeoForge expose Mojang Brigadier dispatchers for command registration, so they use `modules/adapters/brigadier` directly instead of separate adapter artifacts. Shared command tree projection lives in `adapters/brigadier`, never in platform modules or `core`.

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

`BrigadierCommandAdapter<N>` projects the BuildMyCommand graph into Mojang Brigadier nodes and can register directly into any `CommandDispatcher<N>` through `adapter.registration().register(dispatcher)`. It keeps BuildMyCommand as the source of truth: Brigadier executors call back into `CommandFramework.dispatch`, and Brigadier suggestions call back into the framework suggestion engine. Minecraft mod loaders that expose a dispatcher, including Fabric, Forge, and NeoForge, should consume this adapter directly from their command registration event.

This mirrors the clean part of Imperat's Brigadier integration: a dedicated Brigadier manager wraps the native source, maps framework argument metadata to Brigadier argument types, projects the internal command tree, and sends execution plus suggestions back through the framework dispatcher. BuildMyCommand keeps that model in `adapters/brigadier`, while Minecraft modules only exist when platform APIs add lifecycle, ownership, or non-Brigadier command behavior.

The Brigadier projection deliberately avoids making Brigadier the final parser. Non-greedy framework arguments are registered as string nodes, greedy arguments stay greedy, and a `_bmc_input` fallback argument delegates unmatched tails back to the framework. This means framework-owned behavior still applies for argument conversion, permission failures, option and flag aliases, nested route aliases, case-insensitive literals/options, and rich suggestions. Brigadier remains useful for client-visible command trees, but BuildMyCommand remains the authority.

`MinecraftBackendProfile` and `MinecraftBackendProfiles` describe capabilities and edge cases per backend. These profiles are tested so adapters do not silently forget platform-specific constraints.

Spigot, Paper, BungeeCord, and Velocity expose native command adapter factories. Paper, Velocity, Fabric, Forge, NeoForge, Sponge, and Minestom can consume the shared Brigadier adapter through their native registration hooks instead of duplicating parsing, permission checks, suggestions, or dispatch behavior.

`CommandFramework.builder().caseInsensitiveLiterals()` and `caseInsensitiveOptions()` make the core dispatcher tolerant once input reaches BuildMyCommand. Native Brigadier literal nodes compare the input with the literal exactly while parsing, so the adapter also registers framework fallback tunnels that forward unmatched input to BuildMyCommand. Use the native adapter path whenever the platform can route `label + args[]` to BuildMyCommand. Use Brigadier projection when the platform requires a native command tree for client-visible syntax, native completions, and loader registration.

## Adapter Selection

- `MinecraftNativeCommandAdapter`: best for Spigot, Bukkit, Paper fallback command map, BungeeCord, and Velocity simple/raw commands. It preserves the framework as the real parser and is the path for case-insensitive command policies.
- `BrigadierCommandAdapter`: best for any API that exposes a `CommandDispatcher<S>`, including Paper Brigadier, Velocity Brigadier, Fabric, Forge, NeoForge, Sponge, and Minestom. It exports literals, argument positions, aliases, and suggestions while delegating framework semantics back to core through executors and `_bmc_input` fallback nodes.
- Hybrid platform registration: reserve for hosts that need both Brigadier registration and a separate non-Brigadier command API. Keep that composition in the platform module or user integration code; do not duplicate parsing outside core.

## Backend Edge Cases

- Paper: Brigadier cursor ranges, lifecycle re-registration, permission-filtered suggestions.
- Spigot: `label + args[]` reconstruction, trailing empty tab argument, alias label preservation, coarse plugin.yml permissions.
- BungeeCord: command objects bind root labels, tab completion lacks a label parameter, message-only results, unregister-before-reregister reloads.
- Velocity: proxy command ownership, aliases through metadata, Brigadier/Simple/Raw command trade-offs.
- Fabric, Forge, and NeoForge: register the generic Brigadier adapter from the loader command event; loader-specific event classes stay in the user's mod, not in BuildMyCommand.

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
