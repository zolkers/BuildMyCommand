package dev.riege.buildmycommand.examples;

import dev.riege.buildmycommand.api.Arguments;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.Flags;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class ManualExample {
    private ManualExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().register(Commands.literal("ban")
            .description("Ban a user")
            .permission("mod.ban")
            .argument(Arguments.required("target", String.class))
            .argument(Arguments.greedyOptional("reason", String.class))
            .flag(Flags.bool("silent").alias("s"))
            .handler(ctx -> Results.success("Banned " + ctx.arg("target", String.class)))
            .build());
        return framework;
    }
}
