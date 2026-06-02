# Annotations

Annotation commands are scanned into the same command model as builder and DSL commands.

```java
final class ModerationCommands {
    @Route("ban|block <target:String> <reason:String...> [--silent|-s]")
    @Description("Ban a user")
    @Permission("mod.ban")
    @Usage("/ban <target> <reason>")
    @Example("/ban Ada spam")
    CommandResult ban(@RouteCtx CommandContext route) {
        return Results.success(
            route.arg("target", String.class)
                + ":"
                + route.arg("reason", String.class)
                + ":"
                + route.flag("silent")
        );
    }
}

AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());
```

Available annotations include:

- `@Command`, `@Route`, `@Subcommand`, `@Alias`
- `@RouteCtx`
- `@Arg`, `@Option`, `@Flag`, `@Greedy`, `@OptionalArg`, `@Default`
- `@Description`, `@Permission`, `@Hidden`, `@Usage`, `@Example`
- `@Cooldown`, `@Require`, `@CommandGroup`, `@Suggest`
- `@CaseInsensitive`

The scanner delegates binding and validation to annotation compiler components so annotation discovery does not mutate the registry until the command shape has been checked.

`@Route` and class-level `@Subcommand` methods use the DSL as the single source of truth. They should declare exactly one `@RouteCtx CommandContext` parameter and read parsed values from that context. `@Arg`, `@Option`, and `@Flag` are for non-DSL `@Command` methods.

The IntelliJ plugin reports this distinction directly on route methods, so mixed declarations are visible while editing.
