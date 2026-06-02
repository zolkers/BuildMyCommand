package dev.riege.buildmycommand.api;

import java.util.List;

@FunctionalInterface
public interface ArgumentParser<T> {
    ArgumentParseResult<T> parse(String rawToken, ArgumentParseContext context);

    default List<Suggestion> suggestions(ArgumentParseContext context) {
        return List.of();
    }
}
