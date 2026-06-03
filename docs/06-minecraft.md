# 06 - Minecraft

Minecraft integration is split into a generic Brigadier bridge and platform-specific adapters. Brigadier comes from Mojang and is strongly associated with Minecraft, but it is still useful as a generic command-tree protocol because multiple Minecraft platforms expose or emulate it differently.

## Module Map

| Platform | Module | Notes |
| --- | --- | --- |
| Generic Brigadier | `adapters-brigadier` | Converts BuildMyCommand graph to Brigadier nodes. |
| Shared Minecraft model | `adapters-minecraft-common` | Capabilities, runtime profiles, result rendering, edge cases. |
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
| Minecraft 1.16+ | Prefer platform adapters that expose Brigadier/native registration where available. |
| Spigot/Paper | Paper can expose richer command behavior; Spigot compatibility remains important. |
| Proxies | Bungee and Velocity need source/result mapping that fits proxy sender models. |
| Mod loaders | If the environment exposes Brigadier, prefer the generic Brigadier bridge plus Minecraft common behavior. |

Always test on the target platform/version because command registration APIs differ across server/proxy implementations.

## Testing Matrix

| Test | Why |
| --- | --- |
| Register root + alias | Platform command managers often treat aliases differently. |
| Permission denied | Sender permission APIs differ. |
| Greedy reason | Chat command parsing commonly breaks greedy args. |
| Short flags | Minecraft users expect compact admin commands. |
| Suggestions at root, literal, argument, option | Brigadier/platform completions have different cursor semantics. |
| Case-insensitive literal path | Brigadier exact literals need adapter strategy. |
