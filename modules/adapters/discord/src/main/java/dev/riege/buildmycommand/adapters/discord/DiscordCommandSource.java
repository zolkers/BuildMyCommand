package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class DiscordCommandSource implements CommandSource {
    private final DiscordUser user;
    private final List<CommandMessage> replies = new ArrayList<>();

    public DiscordCommandSource(DiscordUser user) {
        this.user = Objects.requireNonNull(user, "user");
    }

    public DiscordUser user() {
        return user;
    }

    public List<CommandMessage> replies() {
        return List.copyOf(replies);
    }

    @Override
    public Optional<String> id() {
        return Optional.of(user.id());
    }

    @Override
    public Optional<String> name() {
        return Optional.of(user.username());
    }

    @Override
    public Locale locale() {
        return user.locale();
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(user.nativeHandle())) {
            return Optional.of(type.cast(user.nativeHandle()));
        }
        if (type.isInstance(user)) {
            return Optional.of(type.cast(user));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Object> metadata(String key) {
        Objects.requireNonNull(key, "key");
        if ("discord.userId".equals(key)) {
            return Optional.of(user.id());
        }
        if ("discord.username".equals(key)) {
            return Optional.of(user.username());
        }
        return Optional.empty();
    }

    @Override
    public void reply(CommandMessage message) {
        replies.add(Objects.requireNonNull(message, "message"));
    }

    @Override
    public boolean hasPermission(String permission) {
        Objects.requireNonNull(permission, "permission");
        return user.permissions().contains(permission);
    }
}
