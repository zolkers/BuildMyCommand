/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.middleware;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MiddlewareChain {
    private final List<CommandMiddleware> middleware;

    public MiddlewareChain(List<CommandMiddleware> middleware) {
        this.middleware = List.copyOf(Objects.requireNonNull(middleware, "middleware"));
    }

    public CommandResult execute(
        CommandContext context,
        CommandNode command,
        List<String> commandPath,
        CommandMiddleware.Chain terminal
    ) {
        return execute(context, command, commandPath, List.of(), terminal);
    }

    public CommandResult execute(
        CommandContext context,
        CommandNode command,
        List<String> commandPath,
        List<CommandMiddleware> commandMiddleware,
        CommandMiddleware.Chain terminal
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(commandPath, "commandPath");
        Objects.requireNonNull(commandMiddleware, "commandMiddleware");
        Objects.requireNonNull(terminal, "terminal");
        List<CommandMiddleware> executionMiddleware = new java.util.ArrayList<>(middleware);
        executionMiddleware.addAll(commandMiddleware);
        return proceed(0, context, command, List.copyOf(commandPath), List.copyOf(executionMiddleware), terminal);
    }

    private CommandResult proceed(
        int index,
        CommandContext context,
        CommandNode command,
        List<String> commandPath,
        List<CommandMiddleware> executionMiddleware,
        CommandMiddleware.Chain terminal
    ) {
        if (index >= executionMiddleware.size()) {
            return Objects.requireNonNull(terminal.proceed(context), "command result");
        }
        CommandMiddleware current = executionMiddleware.get(index);
        CommandResult result = current.execute(
            context,
            command,
            commandPath,
            oneShot(nextContext -> proceed(index + 1, nextContext, command, commandPath, executionMiddleware, terminal))
        );
        return Objects.requireNonNull(result, "command result");
    }

    private static CommandMiddleware.Chain oneShot(CommandMiddleware.Chain delegate) {
        AtomicBoolean called = new AtomicBoolean();
        return context -> {
            if (!called.compareAndSet(false, true)) {
                throw new IllegalStateException("middleware next already called");
            }
            return delegate.proceed(context);
        };
    }
}
