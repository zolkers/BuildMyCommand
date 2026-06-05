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
        return SuggestionSet.of(help.suggest(context));
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
    .argumentSuggestions("query", "visible commands", ctx -> help.suggest(SuggestionContext.from(ctx)))
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

For richer UIs, use the provider as an API instead of only calling `render(...)`.

| API | Use |
| --- | --- |
| `render(source, query, options)` | One-call text rendering. It returns command details when `query` exactly matches a visible route, otherwise a filtered page. |
| `resolve(source, query, options)` | Returns a `HelpResolution` so a UI can decide between details and a page before formatting. |
| `details(source, path)` | Returns details for one exact visible command path, or throws when the path is not visible. |
| `page(source, query, options)` | Returns a `HelpPage` filtered by query and options. |
| `page(entries, options)` | Paginates a pre-filtered entry list. |
| `entries(source)` | Returns all visible `HelpEntry` values for a source. |
| `entries(source, options)` | Returns visible entries with group and ordering options applied. |
| `suggest(context)` | Recommended for `@Suggest("query")`; understands greedy route input and suggests the next path segment. |
| `suggest(context, mode)` | Same as above, with explicit `HelpSuggestionMode`. |
| `suggest(source, HelpQuery, mode)` | Adapter/UI-level completion API when you already own the full query text and cursor. |
| `suggestPaths(source, currentToken)` | Suggests complete visible paths. Use this only when full route suggestions are desired. |
| `suggestGroups(source, currentToken)` | Suggests visible command groups for `--group`. |

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

Use `help.suggest(context)` for shell-style command suggestions and `help.suggestGroups(source, currentToken)` for group suggestions. Suggestions are permission-aware, so users do not get completion entries for commands they cannot see.

`help.suggest(...)` returns the next useful path segment instead of full command paths. For example, if visible commands include `admin audit player` and `admin reload`, then passing `admin ` suggests `audit` and `reload`, and passing `admin audit ` suggests `player`. This matches the way most command-line completion systems guide users progressively instead of flooding the menu with every full route.

Use `help.suggestPaths(source, currentToken)` only when you intentionally want full route suggestions such as `admin audit player`.

When used through `@Suggest("query")`, the provider reads `SuggestionContext.helpQuery()`, not only the last token. That matters for greedy arguments: with `@Route("help [query:String...]")`, completing `/help profile ` can suggest `message` and `view`, while completing `/help profile` can still suggest `profile`.

| Mode | Behavior |
| --- | --- |
| `HelpSuggestionMode.SEGMENT` | Suggest the next path segment. Recommended for command lines and Minecraft completion. |
| `HelpSuggestionMode.PATH` | Suggest full visible command paths. Useful for search boxes or palette-style UIs. |
| `HelpSuggestionMode.SMART` | Try segment suggestions first, then fall back to full path suggestions. |

If you build a custom adapter or UI, create a `HelpQuery` from the full help input:

```java
HelpQuery query = HelpQuery.of(textInsideHelpArgument, cursorInsideHelpArgument);
List<String> suggestions = help.suggest(source, query, HelpSuggestionMode.SEGMENT);
```
