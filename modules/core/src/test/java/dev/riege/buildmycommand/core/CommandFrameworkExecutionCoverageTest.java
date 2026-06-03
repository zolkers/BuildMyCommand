package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.middleware.CooldownMiddleware;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandFrameworkExecutionCoverageTest {
    @Test
    void frameworkExposesCasePoliciesAndHelpShortcut() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        framework.registry().route("Root|r [--silent|-s]").executes(ctx -> Results.success("ok"));

        assertTrue(framework.caseInsensitiveLiterals());
        assertTrue(framework.caseInsensitiveOptions());
        assertEquals("Usage: Root [--silent|-s]", framework.help("root"));
        assertEquals(CommandResult.Status.SUCCESS, framework.dispatch(source(Set.of()), "ROOT --Silent").status());
    }

    @Test
    void dispatcherCoversEmptyInputPrefixFailuresAndPrefixConsumedPathArguments() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("perm view")
            .permission("perm.view")
            .executes(ctx -> Results.success("view"));
        framework.registry().command("user", command -> command
            .argument("id", Integer.class)
            .subcommand("info", info -> info.executes(ctx -> Results.success("user " + ctx.arg("id", Integer.class)))));
        assertEquals(Optional.of("Unknown command: "), framework.dispatch(source(Set.of()), "").reply());
        assertEquals(Optional.of("Missing permission: perm.view"), framework.dispatch(source(Set.of()), "perm view").reply());
        assertEquals(Optional.of("Invalid integer for argument id: bad"), framework.dispatch(source(Set.of()), "user bad info").reply());
        assertEquals(Optional.of("user 5"), framework.dispatch(source(Set.of()), "user 5 info").reply());
    }

    @Test
    void defaultErrorHandlerRethrowsRuntimeAndErrorWhileCustomHandlerCanReturnFailures() throws Exception {
        CommandFramework runtime = CommandFramework.create();
        runtime.registry().route("boom").executes(ctx -> {
            throw new IllegalStateException("boom");
        });
        assertThrows(IllegalStateException.class, () -> runtime.dispatch(source(Set.of()), "boom"));

        CommandFramework fatal = CommandFramework.create();
        fatal.registry().route("fatal").cooldown(Duration.ofSeconds(1)).executes(ctx -> {
            throw new AssertionError("fatal");
        });
        assertThrows(AssertionError.class, () -> fatal.dispatch(source(Set.of()), "fatal"));

        CommandFramework handled = CommandFramework.builder()
            .errorHandler((context, command, path, error) -> Results.failure("handled " + path))
            .build();
        handled.registry().route("handled").executes(ctx -> {
            throw new IllegalArgumentException("handled");
        });

        assertEquals(Optional.of("handled [handled]"), handled.dispatch(source(Set.of()), "handled").reply());

        Method rethrow = CommandFramework.Builder.class.getDeclaredMethod(
            "rethrow",
            CommandContext.class,
            CommandNode.class,
            List.class,
            Throwable.class
        );
        rethrow.setAccessible(true);
        InvocationTargetException wrapped = assertThrows(InvocationTargetException.class, () ->
            rethrow.invoke(null, null, null, List.of(), new Exception("checked")));
        assertTrue(wrapped.getCause() instanceof RuntimeException);
        assertEquals("checked", wrapped.getCause().getCause().getMessage());
        InvocationTargetException fatalWrapped = assertThrows(InvocationTargetException.class, () ->
            rethrow.invoke(null, null, null, List.of(), new AssertionError("checked fatal")));
        assertTrue(fatalWrapped.getCause() instanceof AssertionError);
    }

    @Test
    void cooldownMiddlewareCoversKeysRollbackRemainingAndSourceIdentityVariants() {
        ConcurrentHashMap<CooldownMiddleware.Key, Instant> store = new ConcurrentHashMap<>();
        Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneOffset.UTC);
        CommandFramework framework = CommandFramework.builder()
            .cooldownClock(clock)
            .cooldownStore(store)
            .build();
        framework.registry().route("limited")
            .cooldown(Duration.ofSeconds(5))
            .executes(ctx -> Results.success("ok"));
        framework.registry().route("fail")
            .cooldown(Duration.ofSeconds(5))
            .executes(ctx -> Results.failure("no"));
        framework.registry().route("throwing")
            .cooldown(Duration.ofSeconds(5))
            .executes(ctx -> {
                throw new IllegalStateException("rollback");
            });

        assertEquals(Optional.of("ok"), framework.dispatch(sourceWithId("1"), "limited").reply());
        assertEquals(Optional.of("Command is on cooldown for PT5S"), framework.dispatch(sourceWithId("1"), "limited").reply());
        assertEquals(Optional.of("no"), framework.dispatch(sourceWithName("Ada"), "fail").reply());
        assertFalse(store.keySet().stream().anyMatch(key -> key.commandPath().equals("fail")));
        assertThrows(IllegalStateException.class, () -> framework.dispatch(new CommandSource() {
        }, "throwing"));
        assertFalse(store.keySet().stream().anyMatch(key -> key.commandPath().equals("throwing")));

        CooldownMiddleware middleware = new CooldownMiddleware(clock);
        CommandNode noCooldown = Commands.literal("plain").handler(ctx -> Results.success("ok")).build();
        assertEquals(Optional.of("ok"), middleware.execute(
            new CommandContext(source(Set.of()), CommandInput.raw(source(Set.of()), "plain"), java.util.Map.of()),
            noCooldown,
            List.of("plain"),
            ctx -> Results.success("ok")
        ).reply());
        NoCleanupCooldownStore noCleanupStore = new NoCleanupCooldownStore();
        CooldownMiddleware.Key expiredKey = new CooldownMiddleware.Key("id:expired", "limited");
        noCleanupStore.put(expiredKey, Instant.parse("2026-06-03T09:59:59Z"));
        CommandFramework expiredFramework = CommandFramework.builder()
            .cooldownClock(clock)
            .cooldownStore(noCleanupStore)
            .build();
        expiredFramework.registry().route("limited")
            .cooldown(Duration.ofSeconds(5))
            .executes(ctx -> Results.success("renewed"));
        assertEquals(Optional.of("renewed"), expiredFramework.dispatch(sourceWithId("expired"), "limited").reply());
        assertThrows(NullPointerException.class, () -> new CooldownMiddleware.Key(null, "path"));
        assertThrows(NullPointerException.class, () -> new CooldownMiddleware.Key("source", null));
    }

    private static final class NoCleanupCooldownStore extends ConcurrentHashMap<CooldownMiddleware.Key, Instant> {
        @Override
        public java.util.Set<java.util.Map.Entry<CooldownMiddleware.Key, Instant>> entrySet() {
            return new java.util.HashSet<>();
        }
    }

    private static CommandSource source(Set<String> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
    }

    private static CommandSource sourceWithId(String id) {
        return new CommandSource() {
            @Override
            public Optional<String> id() {
                return Optional.of(id);
            }
        };
    }

    private static CommandSource sourceWithName(String name) {
        return new CommandSource() {
            @Override
            public Optional<String> name() {
                return Optional.of(name);
            }
        };
    }
}
