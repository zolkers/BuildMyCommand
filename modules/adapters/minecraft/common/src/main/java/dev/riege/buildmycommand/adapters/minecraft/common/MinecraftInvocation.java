/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MinecraftInvocation(
    String rawInput,
    String normalizedInput,
    String label,
    List<String> args,
    int cursor
) {
    public MinecraftInvocation {
        Objects.requireNonNull(rawInput, "rawInput");
        normalizedInput = requireText(normalizedInput, "normalizedInput");
        label = requireText(label, "label");
        args = List.copyOf(Objects.requireNonNull(args, "args"));
        if (cursor < 0) {
            throw new IllegalArgumentException("cursor must be positive or zero");
        }
        cursor = Math.min(cursor, normalizedInput.length());
    }

    public static MinecraftInvocation slash(String commandLine, int cursor) {
        Objects.requireNonNull(commandLine, "commandLine");
        String normalized = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        int normalizedCursor = commandLine.startsWith("/") ? Math.max(0, cursor - 1) : cursor;
        String label = firstToken(normalized);
        return new MinecraftInvocation(commandLine, normalized, label, argsAfterLabel(normalized), normalizedCursor);
    }

    public static MinecraftInvocation labelAndArgs(String label, String[] args, int cursorArgIndex) {
        Objects.requireNonNull(args, "args");
        String validatedLabel = requireText(label, "label");
        if (cursorArgIndex < 0) {
            throw new IllegalArgumentException("cursorArgIndex must be positive or zero");
        }

        List<String> arguments = List.copyOf(Arrays.asList(args));
        String normalized = args.length == 0
            ? validatedLabel
            : validatedLabel + " " + String.join(" ", arguments);
        return new MinecraftInvocation(normalized, normalized, validatedLabel, arguments, cursorFor(validatedLabel, args, cursorArgIndex));
    }

    public Optional<String> currentArgPrefix() {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(args.get(args.size() - 1));
    }

    private static int cursorFor(String label, String[] args, int cursorArgIndex) {
        if (args.length == 0) {
            return label.length();
        }
        int cursor = label.length() + 1;
        int boundedIndex = Math.min(cursorArgIndex, args.length - 1);
        for (int index = 0; index < boundedIndex; index++) {
            cursor += args[index].length() + 1;
        }
        return cursor + args[boundedIndex].length();
    }

    private static String firstToken(String normalized) {
        String trimmed = normalized.stripLeading();
        int separator = trimmed.indexOf(' ');
        return separator < 0 ? trimmed : trimmed.substring(0, separator);
    }

    private static List<String> argsAfterLabel(String normalized) {
        String trimmed = normalized.stripLeading();
        int separator = trimmed.indexOf(' ');
        if (separator < 0) {
            return List.of();
        }
        String tail = trimmed.substring(separator + 1);
        if (tail.isEmpty()) {
            return List.of("");
        }
        return List.of(tail.split(" ", -1));
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
