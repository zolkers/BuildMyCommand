package dev.riege.buildmycommand.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CommandNode(
    String literal,
    Optional<String> description,
    Optional<String> permission,
    List<String> aliases,
    List<ArgumentSpec<?>> arguments,
    List<FlagSpec<?>> flags,
    List<CommandNode> children,
    CommandMetadata metadata,
    Optional<CommandRegistry.CommandExecutor> executor
) {
    public CommandNode {
        Objects.requireNonNull(literal, "literal");
        if (literal.isBlank()) {
            throw new IllegalArgumentException("literal must not be blank");
        }
        description = Objects.requireNonNull(description, "description");
        permission = Objects.requireNonNull(permission, "permission");
        description.ifPresent(value -> validateMetadata(value, "description"));
        permission.ifPresent(value -> validateMetadata(value, "permission"));
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        flags = List.copyOf(Objects.requireNonNull(flags, "flags"));
        children = List.copyOf(Objects.requireNonNull(children, "children"));
        metadata = Objects.requireNonNull(metadata, "metadata");
        executor = Objects.requireNonNull(executor, "executor");
    }

    public static final class Builder {
        private final String literal;
        private String description;
        private String permission;
        private final List<String> aliases = new ArrayList<>();
        private final List<ArgumentSpec<?>> arguments = new ArrayList<>();
        private final List<FlagSpec<?>> flags = new ArrayList<>();
        private final List<CommandNode> children = new ArrayList<>();
        private CommandMetadata metadata = CommandMetadata.empty();
        private CommandRegistry.CommandExecutor executor;

        Builder(String literal) {
            this.literal = Objects.requireNonNull(literal, "literal");
        }

        public Builder description(String description) {
            this.description = validateMetadata(description, "description");
            return this;
        }

        public Builder permission(String permission) {
            this.permission = validateMetadata(permission, "permission");
            return this;
        }

        public Builder alias(String alias) {
            aliases.add(Objects.requireNonNull(alias, "alias"));
            return this;
        }

        public Builder aliases(String... aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                alias(alias);
            }
            return this;
        }

        public Builder argument(ArgumentSpec<?> argument) {
            arguments.add(Objects.requireNonNull(argument, "argument"));
            return this;
        }

        public Builder flag(FlagSpec<?> flag) {
            flags.add(Objects.requireNonNull(flag, "flag"));
            return this;
        }

        public Builder child(CommandNode child) {
            children.add(Objects.requireNonNull(child, "child"));
            return this;
        }

        public Builder metadata(CommandMetadata metadata) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            return this;
        }

        public Builder handler(CommandRegistry.CommandExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public CommandNode build() {
            return new CommandNode(
                literal,
                Optional.ofNullable(description),
                Optional.ofNullable(permission),
                aliases,
                arguments,
                flags,
                children,
                metadata,
                Optional.ofNullable(executor)
            );
        }

    }

    private static String validateMetadata(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
