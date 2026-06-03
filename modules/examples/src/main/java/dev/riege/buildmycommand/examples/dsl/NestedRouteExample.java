package dev.riege.buildmycommand.examples.dsl;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class NestedRouteExample {
    private NestedRouteExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        framework.registry()
            .route("user|u rank|role set|put <target:String> <rank:String> [--temporary|-t]")
            .description("Set a user rank")
            .permission("user.rank.set")
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + " -> "
                    + ctx.arg("rank", String.class)
                    + " temporary="
                    + ctx.flag("temporary")));
        framework.registry()
            .route("user|u rank|role clear|remove <target:String>")
            .description("Clear a user rank")
            .permission("user.rank.clear")
            .executes(ctx -> Results.success("Cleared " + ctx.arg("target", String.class)));
        return framework;
    }
}
