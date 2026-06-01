package dev.riege.buildmycommand.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ArgumentResolver {
    private final ArgumentParserRegistry parsers;

    ArgumentResolver(ArgumentParserRegistry parsers) {
        this.parsers = parsers;
    }

    ParseArgumentsResult parseArguments(List<RegistryArgumentSpec> specs, List<String> tokens) {
        Map<String, Object> values = new HashMap<>();
        int tokenIndex = 0;

        for (RegistryArgumentSpec spec : specs) {
            if (spec.kind() == RegistryArgumentKind.GREEDY
                || spec.kind() == RegistryArgumentKind.OPTIONAL_GREEDY) {
                if (tokenIndex >= tokens.size()) {
                    if (spec.kind() == RegistryArgumentKind.GREEDY) {
                        return ParseArgumentsResult.failure("Missing required argument: " + spec.name());
                    }
                    continue;
                }
                String raw = String.join(" ", tokens.subList(tokenIndex, tokens.size()));
                ParseResult<?> parsed = parsers.parse(spec.type(), raw);
                if (parsed.failure().isPresent()) {
                    return ParseArgumentsResult.failure(parsed.failure().get() + " for argument " + spec.name() + ": " + raw);
                }
                values.put(spec.name(), parsed.value());
                tokenIndex = tokens.size();
                continue;
            }

            if (tokenIndex >= tokens.size()) {
                if (spec.kind() == RegistryArgumentKind.REQUIRED) {
                    return ParseArgumentsResult.failure("Missing required argument: " + spec.name());
                }
                continue;
            }

            String raw = tokens.get(tokenIndex);
            ParseResult<?> parsed = parsers.parse(spec.type(), raw);
            if (parsed.failure().isPresent()) {
                return ParseArgumentsResult.failure(parsed.failure().get() + " for argument " + spec.name() + ": " + raw);
            }
            values.put(spec.name(), parsed.value());
            tokenIndex++;
        }

        if (tokenIndex < tokens.size()) {
            return ParseArgumentsResult.failure("Unexpected argument: " + tokens.get(tokenIndex));
        }

        return ParseArgumentsResult.success(values);
    }

    ParseArgumentPrefixResult parseArgumentPrefix(List<RegistryArgumentSpec> specs, List<String> tokens) {
        Map<String, Object> values = new HashMap<>();
        int tokenIndex = 0;

        for (RegistryArgumentSpec spec : specs) {
            if (spec.kind() == RegistryArgumentKind.GREEDY
                || spec.kind() == RegistryArgumentKind.OPTIONAL_GREEDY) {
                return ParseArgumentPrefixResult.failure("greedy arguments cannot appear before subcommands: " + spec.name());
            }

            if (tokenIndex >= tokens.size()) {
                if (spec.kind() == RegistryArgumentKind.REQUIRED) {
                    return ParseArgumentPrefixResult.failure("Missing required argument: " + spec.name());
                }
                continue;
            }

            String raw = tokens.get(tokenIndex);
            ParseResult<?> parsed = parsers.parse(spec.type(), raw);
            if (parsed.failure().isPresent()) {
                return ParseArgumentPrefixResult.failure(parsed.failure().get() + " for argument " + spec.name() + ": " + raw);
            }
            values.put(spec.name(), parsed.value());
            tokenIndex++;
        }

        return ParseArgumentPrefixResult.success(values, tokenIndex);
    }
}
