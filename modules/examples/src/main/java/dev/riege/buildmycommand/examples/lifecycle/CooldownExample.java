package dev.riege.buildmycommand.examples.lifecycle;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.time.Duration;

public final class CooldownExample {
    private CooldownExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("daily reward")
            .cooldown(Duration.ofHours(24))
            .executes(ctx -> Results.success("Reward claimed"));
        return framework;
    }
}
