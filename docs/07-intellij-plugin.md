# 07 - IntelliJ Plugin

The IntelliJ plugin exists to make route DSL strings feel like real code instead of invisible Java string literals.

## Features

| Feature | Status |
| --- | --- |
| Route DSL injection in `@Route` | Supported |
| Route DSL injection in `@SubRoute` | Supported |
| Route DSL injection in builder `.route(...)` | Supported |
| Syntax highlighting/theme | Supported |
| Route inspections | Supported |
| Required plugin declaration | Supported through setup task/script |
| Implicit usage for command annotations | Supported |
| Requirement/permission DSL highlighting | Planned/recommended next step |

## Install Locally

```powershell
.\gradlew.bat installIntellijPluginLocal
```

This builds the plugin ZIP and installs it into the latest local IntelliJ IDEA config directory. Restart IntelliJ after installation.

## Project Setup

```powershell
.\gradlew.bat setupIntellijPlugin
```

This declares the plugin as required in `.idea/externalDependencies.xml` and builds the plugin ZIP.

Shell alternatives:

```bash
./scripts/setup-intellij-plugin.sh
./scripts/setup-intellij-plugin.sh --install
```

## Files

| File | Purpose |
| --- | --- |
| `modules/intellij-plugin/src/main/resources/buildmycommandInjections.xml` | IntelliLang injection declarations. |
| `modules/intellij-plugin/src/main/resources/textmate/...` | TextMate grammar for route DSL. |
| `modules/intellij-plugin/src/main/resources/colorSchemes/...` | Light/Darcula route color schemes. |
| `BuildMyCommandRouteDsl` | Parser/validator model used by inspections/tests. |
| `BuildMyCommandRouteInspection` | Reports route DSL issues. |
| `BuildMyCommandRouteAnnotator` | Adds editor annotations/highlighting. |
| `BuildMyCommandImplicitUsageProvider` | Prevents annotated command classes/methods from being marked unused. |

## Route Highlighting

| Token | Expected visual treatment |
| --- | --- |
| Literals | Command path words. |
| Aliases | Same literal segment, separated by `|`. |
| Argument brackets | `<`, `>`, `[` and `]`. |
| Argument names | `target`, `reason`, etc. |
| Type names | `String`, `Integer`, custom parser names. |
| Options | `--duration`, `--silent`. |
| Short aliases | `-d`, `-s`. |
| Greedy marker | `...`. |

## Inspection Examples

| Input | Issue |
| --- | --- |
| `cmd [<a:String>] <b:String>` | Required argument cannot follow optional argument. |
| `cmd <reason:String...> <x:String>` | Greedy argument must be last. |
| `cmd [--silent|-]` | Invalid short flag alias. |
| `cmd <>` | Invalid argument declaration. |

## Recommended Next Plugin Rule

`@Require("staff || owner")` and `@Permission("admin.reload")` should get dedicated support.

| Annotation | Plugin behavior |
| --- | --- |
| `@Require` | Inject boolean requirement DSL: identifiers, `&&`, `||`, `!`, parentheses. |
| `@Permission` | Treat as one permission node. Warn on boolean operators. |
| Builder `.requirement(...)` | Same as `@Require`. |
| Builder `.permission(...)` | Same as `@Permission`. |

This distinction keeps `@Permission` simple and makes `@Require` the explicit place for complex access logic.
