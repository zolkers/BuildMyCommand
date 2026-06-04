/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.basics;

import dev.riege.buildmycommand.api.Arguments;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.Flags;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class ManualNodeExample {
    private ManualNodeExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().register(Commands.literal("ban")
            .description("Ban a user")
            .permission("mod.ban")
            .argument(Arguments.required("target", String.class))
            .argument(Arguments.greedyOptional("reason", String.class))
            .flag(Flags.bool("silent").alias("s"))
            .handler(ctx -> Results.success(
                "Banned "
                    + ctx.arg("target", String.class)
                    + " silent="
                    + ctx.flag("silent")
                    + " reason="
                    + ctx.optionalArg("reason", String.class).orElse("none")))
            .build());
        return framework;
    }
}
