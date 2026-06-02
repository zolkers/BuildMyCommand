# IntelliJ Plugin Architecture

`modules/intellij-plugin` is the home for BuildMyCommand IDE support.

## Current Slice

The module declares an IntelliJ plugin descriptor, IntelliLang injection configuration, a lightweight BuildMyCommand route language, a syntax highlighter, a TextMate grammar, and light/dark color schemes. The injection configuration marks BuildMyCommand DSL strings in:

- `@dev.riege.buildmycommand.annotation.Command`
- `@dev.riege.buildmycommand.annotation.Route`
- `@dev.riege.buildmycommand.annotation.Subcommand`
- `dev.riege.buildmycommand.api.CommandRegistry#route(String)`

Injected strings use the `BuildMyCommandRoute` language so annotations and route builder calls receive syntax highlighting directly in Java string literals. The TextMate bundle still describes the same DSL for bundled grammar support: literals, required arguments, optional arguments, greedy arguments, flags, options, aliases, and built-in route types. The color schemes map both the custom highlighter keys and TextMate scopes to readable light and Darcula colors.

## Project Setup

Run `.\gradlew.bat setupIntellijPlugin` from the repository root. The task builds the local IntelliJ plugin ZIP and refreshes `.idea/externalDependencies.xml` so IntelliJ marks `dev.riege.buildmycommand.intellij` as a required project plugin.

For a pure script run, use `.\scripts\setup-intellij-plugin.ps1`. By default it also runs `:intellij-plugin:buildPlugin`; pass `-SkipBuild` if Gradle already built the plugin.

On Unix-like shells, use `./scripts/setup-intellij-plugin.sh`. It has the same behavior; pass `--skip-build` if Gradle already built the plugin.

IntelliJ reads `.idea/externalDependencies.xml` when the project opens and notifies if the required plugin is missing, disabled, or too old. The generated plugin ZIP is under `modules/intellij-plugin/build/distributions/` and can be installed with `Settings > Plugins > gear > Install Plugin from Disk...`.

## Next Slice

The next plugin increment should deepen the language implementation:

1. Add a PSI parser and annotator for invalid route shapes.
2. Add completion for built-in route types and annotation attributes.
3. Add inspections that compare `@Route` parameters with method bindings.
4. Add IntelliJ Platform fixture tests for highlighting, injection, and completion.

## References

- IntelliJ language injection: https://plugins.jetbrains.com/docs/intellij/language-injection.html
- IntelliJ syntax and error highlighting: https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html
- IntelliJ custom language support: https://plugins.jetbrains.com/docs/intellij/custom-language-support.html
- IntelliJ TextMate bundles: https://www.jetbrains.com/help/idea/textmate.html
