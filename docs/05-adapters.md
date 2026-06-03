# Adapters

Adapters connect BuildMyCommand to a host runtime. The core framework does not know about Minecraft, terminals, Discord, or IDEs.

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

## Custom Adapter Checklist

1. Keep native APIs at the edge.
2. Wrap native users/senders in `CommandSource`.
3. Route all replies through `CommandMessage`.
4. Preserve cursor offsets for suggestions.
5. Do not swallow unknown native commands.
6. Add smoke tests for unknown command, incomplete command, success, failure, and suggestions.

For Minecraft/Fabric, see [Minecraft/Fabric](06-minecraft.md).
