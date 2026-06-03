package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.annotation.binding.AnnotationCommandCompiler;
import dev.riege.buildmycommand.annotation.binding.MethodCommandBinder;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotationCommandScannerTest {
    @Test
    void registersAnnotatedCommandWithStringArgumentAndBooleanFlag() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());

        CommandResult result = framework.dispatch(source(), "ban Steve --silent");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Steve:true"), result.reply());
    }

    @Test
    void registersAnnotatedCommandWithIntegerArgumentAndContext() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new NumberCommands());

        CommandResult result = framework.dispatch(source(), "level Alex 7");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("level Alex=7 via level Alex 7"), result.reply());
    }

    @Test
    void rejectsUnsupportedAnnotatedParameterAtRegistration() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new InvalidCommands()));

        assertEquals("unsupported annotated command parameter: reason", exception.getMessage());
    }

    @Test
    void registersMultipleAnnotatedCommandsInDeterministicCommandOrder() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new OrderedCommands());

        assertEquals("""
            command alpha
            command zeta""", framework.schema());
    }

    @Test
    void registersAnnotatedCommandMetadata() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new MetadataCommands());

        assertEquals("""
            Usage: reload
            Description: Reload configuration""", framework.help("reload"));
        assertEquals("""
            command reload
              description Reload configuration
              permission admin.reload""", framework.schema());

        CommandResult result = framework.dispatch(source(), "reload");
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("reloaded"), result.reply());
    }

    @Test
    void rejectsBlankAnnotatedCommandMetadata() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException description = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new BlankDescriptionCommands()));
        IllegalArgumentException permission = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new BlankPermissionCommands()));

        assertEquals("description must not be blank", description.getMessage());
        assertEquals("permission must not be blank", permission.getMessage());
    }

    @Test
    void registersAnnotatedRouteDslWithArgumentsFlagAndValuedOption() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new InventoryCommands());

        CommandResult result = framework.dispatch(source(), "inventory give Ada diamond -a 3 --silent");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Ada gets 3 diamond silently=true"), result.reply());
        assertEquals("""
            Usage: inventory give <target:String> <item:String> [--amount:Integer|-a] [--silent|-s]
            Description: Give an item""", framework.help("inventory give"));
    }

    @Test
    void registersAnnotatedRouteCommandAliasAndDslOptionAliases() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new RouteAliasCommands());

        CommandResult result = framework.dispatch(source(), "block Ada -d 30 -s");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Ada:30:true"), result.reply());
        assertEquals("Usage: ban <target:String> [--duration:Integer|-d] [--silent|-s]", framework.help("block"));
    }

    @Test
    void rejectsMethodAnnotatedAsBothLiteralCommandAndRouteDsl() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MixedRouteCommands()));

        assertEquals("annotated command method cannot mix command route annotations: mixed", exception.getMessage());
    }

    @Test
    void registersAliasOptionalGreedyAndDefaultParameterAnnotations() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new MessagingCommands());

        CommandResult defaulted = framework.dispatch(source(), "msg Ada");
        CommandResult explicit = framework.dispatch(source(), "message Ada hello there");

        assertEquals(Optional.of("Ada:No message"), defaulted.reply());
        assertEquals(Optional.of("Ada:hello there"), explicit.reply());
        assertEquals("Usage: msg <target:String> [message:String...]", framework.help("msg"));
    }

    @Test
    void registersClassCommandWithMethodSubcommand() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new ServerCommands());

        CommandResult result = framework.dispatch(source(), "server reload");

        assertEquals(Optional.of("reloaded"), result.reply());
        assertEquals("Usage: server reload", framework.help("server reload"));
    }

    @Test
    void registersClassCommandWithNestedSubcommandGroups() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new TeamCommands());

        CommandResult grant = framework.dispatch(source(), "t m perm g Ada build.fly");
        CommandResult route = framework.dispatch(source(), "team member permission put Ada build.admin -t");

        assertEquals(Optional.of("grant Ada build.fly"), grant.reply());
        assertEquals(Optional.of("set Ada build.admin temporary=true"), route.reply());
        assertEquals("Usage: team member permission grant <target:String> <permission:String>",
            framework.help("team member permission grant"));
    }

    @Test
    void registersNonStaticNestedSubcommandGroups() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new NonStaticNestedCommands());

        assertEquals(Optional.of("inner"), framework.dispatch(source(), "outer inner leaf").reply());
    }

    @Test
    void rejectsNestedSubcommandGroupWithoutUsableConstructor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new BrokenNestedCommands()));

        assertEquals(true, exception.getMessage().startsWith("cannot instantiate nested @Subcommand group: "));
    }

    @Test
    void registersClassCommandWithMethodSubRoute() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new UserCommands());

        CommandResult result = framework.dispatch(source(), "user rank set Ada admin");

        assertEquals(Optional.of("Ada=admin"), result.reply());
    }

    @Test
    void registersClassCommandAndSubRouteAliases() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new AliasedUserCommands());

        CommandResult result = framework.dispatch(source(), "u roles put Ada admin");

        assertEquals(Optional.of("Ada=admin"), result.reply());
        assertEquals("Usage: user rank set <target:String> <rank:String>", framework.help("u roles put"));
    }

    @Test
    void rejectsSubcommandDslAndPointsToSubRoute() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new DslSubcommandCommands()));

        assertEquals("@Subcommand only accepts one literal; use @SubRoute for route DSL: rank set <target:String>",
            exception.getMessage());
    }

    @Test
    void rejectsCommandDslAndPointsToRoute() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new DslCommandCommands()));

        assertEquals("@Command only accepts one literal; use @Route for route DSL: ban <target:String>",
            exception.getMessage());
    }

    @Test
    void caseInsensitiveAnnotationEnablesLiteralAndOptionMatching() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new CaseInsensitiveCommands());

        CommandResult result = framework.dispatch(source(), "BAN Ada -S --Duration 5");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Ada:5:true"), result.reply());
    }

    @Test
    void compilerExposesFutureAnnotationMetadataBeforeRegistryMutation() {
        AnnotationCommandCompiler.CompiledCommands compiled =
            AnnotationCommandCompiler.compile(new FutureMetadataCommands());

        AnnotationCommandCompiler.CompiledCommand command = compiled.commands().get(0);
        MethodCommandBinder.CommandMetadata metadata = command.metadata();

        assertEquals("secret", command.route());
        assertEquals(Optional.of("moderation"), command.group());
        assertEquals(true, metadata.hidden());
        assertEquals(Optional.of("secret <target>"), metadata.usage());
        assertIterableEquals(List.of("secret Ada", "secret Bob"), metadata.examples());
        assertEquals(Optional.of(Duration.ofSeconds(30)), metadata.cooldown());
        assertEquals(Optional.of("perm.a && perm.b"), metadata.requirement());
        assertEquals(Optional.of("players"), command.bindings().get(0).suggestionProvider());
    }

    @Test
    void appliesHiddenUsageExamplesCooldownRequirementAndSuggestionMetadata() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new FutureMetadataCommands());

        assertEquals("Unknown command: secret", framework.help(permittedSource("perm.a", "perm.b"), "secret"));
        assertEquals(List.of(), framework.suggest(source(), "s", 1));
        assertEquals("Missing permission: perm.a && perm.b",
            framework.dispatch(permittedSource(), "secret Ada").reply().orElseThrow());
        assertEquals(Optional.of("Ada"), framework.dispatch(permittedSource("perm.a", "perm.b"), "secret Ada").reply());

        assertEquals("""
            command secret
              hidden true
              group moderation
              usage secret <target>
              example secret Ada
              example secret Bob
              cooldown PT30S
              require perm.a && perm.b
              argument target:String required suggest=players""", framework.schema());
    }

    @Test
    void rendersVisibleAnnotationUsageExamplesAndNamedSuggestions() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new VisibleMetadataCommands());

        assertEquals("""
            Usage: visible <player>
            Description: Show player info
            Example: visible Ada
            Example: visible Bob""", framework.help(permittedSource("perm.visible"), "visible"));
        assertEquals(List.of(
            new Suggestion("Ada", Optional.of("online"), 8, 8, SuggestionType.ARGUMENT, 10),
            new Suggestion("Bob", Optional.empty(), 8, 8, SuggestionType.ARGUMENT, 0)
        ), framework.suggestRich(CommandInput.raw(permittedSource("perm.visible"), "visible ")));
        assertEquals("""
            command visible
              description Show player info
              usage visible <player>
              example visible Ada
              example visible Bob
              cooldown PT5S
              require perm.visible
              argument target:String required suggest=players""", framework.schema());
    }

    @Test
    void metadataAnnotationsDeclareConsistentClassAndMethodTargets() {
        assertTargets(Subcommand.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Description.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Permission.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Usage.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Example.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Cooldown.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Hidden.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Require.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Middleware.class, ElementType.METHOD, ElementType.TYPE);
    }

    @Test
    void classLevelMetadataAppliesToCommandRootWithoutOverwritingLeaves() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new ClassMetadataCommands());

        assertEquals("""
            Usage: admin
            Description: Admin root
            Example: admin help""", framework.help(permittedSource("admin.root"), "admin"));
        assertEquals("Missing permission: admin.root",
            framework.dispatch(permittedSource("admin.reload"), "admin reload").reply().orElseThrow());

        CommandResult result = framework.dispatch(permittedSource("admin.root", "admin.reload"), "admin reload");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("reloaded"), result.reply());
        assertEquals("""
            command admin
              description Admin root
              permission admin.root
              usage admin
              example admin help
              cooldown PT5S
              require admin.root
              child reload
            command admin reload
              description Reload leaf
              permission admin.reload
              require admin.root""", framework.schema());
    }

    @Test
    void annotationMiddlewareAppliesToClassAndMethodCommandsInPathOrder() {
        MIDDLEWARE_EVENTS.clear();
        CommandFramework framework = CommandFramework.builder()
            .middleware((context, command, path, next) -> {
                MIDDLEWARE_EVENTS.add("global:" + String.join("/", path));
                return next.proceed(context);
            })
            .build();

        AnnotationCommandScanner.register(framework.registry(), new MiddlewareAnnotatedCommands());

        CommandResult ban = framework.dispatch(source(), "secure ban Ada");
        CommandResult status = framework.dispatch(source(), "secure status");

        assertEquals(Optional.of("banned Ada"), ban.reply());
        assertEquals(Optional.of("ok"), status.reply());
        assertEquals(List.of(
            "global:secure/ban",
            "class:secure/ban",
            "method:secure/ban",
            "executor:ban",
            "method-after",
            "class-after",
            "global:secure/status",
            "class:secure/status",
            "executor:status",
            "class-after"
        ), MIDDLEWARE_EVENTS);
    }

    @Test
    void annotationMiddlewareAppliesToContainerRouteClasses() {
        MIDDLEWARE_EVENTS.clear();
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new RouteMiddlewareCommands());

        CommandResult result = framework.dispatch(source(), "audit Ada");

        assertEquals(Optional.of("audit Ada"), result.reply());
        assertEquals(List.of("class:audit", "executor:route", "class-after"), MIDDLEWARE_EVENTS);
    }

    @Test
    void rejectsAnnotationMiddlewareWithoutNoArgConstructor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new BrokenMiddlewareCommands()));

        assertEquals(true, exception.getMessage().startsWith("command middleware must declare a no-arg constructor: "));
    }

    @Test
    void rejectsAnnotationMiddlewareThatFailsConstruction() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(),
                new ThrowingMiddlewareCommands()));

        assertEquals(true, exception.getMessage().startsWith("cannot instantiate command middleware: "));
    }

    @Test
    void registryDefaultsRejectApplicativeMetadataInsteadOfIgnoringSecurity() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
            () -> AnnotationCommandScanner.register(new UnsupportedMetadataRegistry(), new RequireOnlyCommands()));

        assertEquals("command requirements are not supported by this registry", exception.getMessage());
    }

    @Test
    void rejectsAmbiguousNamedSuggestionProviderMethods() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new AmbiguousSuggestCommands()));

        assertEquals("ambiguous suggestion provider: players", exception.getMessage());
    }

    @Test
    void rejectsNullSuggestionProviderElementsWithIndex() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new NullSuggestionCommands());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> framework.suggestRich(CommandInput.raw(source(), "nulls ")));

        assertEquals("suggestion provider players returned null at index 1", exception.getMessage());
    }

    @Test
    void rejectsMixedSuggestionProviderElementTypesWithIndex() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new MixedSuggestionCommands());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> framework.suggestRich(CommandInput.raw(source(), "mixed ")));

        assertEquals("suggestion provider players returned mixed element at index 1", exception.getMessage());
    }

    @Test
    void rejectsRouteWithoutRouteContextBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MissingRouteArgumentCommands()));

        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: ban",
            exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsRouteMixedWithArgumentAnnotationsBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MissingAnnotatedArgumentCommands()));

        assertEquals("@Route method cannot use @Arg parameters; read route values from @RouteCtx CommandContext: ban",
            exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsRouteWithMultipleRouteContextsBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MultipleRouteCtxCommands()));

        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: ban",
            exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsInlineEnumRouteTypesWithClearAnnotationValidationError() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new InlineEnumRouteCommands()));

        assertEquals("route argument mode uses analysis-only type enum(a,b)", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsConstrainedRouteTypesWithClearAnnotationValidationError() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new ConstrainedRouteCommands()));

        assertEquals("route argument amount uses analysis-only type Integer{1..5}", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void infersArgumentNamesFromParametersWhenAvailable() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new InferredParameterCommands());

        CommandResult result = framework.dispatch(source(), "ban Ada repeated griefing");

        assertEquals(Optional.of("Ada:repeated griefing"), result.reply());
        assertEquals("Usage: ban <target:String> <reason:String...>", framework.help("ban"));
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }

    private static CommandSource permittedSource(String... permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return List.of(permissions).contains(permission);
            }
        };
    }

    private static void assertTargets(Class<?> annotation, ElementType... expectedTargets) {
        Target target = annotation.getAnnotation(Target.class);
        assertNotNull(target, annotation.getName());
        assertEquals(Set.of(expectedTargets), Set.of(target.value()));
    }

    private static final List<String> MIDDLEWARE_EVENTS = new java.util.concurrent.CopyOnWriteArrayList<>();

    static final class ModerationCommands {
        @Command("ban")
        CommandResult ban(@Arg("target") String target, @Flag("silent") boolean silent) {
            return Results.success(target + ":" + silent);
        }
    }

    static final class NumberCommands {
        @Command("level")
        public CommandResult level(CommandContext context, @Arg("target") String target, @Arg("amount") Integer amount) {
            return Results.success("level " + target + "=" + amount + " via " + context.input());
        }
    }

    static final class InvalidCommands {
        @Command("bad")
        CommandResult bad(@Arg("reason") Float reason) {
            return Results.success(String.valueOf(reason));
        }
    }

    static final class OrderedCommands {
        @Command("zeta")
        CommandResult zeta() {
            return Results.success("zeta");
        }

        @Command("alpha")
        CommandResult alpha() {
            return Results.success("alpha");
        }
    }

    static final class MetadataCommands {
        @Command("reload")
        @Description("Reload configuration")
        @Permission("admin.reload")
        CommandResult reload() {
            return Results.success("reloaded");
        }
    }

    static final class BlankDescriptionCommands {
        @Command("bad-description")
        @Description(" ")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class BlankPermissionCommands {
        @Command("bad-permission")
        @Permission(" ")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class InventoryCommands {
        @Route("inventory give <target:String> <item:String> [--amount:Integer|-a] [--silent|-s]")
        @Description("Give an item")
        CommandResult give(@RouteCtx CommandContext route) {
            return Results.success(
                route.arg("target", String.class)
                    + " gets "
                    + route.option("amount", Integer.class).orElse(1)
                    + " "
                    + route.arg("item", String.class)
                    + " silently="
                    + route.flag("silent")
            );
        }
    }

    static final class RouteAliasCommands {
        @Route("ban <target:String> [--duration:Integer|-d] [--silent|-s]")
        @Alias("block")
        CommandResult ban(@RouteCtx CommandContext route) {
            return Results.success(
                route.arg("target", String.class)
                    + ":"
                    + route.option("duration", Integer.class).orElse(null)
                    + ":"
                    + route.flag("silent")
            );
        }
    }

    static final class MixedRouteCommands {
        @Command("mixed")
        @Route("mixed <target:String>")
        CommandResult mixed(@Arg("target") String target) {
            return Results.success(target);
        }
    }

    static final class MessagingCommands {
        @Command("msg")
        @Alias("message")
        CommandResult msg(
            @Arg("target") String target,
            @Arg("message") @OptionalArg @Greedy @Default("No message") String message
        ) {
            return Results.success(target + ":" + message);
        }
    }

    @Command("server")
    static final class ServerCommands {
        @Subcommand("reload")
        CommandResult reload() {
            return Results.success("reloaded");
        }
    }

    @Command("team")
    @Alias("t")
    static final class TeamCommands {
        @Subcommand("member")
        @Alias("m")
        static final class MemberCommands {
            @Subcommand("permission")
            @Alias("perm")
            static final class PermissionCommands {
                @Subcommand("grant")
                @Alias("g")
                CommandResult grant(@Arg("target") String target, @Arg("permission") String permission) {
                    return Results.success("grant " + target + " " + permission);
                }

                @SubRoute("set <target:String> <permission:String> [--temporary|-t]")
                @Alias("put")
                CommandResult set(@RouteCtx CommandContext route) {
                    return Results.success(
                        "set "
                            + route.arg("target", String.class)
                            + " "
                            + route.arg("permission", String.class)
                            + " temporary="
                            + route.flag("temporary")
                    );
                }
            }
        }
    }

    @Command("outer")
    static final class NonStaticNestedCommands {
        @Subcommand("inner")
        final class InnerCommands {
            @Subcommand("leaf")
            CommandResult leaf() {
                return Results.success("inner");
            }
        }
    }

    @Command("broken")
    static final class BrokenNestedCommands {
        @Subcommand("inner")
        static final class InnerCommands {
            private InnerCommands(String ignored) {
            }

            @Subcommand("leaf")
            CommandResult leaf() {
                return Results.silent();
            }
        }
    }

    @Command("user")
    static final class UserCommands {
        @SubRoute("rank set <target:String> <rank:String>")
        CommandResult setRank(@RouteCtx CommandContext route) {
            return Results.success(route.arg("target", String.class) + "=" + route.arg("rank", String.class));
        }
    }

    @Command("user")
    @Alias("u")
    static final class AliasedUserCommands {
        @SubRoute("rank set <target:String> <rank:String>")
        @Alias({"roles put"})
        CommandResult setRank(@RouteCtx CommandContext route) {
            return Results.success(route.arg("target", String.class) + "=" + route.arg("rank", String.class));
        }
    }

    @Command("user")
    static final class DslSubcommandCommands {
        @Subcommand("rank set <target:String>")
        CommandResult rank() {
            return Results.silent();
        }
    }

    static final class DslCommandCommands {
        @Command("ban <target:String>")
        CommandResult ban(@RouteCtx CommandContext route) {
            return Results.success(route.input());
        }
    }

    @CaseInsensitive
    static final class CaseInsensitiveCommands {
        @Route("ban <target:String> [--duration:Integer|-d] [--silent|-s]")
        CommandResult ban(@RouteCtx CommandContext route) {
            return Results.success(
                route.arg("target", String.class)
                    + ":"
                    + route.option("duration", Integer.class).orElse(null)
                    + ":"
                    + route.flag("silent")
            );
        }
    }

    @CommandGroup("moderation")
    static final class FutureMetadataCommands {
        @Command("secret")
        @Hidden
        @Usage("secret <target>")
        @Example({"secret Ada", "secret Bob"})
        @Cooldown(30)
        @Require("perm.a && perm.b")
        CommandResult secret(@Arg("target") @Suggest("players") String target) {
            return Results.success(target);
        }

        List<String> players() {
            return List.of("Ada", "Bob");
        }
    }

    static final class VisibleMetadataCommands {
        @Command("visible")
        @Description("Show player info")
        @Usage("visible <player>")
        @Example({"visible Ada", "visible Bob"})
        @Cooldown(5)
        @Require("perm.visible")
        CommandResult visible(@Arg("target") @Suggest("players") String target) {
            return Results.success(target);
        }

        List<Suggestion> players(ArgumentParseContext context) {
            return List.of(
                new Suggestion("Ada", Optional.of("online"), context.replacementStart(), context.replacementEnd(),
                    context.suggestionType(), 10),
                new Suggestion("Bob", Optional.empty(), context.replacementStart(), context.replacementEnd(),
                    context.suggestionType(), 0)
            );
        }
    }

    @Command("admin")
    @Description("Admin root")
    @Permission("admin.root")
    @Usage("admin")
    @Example("admin help")
    @Cooldown(5)
    @Require("admin.root")
    static final class ClassMetadataCommands {
        @Subcommand("reload")
        @Description("Reload leaf")
        @Permission("admin.reload")
        CommandResult reload() {
            return Results.success("reloaded");
        }
    }

    @Command("secure")
    @Middleware(ClassLevelMiddleware.class)
    static final class MiddlewareAnnotatedCommands {
        @Subcommand("ban")
        @Middleware(MethodLevelMiddleware.class)
        CommandResult ban(@Arg("target") String target) {
            MIDDLEWARE_EVENTS.add("executor:ban");
            return Results.success("banned " + target);
        }

        @Subcommand("status")
        CommandResult status() {
            MIDDLEWARE_EVENTS.add("executor:status");
            return Results.success("ok");
        }
    }

    @Middleware(ClassLevelMiddleware.class)
    static final class RouteMiddlewareCommands {
        @Route("audit <target:String>")
        CommandResult audit(@RouteCtx CommandContext route) {
            MIDDLEWARE_EVENTS.add("executor:route");
            return Results.success("audit " + route.arg("target", String.class));
        }
    }

    static final class BrokenMiddlewareCommands {
        @Command("bad")
        @Middleware(BrokenConstructorMiddleware.class)
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class ThrowingMiddlewareCommands {
        @Command("bad")
        @Middleware(ThrowingConstructorMiddleware.class)
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class ClassLevelMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            MIDDLEWARE_EVENTS.add("class:" + String.join("/", commandPath));
            CommandResult result = next.proceed(context);
            MIDDLEWARE_EVENTS.add("class-after");
            return result;
        }
    }

    static final class MethodLevelMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            MIDDLEWARE_EVENTS.add("method:" + String.join("/", commandPath));
            CommandResult result = next.proceed(context);
            MIDDLEWARE_EVENTS.add("method-after");
            return result;
        }
    }

    static final class BrokenConstructorMiddleware implements CommandMiddleware {
        BrokenConstructorMiddleware(String ignored) {
        }

        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            return next.proceed(context);
        }
    }

    static final class ThrowingConstructorMiddleware implements CommandMiddleware {
        ThrowingConstructorMiddleware() {
            throw new IllegalStateException("boom");
        }

        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            return next.proceed(context);
        }
    }

    static final class RequireOnlyCommands {
        @Command("secure")
        @Require("perm.secure")
        CommandResult secure() {
            return Results.success("secure");
        }
    }

    static final class AmbiguousSuggestCommands {
        @Command("amb")
        CommandResult amb(@Arg("target") @Suggest("players") String target) {
            return Results.success(target);
        }

        List<String> players() {
            return List.of("Ada");
        }

        List<String> players(ArgumentParseContext context) {
            return List.of(context.rawToken());
        }
    }

    static final class NullSuggestionCommands {
        @Command("nulls")
        CommandResult nulls(@Arg("target") @Suggest("players") String target) {
            return Results.success(target);
        }

        List<String> players() {
            return Arrays.asList("Ada", null);
        }
    }

    static final class MixedSuggestionCommands {
        @Command("mixed")
        CommandResult mixed(@Arg("target") @Suggest("players") String target) {
            return Results.success(target);
        }

        List<Object> players() {
            return List.of("Ada", new Suggestion("Bob", Optional.empty(), 0, 0, SuggestionType.ARGUMENT, 0));
        }
    }

    static final class MissingRouteArgumentCommands {
        @Route("ban <target:String>")
        CommandResult ban() {
            return Results.silent();
        }
    }

    static final class MissingAnnotatedArgumentCommands {
        @Route("ban <target:String>")
        CommandResult ban(@Arg("missing") String target) {
            return Results.success(target);
        }
    }

    static final class MultipleRouteCtxCommands {
        @Route("ban <target:String>")
        CommandResult ban(@RouteCtx CommandContext first, @RouteCtx CommandContext second) {
            return Results.success(first.input() + second.input());
        }
    }

    static final class InlineEnumRouteCommands {
        @Route("choose <mode:enum(a,b)>")
        CommandResult choose(@RouteCtx CommandContext route) {
            return Results.success(route.input());
        }
    }

    static final class ConstrainedRouteCommands {
        @Route("ban <amount:Integer{1..5}>")
        CommandResult ban(@RouteCtx CommandContext route) {
            return Results.success(route.input());
        }
    }

    static final class InferredParameterCommands {
        @Command("ban")
        CommandResult ban(String target, @Greedy String reason) {
            return Results.success(target + ":" + reason);
        }
    }

    private static final class UnsupportedMetadataRegistry implements CommandRegistry {
        @Override
        public void command(String literal, Consumer<CommandBuilder> configure) {
            configure.accept(new UnsupportedMetadataBuilder());
        }

        @Override
        public void register(CommandNode node) {
        }

        @Override
        public RouteBuilder route(String pattern) {
            throw new UnsupportedOperationException("routes are not supported");
        }
    }

    private static final class UnsupportedMetadataBuilder implements CommandRegistry.CommandBuilder {
        @Override
        public CommandRegistry.CommandBuilder description(String description) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder permission(String permission) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder alias(String alias) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder aliases(String... aliases) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder subcommand(
            String literal,
            Consumer<CommandRegistry.CommandBuilder> configure
        ) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder argument(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder optionalArgument(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder greedyArgument(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder optionalGreedyArgument(String name, Class<T> type) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder flag(String name) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder flag(String name, String alias) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type, String alias) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder executes(CommandRegistry.CommandExecutor executor) {
            return this;
        }
    }
}
