# IntelliJ Plugin

The IntelliJ plugin is optional but strongly recommended while authoring route DSL commands.

It provides:

| Feature | Covers |
| --- | --- |
| Route highlighting | `@Route`, `@SubRoute`, `registry.route(...)`, `subRoute(...)`. |
| Requirement highlighting | `@Require("staff || owner")`, builder `.requirement(...)`. |
| Inspections | Wrong annotation targets, bad method signatures, missing route context, bad suggestions. |
| Local setup scripts | Declare/install the plugin for this project. |

## Local Install

PowerShell:

```powershell
.\gradlew.bat installIntellijPluginLocal
```

Shell:

```sh
./gradlew installIntellijPluginLocal
```

Restart IntelliJ after installation.

## Project Requirement

The setup script writes `.idea/externalDependencies.xml` so IntelliJ knows this project expects the BuildMyCommand plugin:

```xml
<plugin id="dev.riege.buildmycommand.intellij" min-version="0.0.4" />
```

If IntelliJ opens an empty "Choose Plugins to Install or Enable" window, install the local plugin first with `installIntellijPluginLocal`, then restart.

## Inspections

| Example | Diagnostic |
| --- | --- |
| `@Route("x") String run(...)` | Command method must return `CommandResult`. |
| `private CommandResult run(...)` | Command method must be public or package-private. |
| `@Route("x") CommandResult run()` | Route DSL method needs one `@RouteCtx CommandContext`. |
| `@RouteCtx String ctx` | `@RouteCtx` must annotate `CommandContext`. |
| `@Command("root <target:String>")` | Use `@Route` for route DSL. |
| `@Subcommand("leaf <target:String>")` | Use `@SubRoute` for route DSL. |
| `@Permission("staff || owner")` | Use `@Require` for boolean expressions. |
| `@Suggest("missing")` | Suggestion name must match a route arg/option in the class. |
| `@Middleware(Bad.class)` | Middleware must implement `CommandMiddleware` and have no-arg constructor. |

## TextMate Theme

The plugin bundles a TextMate grammar and color scheme for route DSL tokens:

| Token | Example |
| --- | --- |
| Literal | `moderation`, `punish` |
| Alias | `moderation|mod` |
| Argument | `<target:String>` |
| Greedy | `<reason:String...>` |
| Option | `[--duration:Integer|-d]` |
| Requirement operator | `staff || owner` |

If highlighting does not appear after installing, restart IntelliJ and check that the plugin is enabled.
