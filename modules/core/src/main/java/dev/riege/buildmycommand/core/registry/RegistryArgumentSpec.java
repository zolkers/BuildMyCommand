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

public record RegistryArgumentSpec(
    String name,
    Class<?> type,
    RegistryArgumentKind kind,
    String suggestionProviderName,
    SuggestionProvider suggestions
) {
    public RegistryArgumentSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("argument name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
    }

    public RegistryArgumentSpec(String name, Class<?> type, RegistryArgumentKind kind) {
        this(name, type, kind, null, null);
    }

    public Optional<SuggestionProvider> suggestionProviderOptional() {
        return Optional.ofNullable(suggestions);
    }

    public RegistryArgumentSpec suggestions(SuggestionProvider provider) {
        return suggestions(null, provider);
    }

    public RegistryArgumentSpec suggestions(String providerName, SuggestionProvider provider) {
        return new RegistryArgumentSpec(name, type, kind, providerName, Objects.requireNonNull(provider, "provider"));
    }
}
