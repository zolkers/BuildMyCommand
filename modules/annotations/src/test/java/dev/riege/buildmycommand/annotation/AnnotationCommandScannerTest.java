package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static CommandSource source() {
        return new CommandSource() {
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
}
