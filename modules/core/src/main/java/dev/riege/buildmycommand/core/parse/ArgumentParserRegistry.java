package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.ArgumentParseResult;
import dev.riege.buildmycommand.api.ArgumentParser;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionProvider;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ArgumentParserRegistry {
    private final Map<Class<?>, ArgumentParser<?>> parsers;
    private final Map<Class<?>, SuggestionProvider> suggestionProviders;

    public ArgumentParserRegistry() {
        this(defaultParsers(), Map.of());
    }

    public ArgumentParserRegistry(
        Map<Class<?>, ArgumentParser<?>> parsers,
        Map<Class<?>, SuggestionProvider> suggestionProviders
    ) {
        this.parsers = new LinkedHashMap<>(Objects.requireNonNull(parsers, "parsers"));
        this.suggestionProviders = new LinkedHashMap<>(Objects.requireNonNull(suggestionProviders, "suggestionProviders"));
    }

    public <T> void register(Class<T> type, ArgumentParser<? extends T> parser) {
        parsers.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(parser, "parser"));
    }

    public <T> void registerSuggestions(Class<T> type, SuggestionProvider provider) {
        suggestionProviders.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(provider, "provider"));
    }

    public Map<Class<?>, ArgumentParser<?>> parsers() {
        return Map.copyOf(parsers);
    }

    public Map<Class<?>, SuggestionProvider> suggestionProviders() {
        return Map.copyOf(suggestionProviders);
    }

    ParseResult<?> parse(Class<?> type, String raw, ArgumentParseContext context) {
        ArgumentParser<?> parser = parsers.get(type);
        if (parser != null) {
            ArgumentParseResult<?> result = parser.parse(raw, context);
            if (result.failure().isPresent()) {
                return ParseResult.failure(result.failure().get());
            }
            return ParseResult.success(result.value().orElseThrow());
        }
        if (type.isEnum()) {
            return parseEnum(type, raw);
        }
        return ParseResult.failure("No parser registered");
    }

    public List<Suggestion> suggestions(Class<?> type, ArgumentParseContext context) {
        List<Suggestion> parserSuggestions = Optional.ofNullable(parsers.get(type))
            .map(parser -> parser.suggestions(context))
            .orElse(List.of());
        if (!parserSuggestions.isEmpty()) {
            return parserSuggestions;
        }
        SuggestionProvider provider = suggestionProviders.get(type);
        if (provider != null) {
            return provider.richSuggestions(context);
        }
        if (type.isEnum()) {
            return enumSuggestions(type, context);
        }
        return List.of();
    }

    private static Map<Class<?>, ArgumentParser<?>> defaultParsers() {
        Map<Class<?>, ArgumentParser<?>> defaults = new LinkedHashMap<>();
        defaults.put(String.class, parser(ParseResult::success));
        defaults.put(int.class, parser(ArgumentParserRegistry::parseInteger));
        defaults.put(Integer.class, parser(ArgumentParserRegistry::parseInteger));
        defaults.put(long.class, parser(ArgumentParserRegistry::parseLong));
        defaults.put(Long.class, parser(ArgumentParserRegistry::parseLong));
        defaults.put(float.class, parser(ArgumentParserRegistry::parseFloat));
        defaults.put(Float.class, parser(ArgumentParserRegistry::parseFloat));
        defaults.put(double.class, parser(ArgumentParserRegistry::parseDouble));
        defaults.put(Double.class, parser(ArgumentParserRegistry::parseDouble));
        defaults.put(boolean.class, parser(ArgumentParserRegistry::parseBoolean));
        defaults.put(Boolean.class, parser(ArgumentParserRegistry::parseBoolean));
        defaults.put(UUID.class, parser(ArgumentParserRegistry::parseUuid));
        defaults.put(Duration.class, parser(ArgumentParserRegistry::parseDuration));
        defaults.put(LocalDate.class, parser(ArgumentParserRegistry::parseLocalDate));
        defaults.put(LocalDateTime.class, parser(ArgumentParserRegistry::parseLocalDateTime));
        defaults.put(Path.class, parser(ArgumentParserRegistry::parsePath));
        defaults.put(URI.class, parser(ArgumentParserRegistry::parseUri));
        defaults.put(URL.class, parser(ArgumentParserRegistry::parseUrl));
        return defaults;
    }

    private static <T> ArgumentParser<T> parser(SimpleParser<T> parser) {
        return (raw, context) -> {
            ParseResult<T> result = parser.parse(raw);
            if (result.failure().isPresent()) {
                return ArgumentParseResult.failure(result.failure().get());
            }
            return ArgumentParseResult.success(result.value());
        };
    }

    private static ParseResult<Integer> parseInteger(String value) {
        try {
            return ParseResult.success(Integer.valueOf(value));
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid integer");
        }
    }

    private static ParseResult<Long> parseLong(String value) {
        try {
            return ParseResult.success(Long.valueOf(value));
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid long");
        }
    }

    private static ParseResult<Float> parseFloat(String value) {
        try {
            Float parsed = Float.valueOf(value);
            if (!Float.isFinite(parsed)) {
                return ParseResult.failure("Invalid float");
            }
            return ParseResult.success(parsed);
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid float");
        }
    }

    private static ParseResult<Double> parseDouble(String value) {
        try {
            Double parsed = Double.valueOf(value);
            if (!Double.isFinite(parsed)) {
                return ParseResult.failure("Invalid double");
            }
            return ParseResult.success(parsed);
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid double");
        }
    }

    private static ParseResult<Boolean> parseBoolean(String value) {
        if ("true".equals(value)) {
            return ParseResult.success(true);
        }
        if ("false".equals(value)) {
            return ParseResult.success(false);
        }
        return ParseResult.failure("Invalid boolean");
    }

    private static ParseResult<UUID> parseUuid(String value) {
        try {
            return ParseResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return ParseResult.failure("Invalid UUID");
        }
    }

    private static ParseResult<Duration> parseDuration(String value) {
        try {
            return ParseResult.success(Duration.parse(value));
        } catch (DateTimeParseException exception) {
            return ParseResult.failure("Invalid duration");
        }
    }

    private static ParseResult<LocalDate> parseLocalDate(String value) {
        try {
            return ParseResult.success(LocalDate.parse(value));
        } catch (DateTimeParseException exception) {
            return ParseResult.failure("Invalid LocalDate");
        }
    }

    private static ParseResult<LocalDateTime> parseLocalDateTime(String value) {
        try {
            return ParseResult.success(LocalDateTime.parse(value));
        } catch (DateTimeParseException exception) {
            return ParseResult.failure("Invalid LocalDateTime");
        }
    }

    private static ParseResult<Path> parsePath(String value) {
        try {
            return ParseResult.success(Path.of(value));
        } catch (InvalidPathException exception) {
            return ParseResult.failure("Invalid path");
        }
    }

    private static ParseResult<URI> parseUri(String value) {
        try {
            return ParseResult.success(URI.create(value));
        } catch (IllegalArgumentException exception) {
            return ParseResult.failure("Invalid URI");
        }
    }

    private static ParseResult<URL> parseUrl(String value) {
        try {
            return ParseResult.success(URI.create(value).toURL());
        } catch (IllegalArgumentException | MalformedURLException exception) {
            return ParseResult.failure("Invalid URL");
        }
    }

    private static ParseResult<?> parseEnum(Class<?> type, String raw) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(raw)) {
                return ParseResult.success(constant);
            }
        }
        return ParseResult.failure("Invalid enum");
    }

    private static List<Suggestion> enumSuggestions(Class<?> type, ArgumentParseContext context) {
        return java.util.Arrays.stream(type.getEnumConstants())
            .map(constant -> new Suggestion(
                ((Enum<?>) constant).name(),
                Optional.empty(),
                context.replacementStart(),
                context.replacementEnd(),
                context.suggestionType(),
                0
            ))
            .toList();
    }

    @FunctionalInterface
    private interface SimpleParser<T> {
        ParseResult<T> parse(String value);
    }
}
