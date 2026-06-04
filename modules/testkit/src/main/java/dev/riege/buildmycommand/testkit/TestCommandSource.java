/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.testkit;

import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class TestCommandSource implements CommandSource {
    private final String id;
    private final String name;
    private final Locale locale;
    private final Set<String> permissions;
    private final Map<String, Object> metadata;
    private final List<CommandMessage> replies = new ArrayList<>();

    private TestCommandSource(
        String id,
        String name,
        Locale locale,
        Set<String> permissions,
        Map<String, Object> metadata
    ) {
        this.id = id;
        this.name = name;
        this.locale = Objects.requireNonNull(locale, "locale");
        this.permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    public static TestCommandSource create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<CommandMessage> replies() {
        return List.copyOf(replies);
    }

    @Override
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public Optional<Object> metadata(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(metadata.get(key));
    }

    @Override
    public void reply(CommandMessage message) {
        replies.add(Objects.requireNonNull(message, "message"));
    }

    @Override
    public boolean hasPermission(String permission) {
        Objects.requireNonNull(permission, "permission");
        return permissions.contains(permission);
    }

    @Override
    public Set<String> permissions() {
        return permissions;
    }

    public static final class Builder {
        private String id = "test-source";
        private String name = "Test Source";
        private Locale locale = Locale.ROOT;
        private final Set<String> permissions = new HashSet<>();
        private final Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = validateOptional(id, "id");
            return this;
        }

        public Builder name(String name) {
            this.name = validateOptional(name, "name");
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = Objects.requireNonNull(locale, "locale");
            return this;
        }

        public Builder permission(String permission) {
            permissions.add(validate(permission, "permission"));
            return this;
        }

        public Builder permissions(String... permissions) {
            Objects.requireNonNull(permissions, "permissions");
            for (String permission : permissions) {
                permission(permission);
            }
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(validate(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        public TestCommandSource build() {
            return new TestCommandSource(id, name, locale, permissions, metadata);
        }

        private static String validateOptional(String value, String label) {
            if (value == null) {
                return null;
            }
            return validate(value, label);
        }

        private static String validate(String value, String label) {
            Objects.requireNonNull(value, label);
            if (value.isBlank()) {
                throw new IllegalArgumentException(label + " must not be blank");
            }
            return value;
        }
    }
}
