# BuildMyCommand IntelliJ Plugin

This module is the in-repository home for IDE support around the BuildMyCommand route DSL.

The current slice provides:

- `@dev.riege.buildmycommand.annotation.Command`
- `@dev.riege.buildmycommand.annotation.Route`
- `@dev.riege.buildmycommand.annotation.Subcommand`
- `CommandRegistry#route(String)`
- a lightweight `BuildMyCommandRoute` language with syntax highlighting
- a bundled TextMate grammar and light/Darcula color attributes

Parser-level validation, completion, and inspections should stay in this module so IDE behavior never leaks into runtime APIs.
