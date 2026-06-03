package dev.riege.buildmycommand.api;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Factory methods for building {@link SuggestionProvider} instances without manual offset handling.
 */
public final class Suggestions {
    private Suggestions() {
    }

    public static SuggestionProvider list(String... values) {
        return set(SuggestionSet.of(values));
    }

    public static SuggestionProvider set(SuggestionSet set) {
        Objects.requireNonNull(set, "set");
        return new SuggestionProvider() {
            @Override
            public List<String> suggestions(ArgumentParseContext context) {
                return richSuggestions(context).stream()
                    .map(Suggestion::value)
                    .toList();
            }

            @Override
            public List<Suggestion> richSuggestions(ArgumentParseContext context) {
                return set.toSuggestions(SuggestionContext.from(context));
            }
        };
    }

    public static SuggestionProvider dynamic(Function<SuggestionContext, SuggestionSet> provider) {
        Objects.requireNonNull(provider, "provider");
        return new SuggestionProvider() {
            @Override
            public List<String> suggestions(ArgumentParseContext context) {
                return richSuggestions(context).stream()
                    .map(Suggestion::value)
                    .toList();
            }

            @Override
            public List<Suggestion> richSuggestions(ArgumentParseContext context) {
                SuggestionSet set = Objects.requireNonNull(provider.apply(SuggestionContext.from(context)),
                    "suggestion set");
                return set.toSuggestions(SuggestionContext.from(context));
            }
        };
    }
}
