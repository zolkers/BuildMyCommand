/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class CommandExceptionHandlers {
    private CommandExceptionHandlers() {
    }

    public static CommandExceptionHandler rethrow() {
        return (context, error) -> {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(error, "error");
            if (error instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (error instanceof Error fatal) {
                throw fatal;
            }
            throw new RuntimeException(error);
        };
    }

    public static CommandExceptionHandler failureMessage() {
        return new CommandExceptionHandler() {
            @Override
            public CommandResult handle(CommandExceptionContext context, Throwable error) {
                return failureMessage(context, error);
            }
        };
    }

    private static CommandResult failureMessage(CommandExceptionContext context, Throwable error) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(error, "error");
        if (error instanceof CommandException) {
            return Results.failure(error.getMessage());
        }
        if (error instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (error instanceof Error fatal) {
            throw fatal;
        }
        throw new RuntimeException(error);
    }

    public static Builder mapping() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Mapping<? extends Throwable>> mappings = new ArrayList<>();
        private CommandExceptionHandler fallback = rethrow();

        private Builder() {
        }

        public <T extends Throwable> Builder on(
            Class<T> type,
            BiFunction<CommandExceptionContext, T, CommandResult> handler
        ) {
            mappings.add(new Mapping<>(Objects.requireNonNull(type, "type"), Objects.requireNonNull(handler,
                "handler")));
            return this;
        }

        public Builder fallback(CommandExceptionHandler fallback) {
            this.fallback = Objects.requireNonNull(fallback, "fallback");
            return this;
        }

        public CommandExceptionHandler build() {
            List<Mapping<? extends Throwable>> snapshot = List.copyOf(mappings);
            CommandExceptionHandler fallbackSnapshot = fallback;
            return (context, error) -> {
                Objects.requireNonNull(context, "context");
                Objects.requireNonNull(error, "error");
                for (Mapping<? extends Throwable> mapping : snapshot) {
                    if (mapping.type().isInstance(error)) {
                        return mapping.handle(context, error);
                    }
                }
                return fallbackSnapshot.handle(context, error);
            };
        }
    }

    private record Mapping<T extends Throwable>(
        Class<T> type,
        BiFunction<CommandExceptionContext, T, CommandResult> handler
    ) {
        private CommandResult handle(CommandExceptionContext context, Throwable error) {
            return handler.apply(context, type.cast(error));
        }
    }
}
