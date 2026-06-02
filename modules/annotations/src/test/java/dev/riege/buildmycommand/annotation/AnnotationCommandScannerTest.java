package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.CommandContext;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
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
    void registersAnnotatedRouteCommandAliasAndParameterAliases() {
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

        assertEquals("annotated command method cannot use both @Command and @Route: mixed", exception.getMessage());
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

        AnnotationCommandScanner.register(framework.registry(), new UserCommands());

        CommandResult result = framework.dispatch(source(), "user rank set Ada admin");

        assertEquals(Optional.of("Ada=admin"), result.reply());
    }

    @Test
    void registersClassCommandAndSubcommandAliases() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new AliasedUserCommands());

        CommandResult result = framework.dispatch(source(), "u roles put Ada admin");

        assertEquals(Optional.of("Ada=admin"), result.reply());
        assertEquals("Usage: user rank set <target:String> <rank:String>", framework.help("u roles put"));
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
        ), framework.suggestRich(dev.riege.buildmycommand.api.CommandInput.raw(permittedSource("perm.visible"), "visible ")));
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
            () -> framework.suggestRich(dev.riege.buildmycommand.api.CommandInput.raw(source(), "nulls ")));

        assertEquals("suggestion provider players returned null at index 1", exception.getMessage());
    }

    @Test
    void rejectsMixedSuggestionProviderElementTypesWithIndex() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new MixedSuggestionCommands());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> framework.suggestRich(dev.riege.buildmycommand.api.CommandInput.raw(source(), "mixed ")));

        assertEquals("suggestion provider players returned mixed element at index 1", exception.getMessage());
    }

    @Test
    void rejectsRouteArgumentWithoutMatchingMethodParameterBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MissingRouteArgumentCommands()));

        assertEquals("route argument target has no matching method parameter on ban", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsAnnotatedArgumentMissingFromRouteDslBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MissingAnnotatedArgumentCommands()));

        assertEquals("@Arg(\"missing\") does not exist in route DSL for ban", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsAnnotatedOptionMissingFromRouteDslBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new MissingAnnotatedOptionCommands()));

        assertEquals("@Option(\"duration\") does not exist in route DSL for ban", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsRouteArgumentTypeMismatchBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new RouteArgumentTypeMismatchCommands()));

        assertEquals("route argument amount expects Integer but method parameter is String on amount",
            exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsRouteGreedyArgumentBoundWithoutGreedyAnnotationBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new RouteGreedyMismatchCommands()));

        assertEquals("route argument reason is greedy but method parameter is not @Greedy", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsRouteFlagBoundAsValueOptionBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new RouteFlagBoundAsOptionCommands()));

        assertEquals("@Option(\"silent\") does not exist in route DSL for ban", exception.getMessage());
        assertEquals("", framework.schema());
    }

    @Test
    void rejectsRouteValueOptionBoundAsFlagBeforeRegistryMutation() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new RouteOptionBoundAsFlagCommands()));

        assertEquals("@Flag(\"duration\") does not exist in route DSL for ban", exception.getMessage());
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
        CommandResult give(
            @Arg("target") String target,
            @Arg("item") String item,
            @Option("amount") Integer amount,
            @Flag("silent") boolean silent
        ) {
            return Results.success(target + " gets " + amount + " " + item + " silently=" + silent);
        }
    }

    static final class RouteAliasCommands {
        @Route("ban <target:String> [--duration:Integer] [--silent]")
        @Alias("block")
        CommandResult ban(
            @Arg("target") String target,
            @Option("duration") @Alias("d") Integer duration,
            @Flag("silent") @Alias("s") boolean silent
        ) {
            return Results.success(target + ":" + duration + ":" + silent);
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

    @Command("user")
    static final class UserCommands {
        @Subcommand("rank set <target:String> <rank:String>")
        CommandResult setRank(@Arg("target") String target, @Arg("rank") String rank) {
            return Results.success(target + "=" + rank);
        }
    }

    @Command("user")
    @Alias("u")
    static final class AliasedUserCommands {
        @Subcommand("rank set <target:String> <rank:String>")
        @Alias({"roles put"})
        CommandResult setRank(@Arg("target") String target, @Arg("rank") String rank) {
            return Results.success(target + "=" + rank);
        }
    }

    @CaseInsensitive
    static final class CaseInsensitiveCommands {
        @Route("ban <target:String> [--duration:Integer|-d] [--silent|-s]")
        CommandResult ban(
            @Arg("target") String target,
            @Option("duration") Integer duration,
            @Flag("silent") boolean silent
        ) {
            return Results.success(target + ":" + duration + ":" + silent);
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

        List<Suggestion> players(dev.riege.buildmycommand.api.ArgumentParseContext context) {
            return List.of(
                new Suggestion("Ada", Optional.of("online"), context.replacementStart(), context.replacementEnd(),
                    context.suggestionType(), 10),
                new Suggestion("Bob", Optional.empty(), context.replacementStart(), context.replacementEnd(),
                    context.suggestionType(), 0)
            );
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

        List<String> players(dev.riege.buildmycommand.api.ArgumentParseContext context) {
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

    static final class MissingAnnotatedOptionCommands {
        @Route("ban <target:String> [--silent]")
        CommandResult ban(@Arg("target") String target, @Option("duration") Integer duration) {
            return Results.success(target + duration);
        }
    }

    static final class RouteArgumentTypeMismatchCommands {
        @Route("ban <amount:Integer>")
        CommandResult ban(@Arg("amount") String amount) {
            return Results.success(amount);
        }
    }

    static final class RouteGreedyMismatchCommands {
        @Route("ban [reason:String...]")
        CommandResult ban(@Arg("reason") @OptionalArg String reason) {
            return Results.success(reason);
        }
    }

    static final class RouteFlagBoundAsOptionCommands {
        @Route("ban [--silent]")
        CommandResult ban(@Option("silent") Boolean silent) {
            return Results.success(String.valueOf(silent));
        }
    }

    static final class RouteOptionBoundAsFlagCommands {
        @Route("ban [--duration:Integer]")
        CommandResult ban(@Flag("duration") boolean duration) {
            return Results.success(String.valueOf(duration));
        }
    }

    static final class InlineEnumRouteCommands {
        @Route("choose <mode:enum(a,b)>")
        CommandResult choose(@Arg("mode") String mode) {
            return Results.success(mode);
        }
    }

    static final class ConstrainedRouteCommands {
        @Route("ban <amount:Integer{1..5}>")
        CommandResult ban(@Arg("amount") Integer amount) {
            return Results.success(String.valueOf(amount));
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
