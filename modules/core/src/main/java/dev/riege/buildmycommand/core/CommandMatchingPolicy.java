package dev.riege.buildmycommand.core;

import java.util.Locale;
import java.util.Objects;

public final class CommandMatchingPolicy {
    private final boolean caseInsensitiveLiterals;
    private final boolean caseInsensitiveOptions;

    public CommandMatchingPolicy(boolean caseInsensitiveLiterals, boolean caseInsensitiveOptions) {
        this.caseInsensitiveLiterals = caseInsensitiveLiterals;
        this.caseInsensitiveOptions = caseInsensitiveOptions;
    }

    public static CommandMatchingPolicy strict() {
        return new CommandMatchingPolicy(false, false);
    }

    public boolean caseInsensitiveLiterals() {
        return caseInsensitiveLiterals;
    }

    public boolean caseInsensitiveOptions() {
        return caseInsensitiveOptions;
    }

    public String literalKey(String literal) {
        Objects.requireNonNull(literal, "literal");
        return caseInsensitiveLiterals ? literal.toLowerCase(Locale.ROOT) : literal;
    }

    public boolean optionEquals(String actual, String expected) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(expected, "expected");
        return caseInsensitiveOptions ? actual.equalsIgnoreCase(expected) : actual.equals(expected);
    }
}
