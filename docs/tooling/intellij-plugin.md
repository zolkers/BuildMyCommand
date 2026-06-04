<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# IntelliJ Plugin

The IntelliJ plugin is optional but strongly recommended while authoring route DSL commands.

It provides:

| Feature | Covers |
| --- | --- |
| Route highlighting | `@Route`, `@SubRoute`, `registry.route(...)`, `subRoute(...)`. |
| Requirement highlighting | `@Require("staff \|\| owner")`, builder `.requirement(...)`. |
| Permission regex highlighting | `@Permission(value = "admin\\..*", regex = true)`, builder `.permissionRegex(...)`. |
| Inspections | Wrong annotation targets, bad method signatures, missing route context, bad suggestions. |
| Custom route types | Project-level `.type(...)` and `.types(...register...)` registrations. |
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
<plugin id="dev.riege.buildmycommand.dsl" min-version="0.2.3" />
```

If IntelliJ opens an empty "Choose Plugins to Install or Enable" window, install the local plugin first with `installIntellijPluginLocal`, then restart.

## Custom DSL Types

The plugin indexes Java files in the current project and accepts custom route types registered through the public framework API:

```java
CommandFramework framework = CommandFramework.builder()
    .type("Material", Material.class, new MaterialParser())
    .types(types -> types.register("ItemStack", ItemStack.class, new ItemStackParser()))
    .build();
```

After that, the route DSL is understood without additional IDE setup:

```java
@SubRoute("give <item:Material> [--stack:ItemStack|-s]")
CommandResult give(@RouteCtx CommandContext ctx) {
    Material item = ctx.arg("item", Material.class);
    return Results.success("giving " + item);
}
```

Recommended project shape:

| Pattern | Why |
| --- | --- |
| Keep type registrations in one setup class. | Easier to review, easier for the IDE to index. |
| Use stable aliases such as `Material`, `World`, `Profile`. | The alias is part of your public command DSL. |
| Register before scanning annotations. | Runtime route parsing needs the alias before commands are imported. |
| Put suggestions in the parser. | The same parser powers runtime parsing and platform suggestions. |

The plugin intentionally understands direct calls to `.type("Alias", SomeClass.class, parser)` and `.register("Alias", SomeClass.class, parser)`. It does not execute Java code, follow arbitrary reflection, or read runtime-only configuration.

## Marketplace Publish

Publishing to JetBrains Marketplace is separate from Maven Central. The plugin is built from `modules/intellij-plugin`.

Marketplace plugin id:

```text
dev.riege.buildmycommand.dsl
```

JetBrains requires the first plugin version to be uploaded manually from the Marketplace UI. This creates the plugin page and lets you set required metadata such as license and repository URL. After that first upload, `publishPlugin` can push later versions.

Build the first zip:

```powershell
.\gradlew.bat :intellij-plugin:clean :intellij-plugin:buildPlugin
```

Upload the newest zip from:

```text
modules/intellij-plugin/build/distributions/
```

Create a permanent token from JetBrains Marketplace, then expose it as an environment variable:

PowerShell:

```powershell
$env:JETBRAINS_MARKETPLACE_TOKEN="perm:..."
.\gradlew.bat :intellij-plugin:publishPlugin
```

To persist the token locally and in GitHub Actions without committing it:

```powershell
.\scripts\setup-jetbrains-marketplace-token.ps1
```

If the token is already in the current PowerShell session, the script reuses it:

```powershell
$env:JETBRAINS_MARKETPLACE_TOKEN="perm:..."
.\scripts\setup-jetbrains-marketplace-token.ps1
```

Shell:

```sh
export JETBRAINS_MARKETPLACE_TOKEN="perm:..."
./gradlew :intellij-plugin:publishPlugin
```

Shell helper:

```sh
./scripts/setup-jetbrains-marketplace-token.sh
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
| `@Permission("staff \|\| owner")` | Use `@Require` for boolean expressions. |
| `@Permission(value = "[", regex = true)` | Permission regex must compile as a Java regex. |
| `.permissionRegex("[")` | Permission regex must compile as a Java regex. |
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
| Requirement operator | `staff \|\| owner` |
| Permission regex | `admin\\.audit\\..*` |

If highlighting does not appear after installing, restart IntelliJ and check that the plugin is enabled.
