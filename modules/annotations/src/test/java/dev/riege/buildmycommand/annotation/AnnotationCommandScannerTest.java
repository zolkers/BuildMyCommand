package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.annotation.binding.AnnotationCommandCompiler;
import dev.riege.buildmycommand.annotation.binding.MethodCommandBinder;
import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
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
    private static final List<String> MIDDLEWARE_EVENTS = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Test
    void routeDslIsCanonicalAnnotationApi() {
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
    void commonRouteRootsMergeIntoOneCommandTree() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new WeccCommands());

        assertEquals(Optional.of("pong"), framework.dispatch(source(), "wecc ping").reply());
        assertEquals(Optional.of("bang Ada"), framework.dispatch(source(), "wecc b Ada").reply());
        assertEquals("""
            command wecc
              child bang
              child ping
            command wecc bang
              argument target:String required
            command wecc ping""", framework.schema());
    }

    @Test
    void classCommandSubRoutesAndNestedGroupsCompose() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new TeamCommands());

        assertEquals(Optional.of("grant Ada build.fly"),
            framework.dispatch(source(), "t m perm g Ada build.fly").reply());
        assertEquals(Optional.of("set Ada admin temporary=true"),
            framework.dispatch(source(), "team member permission put Ada admin -t").reply());
        assertEquals("Usage: team member permission grant <target:String> <permission:String>",
            framework.help("team member permission grant"));
    }

    @Test
    void metadataPermissionRequirementSuggestionsAndAliasPolicyApplyToRouteDsl() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new MetadataCommands());

        assertEquals(List.of(), framework.suggest(source(), "s", 1));
        assertEquals("Missing permission: perm.a && perm.b",
            framework.dispatch(permittedSource(), "s Ada").reply().orElseThrow());
        assertEquals(Optional.of("Ada"),
            framework.dispatch(permittedSource("perm.a", "perm.b"), "secret Ada").reply());
        assertEquals(List.of(), framework.suggestRich(CommandInput.raw(permittedSource("perm.a", "perm.b"), "secret ")));
        assertEquals("""
            command secret
              hidden true
              group moderation
              usage secret <target>
              example secret Ada
              example secret Bob
              cooldown PT30S
              require perm.a && perm.b
              argument target:String required suggest=target""", framework.schema());
    }

    @Test
    void middlewareRunsAroundRouteDslCommands() {
        MIDDLEWARE_EVENTS.clear();
        CommandFramework framework = CommandFramework.builder()
            .middleware((context, command, path, next) -> {
                MIDDLEWARE_EVENTS.add("global:" + String.join("/", path));
                return next.proceed(context);
            })
            .build();

        AnnotationCommandScanner.register(framework.registry(), new MiddlewareCommands());

        assertEquals(Optional.of("warned Ada"),
            framework.dispatch(permittedSource("staff"), "moderation warn Ada").reply());
        assertEquals(List.of(
            "global:moderation/warn",
            "class:moderation/warn",
            "method:moderation/warn",
            "executor:warn",
            "method-after",
            "class-after"
        ), MIDDLEWARE_EVENTS);
    }

    @Test
    void classRootMetadataAppliesWithoutOverwritingSubRouteLeaves() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new AdminCommands());

        assertEquals("""
            Usage: admin
            Description: Admin root
            Example: admin help""", framework.help(permittedSource("admin.root"), "admin"));
        assertEquals(Optional.of("reloaded"),
            framework.dispatch(permittedSource("admin.root", "admin.reload"), "admin reload").reply());
        assertEquals("Missing permission: admin.root",
            framework.dispatch(permittedSource("admin.reload"), "admin reload").reply().orElseThrow());
    }

    @Test
    void commandAndSubcommandStillSupportZeroArgSimpleLeaves() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new ServerCommands());

        assertEquals(Optional.of("status"), framework.dispatch(source(), "server status").reply());
        assertEquals(Optional.of("reloaded"), framework.dispatch(source(), "server reload").reply());
    }

    @Test
    void rejectsRouteMethodsWithoutExactlyOneRouteContext() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MissingRouteCtxCommands()));
        IllegalArgumentException multiple = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MultipleRouteCtxCommands()));

        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: bad",
            missing.getMessage());
        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: bad",
            multiple.getMessage());
    }

    @Test
    void rejectsUnsupportedNonContextParameters() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new UnsupportedParameterCommands()));

        assertEquals("annotated command methods only support CommandContext or @RouteCtx CommandContext parameters: target",
            exception.getMessage());
    }

    @Test
    void rejectsMixedRouteAnnotationsAndDslInLiteralAnnotations() {
        IllegalArgumentException mixed = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new MixedRouteCommands()));
        IllegalArgumentException commandDsl = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new DslCommandCommands()));
        IllegalArgumentException subcommandDsl = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new DslSubcommandCommands()));

        assertEquals("annotated command method cannot mix command route annotations: mixed", mixed.getMessage());
        assertEquals("@Command only accepts one literal; use @Route for route DSL: ban <target:String>",
            commandDsl.getMessage());
        assertEquals("@Subcommand only accepts one literal; use @SubRoute for route DSL: rank set <target:String>",
            subcommandDsl.getMessage());
    }

    @Test
    void rejectsAnalysisOnlyRouteTypes() {
        IllegalArgumentException inlineEnum = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new InlineEnumRouteCommands()));
        IllegalArgumentException constrained = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new ConstrainedRouteCommands()));

        assertEquals("route argument mode uses analysis-only type enum(a,b)", inlineEnum.getMessage());
        assertEquals("route argument amount uses analysis-only type Integer{1..5}", constrained.getMessage());
    }

    @Test
    void compilerExposesMetadataWithoutMutatingRegistry() throws Exception {
        AnnotationCommandCompiler.CompiledCommands compiled =
            AnnotationCommandCompiler.compile(new MetadataCommands());
        AnnotationCommandCompiler.CompiledCommand command = compiled.commands().get(0);
        MethodCommandBinder.CommandMetadata metadata = command.metadata();

        assertEquals("secret|s <target:String>", command.route());
        assertEquals(Optional.of("moderation"), command.group());
        assertEquals(true, metadata.hidden());
        assertEquals(Optional.of("secret <target>"), metadata.usage());
        assertIterableEquals(List.of("secret Ada", "secret Bob"), metadata.examples());
        assertEquals(Optional.of(Duration.ofSeconds(30)), metadata.cooldown());
        assertEquals(Optional.of("perm.a && perm.b"), metadata.requirement());
        assertEquals(false, metadata.suggestAliases());
        assertEquals(List.of("target"), metadata.suggestions().stream().map(MethodCommandBinder.SuggestionBinding::name).toList());
        assertEquals(1, command.bindings().size());
        Method signature = AnnotationCommandCompiler.class.getDeclaredMethod("signature", Method.class);
        signature.setAccessible(true);
        assertEquals("overloaded(dev.riege.buildmycommand.api.CommandContext,java.lang.String)",
            signature.invoke(null, OverloadedRouteCommands.class.getDeclaredMethod(
                "overloaded",
                CommandContext.class,
                String.class
            )));
    }

    @Test
    void metadataAnnotationsDeclareConsistentTargets() {
        assertTargets(Route.class, ElementType.METHOD);
        assertTargets(SubRoute.class, ElementType.METHOD);
        assertTargets(RouteCtx.class, ElementType.PARAMETER);
        assertTargets(Suggest.class, ElementType.METHOD);
        assertTargets(Subcommand.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Description.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Permission.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Usage.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Example.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Cooldown.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Hidden.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Require.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(Middleware.class, ElementType.METHOD, ElementType.TYPE);
        assertTargets(SuggestAliases.class, ElementType.METHOD, ElementType.TYPE);
    }

    @Test
    void registryDefaultsRejectRouteMetadataInsteadOfIgnoringIt() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
            () -> AnnotationCommandScanner.register(new UnsupportedMetadataRegistry(), new MetadataCommands()));

        assertEquals("routes are not supported", exception.getMessage());
    }

    @Test
    void simpleCommandAndNestedSubcommandEdgesStillWork() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new EdgeCommands());
        AnnotationCommandScanner.register(framework.registry(), new EdgeCommandsRoot());
        AnnotationCommandScanner.register(framework.registry(), new NoAliasPrefixCommands());

        assertEquals(Optional.of("plain"), framework.dispatch(source(), "plain").reply());
        assertEquals(Optional.of("plain"), framework.dispatch(source(), "p").reply());
        assertEquals(Optional.of("simple"), framework.dispatch(source(), "simple").reply());
        assertEquals(Optional.of("hidden"), framework.dispatch(source(), "edge hidden").reply());
        assertEquals(Optional.of("deep"), framework.dispatch(source(), "edge branch leaf go").reply());
        assertEquals(Optional.of("inner"), framework.dispatch(source(), "edge inner run").reply());
        assertEquals(Optional.of("noalias"), framework.dispatch(source(), "noalias branch run").reply());
        assertEquals(List.of("alpha", "beta"), framework.suggest(source(), "choose ", 7));
        assertEquals(List.of("fast", "safe"), framework.suggest(source(), "choose --mode ", 14));
    }

    @Test
    void rejectsInvalidAliasesAndClassMetadata() {
        assertEquals("route alias must not be blank", assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new BlankAliasCommands()))
            .getMessage());
        assertEquals("route alias is longer than route: a b c", assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new LongAliasCommands()))
            .getMessage());
        assertEquals("route alias can only target literal tokens: root target", assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new ArgumentAliasCommands()))
            .getMessage());
        assertEquals("cooldown must be positive", assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new BadClassCooldownCommands()))
            .getMessage());
        assertEquals("command group must not be blank", assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new BadCommandGroupCommands()))
            .getMessage());
        assertEquals("@Suggest provider does not match a route argument or option: missing",
            assertThrows(IllegalArgumentException.class,
                () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new UnknownSuggestCommands()))
                .getMessage());
        assertEquals("cannot instantiate nested @Subcommand group: "
                + BadNestedGroupCommands.BadGroup.class.getName(),
            assertThrows(IllegalArgumentException.class,
                () -> AnnotationCommandScanner.register(CommandFramework.create().registry(), new BadNestedGroupCommands()))
                .getMessage());
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

    static final class WeccCommands {
        @Route("wecc ping")
        CommandResult ping(@RouteCtx CommandContext route) {
            return Results.success("pong");
        }

        @Route("wecc bang|b <target:String>")
        @SuggestAliases(false)
        CommandResult bang(@RouteCtx CommandContext route) {
            return Results.success("bang " + route.arg("target", String.class));
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
                @SubRoute("grant <target:String> <permission:String>")
                @Alias("g")
                CommandResult grant(@RouteCtx CommandContext route) {
                    return Results.success("grant " + route.arg("target", String.class)
                        + " " + route.arg("permission", String.class));
                }

                @SubRoute("set <target:String> <permission:String> [--temporary|-t]")
                @Alias("put")
                CommandResult set(@RouteCtx CommandContext route) {
                    return Results.success("set " + route.arg("target", String.class)
                        + " " + route.arg("permission", String.class)
                        + " temporary=" + route.flag("temporary"));
                }
            }
        }
    }

    @CommandGroup("moderation")
    static final class MetadataCommands {
        @Route("secret <target:String>")
        @Alias("s")
        @Hidden
        @Usage("secret <target>")
        @Example({"secret Ada", "secret Bob"})
        @Cooldown(30)
        @Require("perm.a && perm.b")
        @SuggestAliases(false)
        CommandResult secret(@RouteCtx CommandContext route) {
            return Results.success(route.arg("target", String.class));
        }

        @Suggest("target")
        List<Suggestion> players(ArgumentParseContext context) {
            return List.of(
                new Suggestion("Ada", Optional.of("online"), context.replacementStart(), context.replacementEnd(),
                    context.suggestionType(), 10),
                new Suggestion("Bob", Optional.empty(), context.replacementStart(), context.replacementEnd(),
                    context.suggestionType(), 0)
            );
        }
    }

    @Command("moderation")
    @Middleware(ClassLevelMiddleware.class)
    static final class MiddlewareCommands {
        @SubRoute("warn <target:String>")
        @Middleware(MethodLevelMiddleware.class)
        CommandResult warn(@RouteCtx CommandContext route) {
            MIDDLEWARE_EVENTS.add("executor:warn");
            return Results.success("warned " + route.arg("target", String.class));
        }
    }

    @Command("admin")
    @Description("Admin root")
    @Permission("admin.root")
    @Usage("admin")
    @Example("admin help")
    @Cooldown(5)
    @Require("admin.root")
    static final class AdminCommands {
        @SubRoute("reload")
        @Description("Reload leaf")
        @Permission("admin.reload")
        CommandResult reload(@RouteCtx CommandContext route) {
            return Results.success("reloaded");
        }
    }

    @Command("server")
    static final class ServerCommands {
        @Subcommand("status")
        CommandResult status() {
            return Results.success("status");
        }

        @Subcommand("reload")
        CommandResult reload(CommandContext context) {
            return Results.success("reloaded");
        }
    }

    static final class MissingRouteCtxCommands {
        @Route("bad <target:String>")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class MultipleRouteCtxCommands {
        @Route("bad <target:String>")
        CommandResult bad(@RouteCtx CommandContext first, @RouteCtx CommandContext second) {
            return Results.success(first.input() + second.input());
        }
    }

    static final class UnsupportedParameterCommands {
        @Command("bad")
        CommandResult bad(String target) {
            return Results.success(target);
        }
    }

    static final class MixedRouteCommands {
        @Command("mixed")
        @Route("mixed <target:String>")
        CommandResult mixed(@RouteCtx CommandContext route) {
            return Results.success(route.input());
        }
    }

    static final class DslCommandCommands {
        @Command("ban <target:String>")
        CommandResult ban(CommandContext context) {
            return Results.success(context.input());
        }
    }

    @Command("user")
    static final class DslSubcommandCommands {
        @Subcommand("rank set <target:String>")
        CommandResult rank(CommandContext context) {
            return Results.success(context.input());
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

    static final class EdgeCommands {
        @Command("plain")
        @Alias("p")
        @Hidden
        CommandResult plain() {
            return Results.success("plain");
        }

        @Command("simple")
        CommandResult simple() {
            return Results.success("simple");
        }

        @Route("choose <target:String> [--mode:String]")
        @CaseInsensitive(literals = true, options = true)
        CommandResult choose(@RouteCtx CommandContext route) {
            return Results.success(route.input());
        }

        @Suggest("target")
        List<String> targets() {
            return List.of("alpha", "beta");
        }

        @Suggest("mode")
        List<String> modes() {
            return List.of("fast", "safe");
        }
    }

    @Command("edge")
    @Alias("e")
    @Hidden
    @Usage("edge")
    @Example("edge hidden")
    @Cooldown(1)
    @Require("edge.use")
    static final class EdgeRootCommands {
    }

    @Command("edge")
    @Alias("e")
    @Hidden
    @Usage("edge")
    @Example("edge hidden")
    @Cooldown(1)
    @Require("edge.use")
    @SuggestAliases(false)
    static final class EdgeCommandsRoot {
        @Subcommand("hidden")
        @Hidden
        CommandResult hidden() {
            return Results.success("hidden");
        }

        @Subcommand("branch")
        @Hidden
        static final class Branch {
            @Subcommand("leaf")
            @Alias("l")
            static final class Leaf {
                @Subcommand("go")
                @Alias("g")
                CommandResult go() {
                    return Results.success("deep");
                }
            }
        }

        @Subcommand("inner")
        final class Inner {
            @Subcommand("run")
            CommandResult run() {
                return Results.success("inner");
            }
        }
    }

    @Command("noalias")
    static final class NoAliasPrefixCommands {
        @Subcommand("branch")
        static final class Branch {
            @SubRoute("run")
            CommandResult run(@RouteCtx CommandContext route) {
                return Results.success("noalias");
            }
        }
    }

    static final class OverloadedRouteCommands {
        CommandResult overloaded(CommandContext context, String value) {
            return Results.silent();
        }
    }

    static final class BlankAliasCommands {
        @Route("root literal")
        @Alias(" ")
        CommandResult bad(@RouteCtx CommandContext route) {
            return Results.silent();
        }
    }

    static final class LongAliasCommands {
        @Route("root literal")
        @Alias("a b c")
        CommandResult bad(@RouteCtx CommandContext route) {
            return Results.silent();
        }
    }

    static final class ArgumentAliasCommands {
        @Route("root <target:String>")
        @Alias("root target")
        CommandResult bad(@RouteCtx CommandContext route) {
            return Results.silent();
        }
    }

    @Command("bad")
    @Cooldown(0)
    static final class BadClassCooldownCommands {
        @Subcommand("run")
        CommandResult run() {
            return Results.silent();
        }
    }

    @CommandGroup(" ")
    static final class BadCommandGroupCommands {
        @Route("bad group")
        CommandResult run(@RouteCtx CommandContext route) {
            return Results.silent();
        }
    }

    static final class UnknownSuggestCommands {
        @Route("bad suggest <target:String>")
        CommandResult run(@RouteCtx CommandContext route) {
            return Results.silent();
        }

        @Suggest("missing")
        List<String> missing() {
            return List.of("x");
        }
    }

    @Command("badnested")
    static final class BadNestedGroupCommands {
        @Subcommand("broken")
        static final class BadGroup {
            BadGroup(String ignored) {
            }

            @Subcommand("run")
            CommandResult run() {
                return Results.silent();
            }
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
