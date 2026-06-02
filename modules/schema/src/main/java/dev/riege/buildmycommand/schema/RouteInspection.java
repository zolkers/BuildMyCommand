package dev.riege.buildmycommand.schema;

import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.FlagSpec;

import java.util.List;
import java.util.Objects;

public record RouteInspection(
    List<String> tokens,
    List<String> matchedPath,
    List<ArgumentSpec<?>> arguments,
    List<FlagSpec<?>> options,
    boolean executable
) {
    public RouteInspection {
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        matchedPath = List.copyOf(Objects.requireNonNull(matchedPath, "matchedPath"));
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        options = List.copyOf(Objects.requireNonNull(options, "options"));
    }
}
