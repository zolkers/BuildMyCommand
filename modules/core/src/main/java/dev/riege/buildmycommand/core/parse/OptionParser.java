package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.core.registry.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OptionParser {
    private final ArgumentParserRegistry parsers;

    public OptionParser(ArgumentParserRegistry parsers) {
        this.parsers = parsers;
    }

    public ParseOptionsResult parseOptions(List<RegistryOptionSpec> specs, List<String> tokens) {
        Map<String, Object> values = new HashMap<>();
        List<String> positionals = new ArrayList<>();

        int tokenIndex = 0;
        while (tokenIndex < tokens.size()) {
            String token = tokens.get(tokenIndex);
            RegistryOptionSpec spec = findOption(specs, token);
            if (spec == null) {
                if (isOptionLike(token)) {
                    return ParseOptionsResult.failure("Unknown flag or option: " + token);
                }
                positionals.add(token);
                tokenIndex++;
                continue;
            }

            if (spec.kind() == RegistryOptionKind.FLAG) {
                values.put(spec.name(), true);
                tokenIndex++;
                continue;
            }

            if (tokenIndex + 1 >= tokens.size()) {
                return ParseOptionsResult.failure("Missing value for option: " + spec.name());
            }

            String raw = tokens.get(tokenIndex + 1);
            ParseResult<?> parsed = parsers.parse(spec.type(), raw);
            if (parsed.failure().isPresent()) {
                return ParseOptionsResult.failure(parsed.failure().get() + " for option " + spec.name() + ": " + raw);
            }
            values.put(spec.name(), parsed.value());
            tokenIndex += 2;
        }

        return ParseOptionsResult.success(values, positionals);
    }

    private static boolean isOptionLike(String token) {
        if (token.startsWith("--")) {
            return token.length() > 2;
        }
        return token.length() > 1 && token.charAt(0) == '-' && !Character.isDigit(token.charAt(1));
    }

    private static RegistryOptionSpec findOption(
        List<RegistryOptionSpec> specs,
        String token
    ) {
        for (RegistryOptionSpec spec : specs) {
            if (token.equals("--" + spec.name()) || spec.aliasOptional().map(alias -> token.equals("-" + alias)).orElse(false)) {
                return spec;
            }
        }
        return null;
    }
}
