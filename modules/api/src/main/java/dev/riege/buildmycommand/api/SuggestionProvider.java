/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface SuggestionProvider {
    List<String> suggestions(ArgumentParseContext context);

    default List<Suggestion> richSuggestions(ArgumentParseContext context) {
        return suggestions(context).stream()
            .map(value -> new Suggestion(
                value,
                Optional.empty(),
                context.replacementStart(),
                context.replacementEnd(),
                context.suggestionType(),
                0
            ))
            .toList();
    }
}
