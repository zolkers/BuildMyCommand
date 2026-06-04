<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Building Help Commands

BuildMyCommand does not force one universal help command. Instead, `core` gives you reusable help building blocks so each project can expose help in the shape that fits its platform, language, UI, and command namespace.

The usual pattern is:

1. Register your real commands.
2. Create `framework.helpProvider()`.
3. Write your own `@Route` or `@SubRoute` help command.
4. Delegate listing, pagination, details, filters, and suggestions to `HelpProviderAPI`.

## Annotation Pattern

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitiveLiterals()
    .caseInsensitiveOptions()
    .build();

AnnotationCommandScanner.register(framework.registry(), new ProfileCommands());
AnnotationCommandScanner.register(framework.registry(), new AdminCommands());
AnnotationCommandScanner.register(framework.registry(), new HelpCommands(framework.helpProvider()));
```

Then define the help command as part of your own command surface:

```java
@CommandGroup("System")
@CaseInsensitive(literals = true, options = true)
final class HelpCommands {
    private final HelpProviderAPI help;

    HelpCommands(HelpProviderAPI help) {
        this.help = help;
    }

    @Route(HelpProviderAPI.DEFAULT_ROUTE)
    @Description("Show visible commands or inspect one command")
    @Usage("/help [command] [--page <page>] [--size <size>] [--alphabetic] [--group <group>]")
    @Example({"/help", "/help profile message", "/help --alphabetic --page 2"})
    CommandResult help(@RouteCtx CommandContext route) {
        String query = route.optionalArg("query", String.class).orElse("");
        return Results.success(help.render(route.source(), query, HelpOptions.from(route)));
    }

    @Suggest("query")
    SuggestionSet commands(SuggestionContext context) {
        return SuggestionSet.of(help.suggest(context.source(), context.currentToken())).filteringCurrentToken();
    }

    @Suggest("group")
    SuggestionSet groups(SuggestionContext context) {
        return SuggestionSet.of(help.suggestGroups(context.source(), context.currentToken())).filteringCurrentToken();
    }
}
```

That route is only the default pattern. You can inline your own route string instead and name it `help`, `wecc help`, `commands`, `?`, or anything else.

## Example User Syntax

| Input | Result |
| --- | --- |
| `/help` | Lists visible commands, grouped by metadata. |
| `/help profile message` | Shows the exact generated help for one command. |
| `/help admin --group Administration` | Lists commands matching `admin` inside one group. |
| `/help --alphabetic` | Sorts commands alphabetically. |
| `/help --page 2 --size 5` | Shows page 2 with 5 entries per page. |

The list automatically hides commands marked with `@Hidden` and commands blocked by `@Permission` or `@Require`.

## Builder Pattern

If you are not using annotations, use the exposed default route pattern with the builder API:

```java
HelpProviderAPI help = framework.helpProvider()
    .title("WECC Commands")
    .footer("Use /help <command> for details.");

framework.registry()
    .route(HelpProviderAPI.DEFAULT_ROUTE)
    .description("Show command help")
    .argumentSuggestions("query", "visible commands", ctx -> help.suggest(ctx.source(), ctx.rawToken()))
    .optionSuggestions("group", "help groups", ctx -> help.suggestGroups(ctx.source(), ctx.rawToken()))
    .executes(ctx -> Results.success(help.render(
        ctx.source(),
        ctx.optionalArg("query", String.class).orElse(""),
        HelpOptions.from(ctx)
    )));
```

Use a custom pattern such as `wecc help|h ...` when the platform already owns `/help`, or when your app has a namespaced command tree.

## Custom Formatting

The help system does not force one visual style. Provide a formatter to render pages and command details however your platform expects:

```java
HelpProviderAPI help = framework.helpProvider()
    .formatter(new HelpFormatter() {
        @Override
        public String formatPage(HelpPage page) {
            return page.entries().stream()
                .map(entry -> "- /" + entry.path() + ": " + entry.description())
                .collect(Collectors.joining("\n"));
        }

        @Override
        public String formatDetails(String title, String details) {
            return details;
        }
    });

AnnotationCommandScanner.register(framework.registry(), new HelpCommands(help));
```

## Programmatic Rendering

Adapters and UIs can render help without registering a command:

```java
HelpProviderAPI help = framework.helpProvider();

String page = help.render(
    source,
    "",
    HelpOptions.builder()
        .page(1)
        .pageSize(10)
        .alphabetic(true)
        .group("Administration")
        .build()
);

String details = help.render(source, "profile message", HelpOptions.defaults());
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

Use `help.suggest(source, currentToken)` for command path suggestions and `help.suggestGroups(source, currentToken)` for group suggestions. Suggestions are permission-aware, so users do not get completion entries for commands they cannot see.
