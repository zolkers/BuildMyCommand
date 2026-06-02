# IntelliJ Plugin Architecture

`modules/intellij-plugin` is the home for BuildMyCommand IDE support.

## Current Slice

The module declares an IntelliJ plugin descriptor, IntelliLang injection configuration, a TextMate grammar, and light/dark color schemes. The injection configuration marks BuildMyCommand DSL strings in:

- `@dev.riege.buildmycommand.annotation.Command`
- `dev.riege.buildmycommand.api.CommandRegistry#route(String)`

The TextMate bundle describes literals, required arguments, optional arguments, greedy arguments, flags, options, aliases, and built-in route types. The color schemes map those TextMate scopes to readable light and Darcula colors.

## Next Slice

The next plugin increment should add a BuildMyCommand route language implementation:

1. Adopt the IntelliJ Platform Gradle plugin.
2. Add a `TextMateBundleProvider` for automatic bundle registration.
3. Add a PSI parser and annotator for invalid route shapes.
4. Add completion for built-in route types and annotation attributes.
5. Add tests using the IntelliJ Platform test framework.

## References

- IntelliJ language injection: https://plugins.jetbrains.com/docs/intellij/language-injection.html
- IntelliJ syntax and error highlighting: https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html
- IntelliJ custom language support: https://plugins.jetbrains.com/docs/intellij/custom-language-support.html
- IntelliJ TextMate bundles: https://www.jetbrains.com/help/idea/textmate.html
