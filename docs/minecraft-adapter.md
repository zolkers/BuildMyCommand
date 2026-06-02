# Minecraft Adapter Architecture

BuildMyCommand targets Minecraft through a neutral bridge instead of one hard dependency.

## Why

Modern Paper and Fabric command APIs both route through Mojang Brigadier, but their registration lifecycle and source types differ by loader and version. Paper documents Brigadier command registration through its command API and lifecycle registration. Fabric documents command registration through Fabric API callbacks. NeoForge has its own mod event model. A single compiled adapter cannot honestly cover every server/mod-loader version without either reflection or separate backend modules.

## Current Module

`modules/minecraft-adapter` provides:

- `MinecraftCommandBridge<S>`: dispatches and suggests against `CommandFramework` from any platform source type.
- `MinecraftSourceMapper<S>`: maps a Paper/Fabric/NeoForge/Bukkit sender/source object into `CommandSource`.
- `MinecraftRuntimeDescriptor`: records loader, Minecraft version, API version, and detected capabilities.
- `MinecraftCapability`: describes runtime features such as Brigadier, lifecycle events, legacy command maps, and client commands.

## Backend Pattern

Version-specific modules should stay thin:

1. Detect loader and version at runtime.
2. Build a `MinecraftRuntimeDescriptor`.
3. Register root literals with the platform's command registration API.
4. Delegate execution to `MinecraftCommandBridge#dispatch`.
5. Delegate completions to `MinecraftCommandBridge#suggest`.

This keeps BuildMyCommand stable while loaders change their registration APIs.

## References

- Paper command API: https://docs.papermc.io/paper/dev/api/command-api/
- Paper registration lifecycle: https://docs.papermc.io/paper/dev/command-api/basics/registration/
- Fabric command basics: https://docs.fabricmc.net/develop/commands/basics
- NeoForge docs: https://docs.neoforged.net/
