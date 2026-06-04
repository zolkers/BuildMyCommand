/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;

public final class AnnotationParameterExample {
    private AnnotationParameterExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new InventoryCommands());
        return framework;
    }

    static final class InventoryCommands {
        @Route("kit <target:String> [kit:String] [--amount:Integer] [--silent]")
        CommandResult kit(@RouteCtx CommandContext route) {
            String kit = route.optionalArg("kit", String.class).orElse("starter");
            int amount = route.option("amount", Integer.class).orElse(1);
            return Results.success("Giving " + amount + "x " + kit
                + " to " + route.arg("target", String.class)
                + " silent=" + route.flag("silent"));
        }

        @Suggest("kit")
        List<String> kits() {
            return List.of("starter", "builder", "pvp");
        }
    }
}
