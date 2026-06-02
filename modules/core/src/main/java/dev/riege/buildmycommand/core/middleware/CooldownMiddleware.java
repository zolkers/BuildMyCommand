package dev.riege.buildmycommand.core.middleware;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class CooldownMiddleware implements CommandMiddleware {
    private final Clock clock;
    private final ConcurrentMap<Key, Instant> expiresAt;

    public CooldownMiddleware(Clock clock) {
        this(clock, new ConcurrentHashMap<>());
    }

    public CooldownMiddleware(Clock clock, ConcurrentMap<Key, Instant> expiresAt) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @Override
    public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(commandPath, "commandPath");
        Objects.requireNonNull(next, "next");

        Duration cooldown = command.metadata().cooldown().orElse(null);
        if (cooldown == null) {
            return next.proceed(context);
        }

        Instant now = clock.instant();
        cleanupExpired(now);
        Key key = new Key(sourceKey(context.source()), String.join(" ", commandPath));
        Instant reservedUntil = now.plus(cooldown);
        AtomicReference<Duration> remaining = new AtomicReference<>();
        expiresAt.compute(key, (ignored, expiry) -> {
            if (expiry != null && expiry.isAfter(now)) {
                remaining.set(Duration.between(now, expiry));
                return expiry;
            }
            return reservedUntil;
        });
        if (remaining.get() != null) {
            return Results.failure("Command is on cooldown for " + remaining.get());
        }

        try {
            CommandResult result = next.proceed(context);
            if (result.status() == CommandResult.Status.FAILURE) {
                expiresAt.remove(key, reservedUntil);
            }
            return result;
        } catch (RuntimeException exception) {
            expiresAt.remove(key, reservedUntil);
            throw exception;
        } catch (Error error) {
            expiresAt.remove(key, reservedUntil);
            throw error;
        }
    }

    private void cleanupExpired(Instant now) {
        expiresAt.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private static String sourceKey(CommandSource source) {
        return source.id()
            .map(id -> "id:" + id)
            .or(() -> source.name().map(name -> "name:" + name))
            .orElseGet(() -> "identity:" + source.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(source)));
    }

    public record Key(String source, String commandPath) {
        public Key {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(commandPath, "commandPath");
        }
    }
}
