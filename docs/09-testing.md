# 09 - Testing

BuildMyCommand is designed to be tested without launching a full platform.

## Test Levels

| Level | Test target | Example |
| --- | --- | --- |
| Unit | Parser, metadata, builders, middleware. | `CommandMetadata.Builder` validation. |
| Runtime | `CommandFramework` dispatch. | Dispatch input and assert `CommandResult`. |
| Annotation | Scanner/compiler behavior. | Register annotated class and inspect schema/results. |
| Adapter | Native source/input/result mapping. | Discord/terminal/Minecraft adapter tests. |
| Smoke examples | Public examples compile and run. | `ExampleSmokeTest`. |

## Dispatch Test

```java
@Test
void givesKit() {
    CommandFramework framework = CommandFramework.create();
    AnnotationCommandScanner.register(framework.registry(), new KitCommands());

    CommandResult result = framework.dispatch(source("kit.give"), "kit Ada starter -a 3");

    assertEquals(CommandResult.Status.SUCCESS, result.status());
    assertEquals(Optional.of("Giving 3x starter to Ada"), result.reply());
}
```

## Test Source

```java
private static CommandSource source(String... permissions) {
    return new CommandSource() {
        @Override
        public boolean hasPermission(String permission) {
            return List.of(permissions).contains(permission);
        }
    };
}
```

## What Every Command Feature Should Test

| Feature | Required tests |
| --- | --- |
| Root alias | Canonical root and alias dispatch. |
| Deep path | Full path dispatch and unknown child failure. |
| Required arg | Success and missing arg failure. |
| Greedy arg | Spaces are preserved. |
| Option | Long and short alias parse. |
| Flag | Long and short alias parse. |
| Permission | Allowed and denied source. |
| Middleware | Runs in order and can block. |
| Suggestions | Cursor-specific suggestions. |
| Case-insensitive policy | Mixed-case input succeeds only when configured. |

## Coverage Policy

The repository currently enforces 100% JaCoCo coverage per publishable module:

```powershell
.\gradlew.bat check
```

| Check | Purpose |
| --- | --- |
| `test` | Runs JUnit tests. |
| `jacocoTestCoverageVerification` | Enforces full module coverage. |
| `qualityStyle` | Rejects trailing whitespace and tabs. |
| `qualityStaticAnalysis` | Rejects TODO/FIXME and oversized Java files. |

## Adapter Test Checklist

| Case | Why |
| --- | --- |
| Native input with prefix | Prefix handling differs by platform. |
| Native input without prefix | Some runtimes already isolate command text. |
| Permission denied | Source mapper must be correct. |
| Unknown command | Renderer must preserve failures. |
| Successful command | End-to-end baseline. |
| Suggestions | Cursor/range bugs are common. |
| Native registration | Platform command managers often treat aliases specially. |

## IntelliJ Plugin Tests

Plugin tests should cover:

| Area | Example |
| --- | --- |
| Injection | `@Route`/`@SubRoute` strings are recognized. |
| Lexer/highlighter | Tokens receive expected categories. |
| Inspections | Invalid routes report issues. |
| Resources | Theme, TextMate grammar, plugin xml exist. |
| External dependency setup | `.idea/externalDependencies.xml` declares required plugin. |
