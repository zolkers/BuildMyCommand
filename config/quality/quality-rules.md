# Quality Rules

The repository uses local Gradle quality tasks so CI does not depend on extra analysis services.

`qualityStyle` checks main Java sources for:

- trailing whitespace
- tab indentation

`qualityStaticAnalysis` checks main Java sources for:

- unresolved `TODO` or `FIXME`
- Java source files over 700 lines

JaCoCo thresholds are configured per module in `build.gradle.kts`. Core modules carry higher thresholds than native integration modules while adapters are still expanding.
