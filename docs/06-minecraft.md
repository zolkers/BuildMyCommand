# 06 - Minecraft

Minecraft integration is split into a generic Brigadier bridge and platform-specific adapters. Brigadier comes from Mojang and is strongly associated with Minecraft, but it is still useful as a generic command-tree protocol because multiple Minecraft platforms expose or emulate it differently.

## Module Map

| Platform | Module | Notes |
| --- | --- | --- |
| Generic Brigadier | `adapters-brigadier` | Converts BuildMyCommand graph to Brigadier nodes. |
| Shared Minecraft model | `adapters-minecraft-common` | Capabilities, runtime profiles, result rendering, edge cases. |
| Fabric | `adapters-minecraft-fabric` | Fabric Command API v1/v2 bridge for 1.16.5+ style registration. |
| Forge | `adapters-minecraft-forge` | Forge `RegisterCommandsEvent` bridge for 1.16.5+ style registration. |
| NeoForge | `adapters-minecraft-neoforge` | NeoForge `RegisterCommandsEvent` bridge. |
| Spigot | `adapters-minecraft-spigot` | Bukkit/Spigot command integration. |
| Paper | `adapters-minecraft-paper` | Paper-specific layer plus Spigot base. |
| BungeeCord | `adapters-minecraft-bungee` | Proxy command integration. |
| Velocity | `adapters-minecraft-velocity` | Proxy command integration. |
| Minestom | `adapters-minecraft-minestom` | Native server framework integration. |
| Sponge | `adapters-minecraft-sponge` | Sponge integration. |

## Compatibility Mindset

The framework core is platform-independent. Minecraft adapters are responsible for bridging:

| Concern | Framework | Minecraft adapter |
| --- | --- | --- |
| Parsing route DSL | Core | Reused |
| Permissions | `CommandSource.hasPermission` | Map to sender/player/proxy permissions |
| Suggestions | `SuggestionProvider` / rich suggestions | Map to Brigadier/platform suggestion APIs |
| Case sensitivity | Matching policy | Respect or emulate where platform allows |
| Native command tree | `CommandGraph` | Register literals/arguments/options |
| Output | `CommandResult` / `CommandMessage` | Send chat/component/platform result |

## Brigadier Notes

Brigadier is powerful for command trees but limited for some framework-level behavior:

| Topic | Adapter responsibility |
| --- | --- |
| Case-insensitive literals | Brigadier literals are exact; adapter may normalize or register aliases where possible. |
| Options/flags | Framework models them; Brigadier bridge must expose parse-compatible nodes. |
| Greedy strings | Map to Brigadier greedy string argument where possible. |
| Permissions | Brigadier `requires` should delegate to framework/platform permission checks. |
| Suggestions | Use framework suggestions and translate to Brigadier suggestions. |

## Platform Profiles

Minecraft common exposes descriptors/profiles so adapters can describe what a runtime supports.

| Concept | Purpose |
| --- | --- |
| `MinecraftRuntimeDescriptor` | Identifies platform/runtime. |
| `MinecraftBackendProfile` | Captures backend capabilities and constraints. |
| `MinecraftCapability` | Fine-grained support flags. |
| `MinecraftCommandEdgeCase` | Documents adapter/platform edge cases. |
| `MinecraftCommandRegistrationPlan` | Registration output plan. |

## Recommended Command Style For Minecraft

Use route annotations. Minecraft commands become deeply nested quickly, and `@SubRoute` keeps the tree readable:

```java
@Command("admin")
@Alias("a")
@CaseInsensitive(literals = true, options = true)
static final class AdminCommands {
    @SubRoute("moderation punish temporary add <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
    @Permission("admin.moderation.punish")
    CommandResult tempPunish(@RouteCtx CommandContext ctx) {
        ...
    }
}
```

## Version Guidance

| Area | Guidance |
| --- | --- |
| Minecraft 1.16.5+ | Prefer the dedicated loader adapter when it exists; it wraps Brigadier registration while preserving loader-specific lifecycle details. |
| Fabric 1.16.5 | Use `FabricMinecraftAdapter.legacyRegistration(...)` for Command API v1 callbacks. |
| Fabric modern | Use `FabricMinecraftAdapter.registration(...)` for Command API v2 callbacks. |
| Forge 1.16.5 | Use `ForgeMinecraftAdapter.legacyRegistration(...)` in `RegisterCommandsEvent`. |
| Forge modern | Use `ForgeMinecraftAdapter.registration(...)` in `RegisterCommandsEvent`. |
| NeoForge | Use `NeoForgeMinecraftAdapter.registration(...)` in NeoForge's `RegisterCommandsEvent`. |
| Spigot/Paper | Paper can expose richer command behavior; Spigot compatibility remains important. |
| Proxies | Bungee and Velocity need source/result mapping that fits proxy sender models. |
| Mod loaders without a dedicated module | Use `adapters-brigadier` plus `adapters-minecraft-common` directly. |

Always test on the target platform/version because command registration APIs differ across server/proxy implementations.

## Fabric Registration Shape

Fabric 1.16.5 uses `net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback`; modern Fabric uses command API v2. The adapter keeps both paths explicit:

```java
CommandFramework framework = CommandFramework.create();
FabricBrigadierRegistration<ServerCommandSource> registration =
    FabricMinecraftAdapter.legacyRegistration(framework, FabricCommandSources::map);

CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
    registration.registerInto(dispatcher);
});
```

For modern Fabric:

```java
FabricBrigadierRegistration<CommandSourceStack> registration =
    FabricMinecraftAdapter.registration(framework, FabricCommandSources::map);

CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    registration.registerInto(dispatcher);
});
```

## Forge And NeoForge Registration Shape

Forge 1.16.5 and modern Forge both register during `RegisterCommandsEvent`; the source type name differs by mapping/version, so the adapter is generic over the native source:

```java
ForgeBrigadierRegistration<CommandSource> registration =
    ForgeMinecraftAdapter.legacyRegistration(framework, ForgeCommandSources::map);

@SubscribeEvent
public void registerCommands(RegisterCommandsEvent event) {
    registration.registerInto(event.getDispatcher());
}
```

NeoForge follows the same shape with its own event package:

```java
NeoForgeBrigadierRegistration<CommandSourceStack> registration =
    NeoForgeMinecraftAdapter.registration(framework, NeoForgeCommandSources::map);

@SubscribeEvent
public void registerCommands(RegisterCommandsEvent event) {
    registration.registerInto(event.getDispatcher());
}
```

The mapper is where platform permissions, sender identity, locale, metadata, and replies become a BuildMyCommand `CommandSource`.

## Testing Matrix

| Test | Why |
| --- | --- |
| Register root + alias | Platform command managers often treat aliases differently. |
| Permission denied | Sender permission APIs differ. |
| Greedy reason | Chat command parsing commonly breaks greedy args. |
| Short flags | Minecraft users expect compact admin commands. |
| Suggestions at root, literal, argument, option | Brigadier/platform completions have different cursor semantics. |
| Case-insensitive literal path | Brigadier exact literals need adapter strategy. |
