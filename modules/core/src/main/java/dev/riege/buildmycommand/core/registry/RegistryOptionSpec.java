/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.SuggestionProvider;
import java.util.Objects;
import java.util.Optional;

public record RegistryOptionSpec(
    String name,
    Class<?> type,
    String alias,
    RegistryOptionKind kind,
    String suggestionProviderName,
    SuggestionProvider suggestions
) {
    public RegistryOptionSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("flag or option name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        if (alias != null && alias.isBlank()) {
            throw new IllegalArgumentException("flag or option alias must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
    }

    public RegistryOptionSpec(String name, Class<?> type, String alias, RegistryOptionKind kind) {
        this(name, type, alias, kind, null, null);
    }

    public Optional<String> aliasOptional() {
        return Optional.ofNullable(alias);
    }

    public Optional<SuggestionProvider> suggestionProviderOptional() {
        return Optional.ofNullable(suggestions);
    }

    public RegistryOptionSpec suggestions(SuggestionProvider provider) {
        return suggestions(null, provider);
    }

    public RegistryOptionSpec suggestions(String providerName, SuggestionProvider provider) {
        return new RegistryOptionSpec(name, type, alias, kind, providerName, Objects.requireNonNull(provider, "provider"));
    }
}
