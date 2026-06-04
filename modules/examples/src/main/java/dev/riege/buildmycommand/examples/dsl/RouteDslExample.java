/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.dsl;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class RouteDslExample {
    private RouteDslExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("give|grant <target:String> <item:String> [--amount:Integer|-a] [--silent|-s]")
            .description("Give an item")
            .permission("inventory.give")
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + " gets "
                    + ctx.option("amount", Integer.class).orElse(1)
                    + " "
                    + ctx.arg("item", String.class)
                    + " silent="
                    + ctx.flag("silent")));
        return framework;
    }
}
