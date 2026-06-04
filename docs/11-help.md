<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Help Commands

BuildMyCommand includes a ready-to-register help command. The recommended path is still annotation-first: register your annotated commands, then scan the provided annotated help command.

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitiveLiterals()
    .caseInsensitiveOptions()
    .build();

AnnotationCommandScanner.register(framework.registry(), new ProfileCommands());
AnnotationCommandScanner.register(framework.registry(), new AdminCommands());
AnnotationCommandScanner.register(
    framework.registry(),
    new AnnotatedCommandHelp(CommandHelp.forFramework(framework))
);
```

This creates:

```text
help|h [query:String...] [--page:Integer|-p] [--size:Integer|-s] [--alphabetic|-a] [--group:String|-g]
```

## User Syntax

| Input | Result |
| --- | --- |
| `/help` | Lists visible commands, grouped by metadata. |
| `/help profile message` | Shows the exact generated help for one command. |
| `/help admin --group Administration` | Lists commands matching `admin` inside one group. |
| `/help --alphabetic` | Sorts commands alphabetically. |
| `/help --page 2 --size 5` | Shows page 2 with 5 entries per page. |

The list automatically hides commands marked with `@Hidden` and commands blocked by `@Permission` or `@Require`.

## Custom Route

Use the core registration helper when the platform already owns `/help`, or when your framework should expose help under a namespaced command. This path uses the builder internally because Java annotation values cannot be runtime strings.

```java
CommandHelp.forFramework(framework)
    .title("WECC Commands")
    .footer("Use /wecc help <command> for details.")
    .register("wecc help|h [query:String...] [--page:Integer|-p] [--size:Integer|-s] [--alphabetic|-a] [--group:String|-g]");
```

## Custom Formatting

The help system does not force one visual style. Provide a formatter to render pages and command details however your platform expects:

```java
CommandHelp help = CommandHelp.forFramework(framework)
    .formatter(new CommandHelpFormatter() {
        @Override
        public String formatPage(CommandHelpPage page) {
            return page.entries().stream()
                .map(entry -> "- /" + entry.path() + ": " + entry.description())
                .collect(Collectors.joining("\n"));
        }

        @Override
        public String formatDetails(String title, String details) {
            return details;
        }
    });

AnnotationCommandScanner.register(framework.registry(), new AnnotatedCommandHelp(help));
```

## Programmatic Rendering

Adapters and UIs can render help without registering a command:

```java
CommandHelp help = CommandHelp.forFramework(framework);

String page = help.render(
    source,
    "",
    CommandHelpOptions.builder()
        .page(1)
        .pageSize(10)
        .alphabetic(true)
        .group("Administration")
        .build()
);

String details = help.render(source, "profile message", CommandHelpOptions.defaults());
```

## Options

| API | CLI flag | Default | Meaning |
| --- | --- | --- | --- |
| `page(int)` | `--page`, `-p` | `1` | Page number, clamped to the last available page. |
| `pageSize(int)` | `--size`, `-s` | `8` | Number of commands per page. |
| `alphabetic(boolean)` | `--alphabetic`, `-a` | `false` | Sort by command path instead of registry order. |
| `group(String)` | `--group`, `-g` | empty | Show one metadata group only. |
| `showAliases(boolean)` | none | `true` | Include literal aliases in rendered rows. |
| `commandPrefix(String)` | none | `/` | Prefix displayed before command paths. |

## Suggestions

The registered help command suggests visible command paths for `query` and visible group names for `--group`. Suggestions are permission-aware, so users do not get completion entries for commands they cannot see.
