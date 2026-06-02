package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Objects;

public record AdapterRuntime(
    CommandFramework framework,
    CommandPlatform platform
) {
    public AdapterRuntime {
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(platform, "platform");
    }

    public CommandResult dispatch(CommandInput input) {
        Objects.requireNonNull(input, "input");
        return framework.dispatch(input);
    }

    public AdapterRegistrationLabels registrationLabels() {
        return AdapterRegistrationLabels.from(framework);
    }
}
