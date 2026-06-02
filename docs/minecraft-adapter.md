# Minecraft Adapter Architecture

BuildMyCommand targets Minecraft through a family of adapter modules instead of one hard dependency.

## Module Layout

- `modules/adapters/minecraft/common`: version-neutral invocation, source mapping, runtime capabilities, backend profiles, and registration plans.
- `modules/adapters/minecraft/paper`: Paper Brigadier/lifecycle profile and invocation normalization.
- `modules/adapters/minecraft/spigot`: Bukkit/Spigot `CommandExecutor` and `TabCompleter` profile.
- `modules/adapters/minecraft/bungee`: BungeeCord proxy command and tab-complete profile.
- `modules/adapters/minecraft/velocity`: Velocity command API profile.
- `modules/adapters/minecraft/fabric`: Fabric `CommandRegistrationCallback` profile.
- `modules/adapters/minecraft/forge`: Forge `RegisterCommandsEvent` profile.
- `modules/adapters/minecraft/neoforge`: NeoForge `RegisterCommandsEvent` profile.

The platform modules intentionally depend only on `common` today. Native Paper, Bukkit, BungeeCord, Velocity, Fabric, Forge, and NeoForge jars should be introduced in those modules one by one, never in `core`.

## Neutral Model

`MinecraftInvocation` preserves the information that platform APIs expose differently:

- raw input with slash state
- normalized input without a leading slash
- invoked label or alias
- args array excluding the label
- normalized cursor
- current argument prefix

`MinecraftCommandBridge<S>` accepts either raw command strings or a `MinecraftInvocation`, maps the native source through `MinecraftSourceMapper<S>`, and delegates to `CommandFramework`.

`MinecraftBackendProfile` and `MinecraftBackendProfiles` describe capabilities and edge cases per backend. These profiles are tested so adapters do not silently forget platform-specific constraints.

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
