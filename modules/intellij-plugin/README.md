# BuildMyCommand IntelliJ Plugin

This module is the in-repository home for IDE support.

The first slice uses IntelliJ IntelliLang configuration to mark BuildMyCommand DSL strings:

- `@dev.riege.buildmycommand.annotation.Command`
- `CommandRegistry#route(String)`

The next architectural step is a real IntelliJ Platform module with a BuildMyCommand language, parser, annotator, and completion contributor. That should live in this module instead of in `core`, so IDE behavior never leaks into runtime APIs.
