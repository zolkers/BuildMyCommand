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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownMiddleware implements CommandMiddleware {
    private final Clock clock;
    private final Map<Key, Instant> expiresAt;

    public CooldownMiddleware(Clock clock) {
        this(clock, new ConcurrentHashMap<>());
    }

    public CooldownMiddleware(Clock clock, Map<Key, Instant> expiresAt) {
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
        Key key = new Key(sourceKey(context.source()), String.join(" ", commandPath));
        Instant expiry = expiresAt.get(key);
        if (expiry != null && expiry.isAfter(now)) {
            return Results.failure("Command is on cooldown for " + Duration.between(now, expiry));
        }

        CommandResult result = next.proceed(context);
        if (result.status() != CommandResult.Status.FAILURE) {
            expiresAt.put(key, now.plus(cooldown));
        }
        return result;
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
