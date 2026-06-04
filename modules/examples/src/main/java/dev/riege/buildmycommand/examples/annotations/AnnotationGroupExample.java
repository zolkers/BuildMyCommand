/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class AnnotationGroupExample {
    private AnnotationGroupExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new AdminCommands());
        return framework;
    }

    @Command("admin")
    @Alias("adm")
    @CommandGroup("Administration")
    @Require("staff || owner")
    static final class AdminCommands {
        @Subcommand("reload")
        @Permission("admin.reload")
        CommandResult reload() {
            return Results.success("Reloaded");
        }

        @Subcommand("status")
        CommandResult status() {
            return Results.success("OK");
        }
    }
}
