# 05 - Adapters

Adapters connect BuildMyCommand to a host platform. They translate native users/messages/dispatch systems into `CommandSource`, `CommandInput`, and rendered replies.

## Adapter Modules

| Module | Use case |
| --- | --- |
| `adapters-core` | Build your own runtime adapter. |
| `adapters-terminal` | Execute commands from terminal input. |
| `adapters-discord` | Discord-style message adapter base. |
| `adapters-brigadier` | Register a BuildMyCommand graph into Brigadier. |
| `adapters-minecraft-common` | Shared Minecraft adapter contracts and profiles. |

Platform-specific Minecraft modules are thin factories/registration helpers over the common adapter contract. Paper,
Spigot, Bungee, Velocity, Minestom, and Sponge integrations should still enter the framework through `IAdapter` or
`CommandAdapter` so dispatch, suggestions, permissions, and case policy stay consistent.

## Core Contracts

| Type | Responsibility |
| --- | --- |
| `IAdapter` | Full adapter contract. Defines capabilities, mapping, dispatch, suggestions, rendering. |
| `CommandAdapter` | High-level adapter API for native source/input/result types. |
| `SimpleCommandAdapter` | Default implementation for many runtimes. |
| `AdapterSourceMapper` | Native source -> `CommandSource`. |
| `AdapterInputMapper` | Native input -> command text/prefix/platform metadata. |
| `AdapterRenderer` | `CommandResult` -> native reply/result. |
| `AdapterConfig` | Adapter behavior settings. |
| `AdapterCapabilities` | What the host supports: permissions, rich suggestions, native registration, etc. |

Every production adapter should expose the same contract surface:

| Contract method | Required behavior |
| --- | --- |
| `mapSource(...)` | Map identity, locale, permissions, reply behavior, and native unwrap data. |
| `mapInput(...)` | Preserve raw input, normalized command text, cursor, prefix, and platform metadata. |
| `dispatch(...)` | Call the framework with mapped input. |
| `suggestRich(...)` / `suggest(...)` | Delegate to the framework suggestion engine with the correct cursor. |
| `render(...)` | Convert success, failure, and silent results without changing status semantics. |
| `execute(...)` | Dispatch then render. |

## Adapter Flow

| Step | Native side | Framework side |
| --- | --- | --- |
| 1 | Receive native event/input. | Adapter extracts command text. |
| 2 | Map source. | Adapter creates `CommandSource`. |
| 3 | Dispatch. | `CommandFramework.dispatch(...)`. |
| 4 | Render. | Adapter turns `CommandResult` into native response. |
| 5 | Suggest/register. | Adapter maps framework graph to host completion/registration if supported. |

## Minimal Custom Adapter

```java
CommandAdapter<User, Message, Reply> adapter = SimpleCommandAdapter.<User, Message, Reply>builder(framework)
    .sourceMapper(user -> new CommandSource() {
        @Override
        public Optional<String> name() {
            return Optional.of(user.name());
        }

        @Override
        public boolean hasPermission(String permission) {
            return user.permissions().contains(permission);
        }
    })
    .inputMapper(message -> message.content())
    .renderer(result -> new Reply(result.status() == CommandResult.Status.SUCCESS, result.reply().orElse("")))
    .build();
```

## Capability Table

| Capability | Meaning | Adapter impact |
| --- | --- | --- |
| Permissions | Host can check permissions. | Implement `CommandSource.hasPermission`. |
| Suggestions | Host asks for completions. | Map `framework.suggestRich(...)`. |
| Native registration | Host has a command tree API. | Export `framework.graph()` or use Brigadier adapter. |
| Rich messages | Host supports styled messages. | Render `CommandMessage` metadata/level. |
| Case policy | Host has case behavior constraints. | Respect framework matching policy or normalize input. |

## Terminal Adapter

Terminal adapter belongs in the adapter module family and is useful for demos, local tools, and testing:

```java
TerminalAdapter terminal = TerminalAdapter.attach(framework, System.in, System.out);
terminal.run();
```

| Feature | Support |
| --- | --- |
| Dispatch | Yes |
| Permissions | Depends on supplied source |
| Suggestions | Framework side available |
| Native registration | Not applicable |

## Adapter Quality Checklist

| Requirement | Why |
| --- | --- |
| Preserve raw input and cursor | Suggestions and diagnostics need exact input. |
| Map permissions explicitly | Default source permits everything. |
| Do not swallow framework failures | Render `FAILURE` distinctly. |
| Expose platform capabilities | Users need predictable behavior. |
| Test unknown command, permission denied, parse error, success | Covers the most common integration bugs. |
