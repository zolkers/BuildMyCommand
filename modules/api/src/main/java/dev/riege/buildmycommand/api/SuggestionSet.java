/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fluent, immutable suggestion result returned by advanced {@code @Suggest} methods or builder helpers.
 *
 * <p>A set can be static, dynamic, filtered against the current token, enriched with tooltips, and
 * capped to a small number of entries. The final replacement range and suggestion kind are taken from
 * {@link SuggestionContext}, so providers do not need to manually copy offsets for common cases.</p>
 */
public final class SuggestionSet {
    private final List<SuggestionValue> values;
    private final boolean filterCurrentToken;
    private final Optional<String> tooltip;
    private final Optional<Integer> priority;
    private final Optional<Integer> limit;

    private SuggestionSet(
        List<SuggestionValue> values,
        boolean filterCurrentToken,
        Optional<String> tooltip,
        Optional<Integer> priority,
        Optional<Integer> limit
    ) {
        this.values = List.copyOf(Objects.requireNonNull(values, "values"));
        this.filterCurrentToken = filterCurrentToken;
        this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.limit = Objects.requireNonNull(limit, "limit");
    }

    public static SuggestionSet empty() {
        return new SuggestionSet(List.of(), false, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static SuggestionSet of(String... values) {
        Objects.requireNonNull(values, "values");
        return of(Arrays.asList(values));
    }

    public static SuggestionSet of(Iterable<?> values) {
        Objects.requireNonNull(values, "values");
        List<SuggestionValue> entries = new ArrayList<>();
        for (Object value : values) {
            entries.add(new SuggestionValue(String.valueOf(Objects.requireNonNull(value, "suggestion value")),
                Optional.empty(), 0));
        }
        return new SuggestionSet(entries, false, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static SuggestionSet rich(Iterable<Suggestion> suggestions) {
        Objects.requireNonNull(suggestions, "suggestions");
        List<SuggestionValue> entries = new ArrayList<>();
        for (Suggestion suggestion : suggestions) {
            Objects.requireNonNull(suggestion, "suggestion");
            entries.add(new SuggestionValue(suggestion.value(), suggestion.tooltip(), suggestion.priority()));
        }
        return new SuggestionSet(entries, false, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public SuggestionSet filteringCurrentToken() {
        return new SuggestionSet(values, true, tooltip, priority, limit);
    }

    public SuggestionSet tooltip(String tooltip) {
        Objects.requireNonNull(tooltip, "tooltip");
        return new SuggestionSet(values, filterCurrentToken, Optional.of(tooltip), priority, limit);
    }

    public SuggestionSet priority(int priority) {
        return new SuggestionSet(values, filterCurrentToken, tooltip, Optional.of(priority), limit);
    }

    public SuggestionSet limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("suggestion limit must be positive");
        }
        return new SuggestionSet(values, filterCurrentToken, tooltip, priority, Optional.of(limit));
    }

    public List<Suggestion> toSuggestions(SuggestionContext context) {
        Objects.requireNonNull(context, "context");
        List<Suggestion> suggestions = new ArrayList<>();
        String token = context.currentToken();
        for (SuggestionValue value : values) {
            if (filterCurrentToken && !value.value().regionMatches(true, 0, token, 0, token.length())) {
                continue;
            }
            suggestions.add(new Suggestion(
                value.value(),
                tooltip.or(() -> value.tooltip()),
                context.replacementStart(),
                context.replacementEnd(),
                context.suggestionType(),
                priority.orElse(value.priority())
            ));
            if (limit.isPresent() && suggestions.size() >= limit.orElseThrow()) {
                break;
            }
        }
        return List.copyOf(suggestions);
    }

    private record SuggestionValue(String value, Optional<String> tooltip, int priority) {
        private SuggestionValue {
            Objects.requireNonNull(value, "value");
            tooltip = Objects.requireNonNull(tooltip, "tooltip");
        }
    }
}
