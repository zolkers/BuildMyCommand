package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftAdapterContracts;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SpigotCommandRegistration {
    private final String fallbackPrefix;
    private final IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter;
    private final Map<String, SpigotNativeCommand> registered = new LinkedHashMap<>();

    public SpigotCommandRegistration(
        String fallbackPrefix,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        this.fallbackPrefix = requireText(fallbackPrefix, "fallbackPrefix");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public String fallbackPrefix() {
        return fallbackPrefix;
    }

    public IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter() {
        return adapter;
    }

    public List<String> labels() {
        return MinecraftAdapterContracts.rootLabels(adapter);
    }

    public MinecraftCommandRegistrationPlan plan() {
        return MinecraftCommandRegistrationPlan.fromNativeAdapter(SpigotMinecraftAdapter.profile(), adapter);
    }

    public List<SpigotNativeCommand> commands() {
        return List.copyOf(registered.values());
    }

    public List<String> register(CommandMap commandMap) {
        Objects.requireNonNull(commandMap, "commandMap");
        unregister(commandMap);

        List<String> labels = labels();
        for (String label : labels) {
            SpigotNativeCommand command = new SpigotNativeCommand(label, adapter);
            commandMap.register(label, fallbackPrefix, command);
            registered.put(label, command);
        }
        return labels;
    }

    public SpigotCommandRegistration unregister(CommandMap commandMap) {
        Objects.requireNonNull(commandMap, "commandMap");
        if (registered.isEmpty()) {
            return this;
        }

        List<Map.Entry<String, SpigotNativeCommand>> entries = new ArrayList<>(registered.entrySet());
        for (Map.Entry<String, SpigotNativeCommand> entry : entries) {
            entry.getValue().unregister(commandMap);
            removeKnownCommand(commandMap, fallbackPrefix, entry.getKey(), entry.getValue());
        }
        registered.clear();
        return this;
    }

    private static void removeKnownCommand(CommandMap commandMap, String fallbackPrefix, String label, Command command) {
        for (Class<?> type = commandMap.getClass(); type != null; type = type.getSuperclass()) {
            Field field = findCommandMapField(type);
            if (field != null) {
                removeFromField(commandMap, field, fallbackPrefix, label, command);
                return;
            }
        }
    }

    private static Field findCommandMapField(Class<?> type) {
        for (String candidate : List.of("knownCommands", "commands")) {
            try {
                Field field = type.getDeclaredField(candidate);
                if (Map.class.isAssignableFrom(field.getType())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // Try the next common field name.
            }
        }
        return null;
    }

    private static void removeFromField(
        CommandMap commandMap,
        Field field,
        String fallbackPrefix,
        String label,
        Command command
    ) {
        try {
            field.setAccessible(true);
            Object value = field.get(commandMap);
            if (value instanceof Map<?, ?> knownCommands) {
                @SuppressWarnings("unchecked")
                Map<String, Command> commands = (Map<String, Command>) knownCommands;
                String fallbackLabel = fallbackPrefix + ":" + label;
                commands.remove(label, command);
                commands.remove(fallbackLabel, command);
                commands.remove(command.getName(), command);
                commands.remove(command.getName().toLowerCase(Locale.ROOT), command);
                commands.remove(fallbackLabel.toLowerCase(Locale.ROOT), command);
            }
        } catch (IllegalAccessException ignored) {
            // Bukkit does not expose removal on CommandMap; best effort keeps this facade server-friendly.
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
