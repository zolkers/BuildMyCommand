package dev.buildmycommand.examples;

import dev.buildmycommand.api.Results;
import dev.buildmycommand.core.CommandFramework;

public final class RouteDslExample {
    private RouteDslExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("give <target:String> <item:String> [--amount:Integer|-a]")
            .description("Give an item")
            .permission("inventory.give")
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + " gets "
                    + ctx.option("amount", Integer.class).orElse(1)
                    + " "
                    + ctx.arg("item", String.class)));
        return framework;
    }
}
