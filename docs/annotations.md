# Annotations

Annotation commands are scanned into the same command model as builder and DSL commands.

```java
final class ModerationCommands {
    @Route("ban|block <target:String> <reason:String...> [--silent|-s]")
    @Description("Ban a user")
    @Permission("mod.ban")
    @Usage("/ban <target> <reason>")
    @Example("/ban Ada spam")
    CommandResult ban(
        @Arg("target") String target,
        @Arg("reason") String reason,
        @Flag("silent") boolean silent
    ) {
        return Results.success(target + ":" + reason + ":" + silent);
    }
}

AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());
```

Available annotations include:

- `@Command`, `@Route`, `@Subcommand`, `@Alias`
- `@Arg`, `@Option`, `@Flag`, `@Greedy`, `@OptionalArg`, `@Default`
- `@Description`, `@Permission`, `@Hidden`, `@Usage`, `@Example`
- `@Cooldown`, `@Require`, `@CommandGroup`, `@Suggest`
- `@CaseInsensitive`

The scanner delegates binding and validation to annotation compiler components so annotation discovery does not mutate the registry until the command shape has been checked.
