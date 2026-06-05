/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import java.util.List;
import java.util.Objects;

/**
 * Parsed view of the text passed to a help command.
 *
 * <p>The query preserves whether the user ended with a space, which is important for progressive
 * completion: {@code "admin"} completes the current segment, while {@code "admin "} completes the
 * next segment under {@code admin}.</p>
 */
public record HelpQuery(String raw, int cursor, String prefix, List<String> tokens, String currentToken) {
    public HelpQuery {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(prefix, "prefix");
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        Objects.requireNonNull(currentToken, "currentToken");
        if (cursor < 0 || cursor > raw.length()) {
            throw new IllegalArgumentException("help query cursor out of range");
        }
    }

    public static HelpQuery parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        return of(raw, raw.length());
    }

    public static HelpQuery of(String raw, int cursor) {
        Objects.requireNonNull(raw, "raw");
        if (cursor < 0 || cursor > raw.length()) {
            throw new IllegalArgumentException("help query cursor out of range");
        }
        String prefix = raw.substring(0, cursor).stripLeading();
        List<String> tokens = tokens(prefix);
        String currentToken = prefix.isEmpty() || endsWithWhitespace(prefix) || tokens.isEmpty()
            ? ""
            : tokens.getLast();
        return new HelpQuery(raw, cursor, prefix, tokens, currentToken);
    }

    public boolean completingNextToken() {
        return !prefix.isEmpty() && endsWithWhitespace(prefix);
    }

    public List<String> prefixTokens() {
        if (completingNextToken() || tokens.isEmpty()) {
            return tokens;
        }
        return tokens.subList(0, tokens.size() - 1);
    }

    public boolean blank() {
        return prefix.isBlank();
    }

    private static boolean endsWithWhitespace(String input) {
        return !input.isEmpty() && Character.isWhitespace(input.charAt(input.length() - 1));
    }

    private static List<String> tokens(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return List.of(trimmed.split("\\s+"));
    }
}
