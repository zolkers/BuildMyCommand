# Releasing

BuildMyCommand modules use Gradle `maven-publish`.

Local verification:

```powershell
.\gradlew.bat clean check
.\gradlew.bat publishToMavenLocal
.\gradlew.bat :intellij-plugin:buildPlugin
```

Publishable Java modules produce:

- main jar
- sources jar
- javadoc jar
- Maven POM metadata

The `examples` module is intentionally not published. `intellij-plugin` is built through the IntelliJ plugin distribution task, not Maven publication.

Artifact ids are derived from Gradle paths:

- `:core` -> `core`
- `:adapters:minecraft:paper` -> `adapters-minecraft-paper`
- `:discord-adapter` -> `discord-adapter`

Remote publishing should be added as a separate credentialed workflow when the target repository is chosen. The current config supports Maven Local and a per-module build-directory Maven repository.
