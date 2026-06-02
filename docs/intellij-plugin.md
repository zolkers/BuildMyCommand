# IntelliJ Plugin Architecture

`modules/intellij-plugin` is the home for BuildMyCommand IDE support.

## Current Slice

The module declares an IntelliJ plugin descriptor and an IntelliLang injection configuration. The configuration marks BuildMyCommand DSL strings in:

- `@dev.riege.buildmycommand.annotation.Command`
- `dev.riege.buildmycommand.api.CommandRegistry#route(String)`

This gives the project a real IntelliJ plugin boundary while keeping runtime modules free of IDE code.

## Next Slice

The next plugin increment should add a BuildMyCommand route language:

1. Define a small grammar for literals, arguments, optional arguments, greedy arguments, flags, options, aliases, and known types.
2. Add syntax highlighting for route tokens.
3. Add an annotator for invalid route shapes.
4. Add completion for built-in route types and annotation attributes.
5. Add tests using the IntelliJ Platform test framework once the module adopts the IntelliJ Platform Gradle plugin.

## References

- IntelliJ language injection: https://plugins.jetbrains.com/docs/intellij/language-injection.html
- IntelliJ syntax and error highlighting: https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html
- IntelliJ custom language support: https://plugins.jetbrains.com/docs/intellij/custom-language-support.html
