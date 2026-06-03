package dev.riege.buildmycommand.adapters;

public record AdapterMatchingPolicy(
    boolean caseInsensitiveLiterals,
    boolean caseInsensitiveOptions,
    boolean caseSensitiveArguments
) {
    public static AdapterMatchingPolicy strict() {
        return new AdapterMatchingPolicy(false, false, true);
    }
}
