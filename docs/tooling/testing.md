<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Testing

Command tests should exercise real parsing, suggestions, permissions, and middleware.

## Command Dispatch

```java
@Test
void pingWorks() {
    CommandFramework framework = CommandFramework.create();
    AnnotationCommandScanner.register(framework.registry(), new PingCommand());

    CommandResult result = framework.dispatch(source(), "wecc ping");

    assertEquals(CommandResult.Status.SUCCESS, result.status());
    assertEquals(Optional.of("pong from client"), result.reply());
}
```

## Suggestions

```java
@Test
void targetSuggestionsUseSourceMetadata() {
    CommandFramework framework = CommandFramework.create();
    AnnotationCommandScanner.register(framework.registry(), new PingCommand());

    CommandSource source = new CommandSource() {
        @Override
        public Optional<Object> metadata(String key) {
            return "players".equals(key) ? Optional.of(List.of("Ada", "Alex", "Bob")) : Optional.empty();
        }
    };

    assertEquals(List.of("Ada", "Alex"), framework.suggest(source, "wecc bang A", 11));
}
```

## Source Wrapper Tests

Test your `CommandSource` wrapper separately:

| Case | Assert |
| --- | --- |
| `name()` | Returns player/client name. |
| `unwrap(FabricClientCommandSource.class)` | Returns native source. |
| `reply(ERROR)` | Calls platform error channel. |
| `reply(SUCCESS)` | Calls platform feedback channel. |
| `hasPermission(...)` | Matches your policy. |

## Adapter Smoke Tests

Every adapter should cover:

| Scenario | Expected |
| --- | --- |
| Known command | Executes once. |
| Unknown command | Does not swallow other platform commands. |
| Incomplete command | Returns a useful parse error. |
| Suggestions | Cursor-aware suggestions are preserved. |
| Permissions | Denied commands return failure. |
| Case policy | Case-sensitive/insensitive behavior is explicit. |

## Repository Checks

```powershell
.\gradlew.bat clean check
```

The repository currently enforces full coverage for publishable modules, so new behavior should come with tests.
