package dev.riege.buildmycommand.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CommandMetadata(
    boolean hidden,
    Optional<String> usage,
    List<String> examples,
    Optional<Duration> cooldown,
    Optional<String> requirement,
    Optional<String> group,
    boolean suggestAliases,
    List<CommandMiddleware> middlewares
) {
    public CommandMetadata {
        usage = Objects.requireNonNull(usage, "usage");
        examples = List.copyOf(Objects.requireNonNull(examples, "examples"));
        cooldown = Objects.requireNonNull(cooldown, "cooldown");
        requirement = Objects.requireNonNull(requirement, "requirement");
        group = Objects.requireNonNull(group, "group");
        middlewares = List.copyOf(Objects.requireNonNull(middlewares, "middlewares"));
    }

    public static CommandMetadata empty() {
        return new CommandMetadata(false, Optional.empty(), List.of(), Optional.empty(), Optional.empty(),
            Optional.empty(), true, List.of());
    }

    public static final class Builder {
        private boolean hidden;
        private String usage;
        private final List<String> examples = new ArrayList<>();
        private Duration cooldown;
        private String requirement;
        private String group;
        private boolean suggestAliases = true;
        private final List<CommandMiddleware> middlewares = new ArrayList<>();

        public Builder hidden() {
            hidden = true;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = metadata(usage, "usage");
            return this;
        }

        public Builder example(String example) {
            examples.add(metadata(example, "example"));
            return this;
        }

        public Builder examples(List<String> examples) {
            Objects.requireNonNull(examples, "examples");
            examples.forEach(this::example);
            return this;
        }

        public Builder cooldown(Duration cooldown) {
            Objects.requireNonNull(cooldown, "cooldown");
            if (cooldown.isZero() || cooldown.isNegative()) {
                throw new IllegalArgumentException("cooldown must be positive");
            }
            this.cooldown = cooldown;
            return this;
        }

        public Builder requirement(String requirement) {
            this.requirement = metadata(requirement, "requirement");
            return this;
        }

        public Builder group(String group) {
            this.group = metadata(group, "group");
            return this;
        }

        public Builder suggestAliases(boolean suggestAliases) {
            this.suggestAliases = suggestAliases;
            return this;
        }

        public Builder middleware(CommandMiddleware middleware) {
            middlewares.add(Objects.requireNonNull(middleware, "middleware"));
            return this;
        }

        public Builder middlewares(List<CommandMiddleware> middlewares) {
            Objects.requireNonNull(middlewares, "middlewares");
            middlewares.forEach(this::middleware);
            return this;
        }

        public CommandMetadata build() {
            return new CommandMetadata(
                hidden,
                Optional.ofNullable(usage),
                examples,
                Optional.ofNullable(cooldown),
                Optional.ofNullable(requirement),
                Optional.ofNullable(group),
                suggestAliases,
                middlewares
            );
        }

        private static String metadata(String value, String label) {
            Objects.requireNonNull(value, label);
            if (value.isBlank()) {
                throw new IllegalArgumentException(label + " must not be blank");
            }
            return value;
        }
    }
}
