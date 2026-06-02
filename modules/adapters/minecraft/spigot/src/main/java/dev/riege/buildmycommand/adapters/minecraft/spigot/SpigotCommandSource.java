package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandSource;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class SpigotCommandSource implements CommandSource {
    private final CommandSender sender;

    public SpigotCommandSource(CommandSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(sender.getName());
    }

    @Override
    public Locale locale() {
        return Locale.ROOT;
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.isInstance(sender) ? Optional.of(type.cast(sender)) : Optional.empty();
    }

    @Override
    public void reply(CommandMessage message) {
        Objects.requireNonNull(message, "message");
        sender.sendMessage(message.text());
    }

    @Override
    public void reply(String message) {
        sender.sendMessage(Objects.requireNonNull(message, "message"));
    }

    @Override
    public boolean hasPermission(String permission) {
        Objects.requireNonNull(permission, "permission");
        return permission.isBlank() || sender.hasPermission(permission);
    }
}
