/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.testing;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.riege.buildmycommand.testkit.CommandTestKit;
import dev.riege.buildmycommand.testkit.TestCommandSource;

public final class TestKitExample {
    private TestKitExample() {
    }

    public static CommandResult exerciseCommand() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("ban <target:String>")
            .permission("mod.ban")
            .executes(ctx -> Results.success("Banned " + ctx.arg("target", String.class)));
        TestCommandSource source = TestCommandSource.builder().permission("mod.ban").build();
        CommandTestKit.create(framework, source)
            .dispatch("ban Ada")
            .assertSuccess()
            .assertReply("Banned Ada");
        return framework.dispatch(source, "ban Ada");
    }
}
