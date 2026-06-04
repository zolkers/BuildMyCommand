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
<plugin id="dev.riege.buildmycommand.intellij" min-version="0.1.1" />
```

If IntelliJ opens an empty "Choose Plugins to Install or Enable" window, install the local plugin first with `installIntellijPluginLocal`, then restart.

## Marketplace Publish

Publishing to JetBrains Marketplace is separate from Maven Central. The plugin is built from `modules/intellij-plugin`.

Create a permanent token from JetBrains Marketplace, then expose it as an environment variable:

PowerShell:

```powershell
$env:JETBRAINS_MARKETPLACE_TOKEN="perm:..."
.\gradlew.bat :intellij-plugin:publishPlugin
```

Shell:

```sh
export JETBRAINS_MARKETPLACE_TOKEN="perm:..."
./gradlew :intellij-plugin:publishPlugin
```

Optional: publish to a non-default Marketplace channel:

```powershell
$env:JETBRAINS_MARKETPLACE_CHANNEL="eap"
.\gradlew.bat :intellij-plugin:publishPlugin
```

The token can also be passed as a Gradle property for local one-off usage:

```powershell
.\gradlew.bat :intellij-plugin:publishPlugin -PjetbrainsMarketplaceToken="perm:..."
```

Do not commit Marketplace tokens. For GitHub Actions, store the token as `JETBRAINS_MARKETPLACE_TOKEN`.

Before publishing, build and verify locally:

```powershell
.\gradlew.bat :intellij-plugin:clean :intellij-plugin:buildPlugin :intellij-plugin:check
```

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
