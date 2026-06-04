/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.basics;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class BuilderCommandsExample {
    private BuilderCommandsExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command
            .description("Reply with Pong")
            .alias("p")
            .executes(ctx -> Results.success("Pong")));
        framework.registry().command("echo", command -> command
            .description("Echo text back")
            .greedyArgument("text", String.class)
            .executes(ctx -> Results.success(ctx.arg("text", String.class))));
        return framework;
    }
}
