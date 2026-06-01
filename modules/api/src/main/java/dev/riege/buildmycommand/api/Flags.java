package dev.riege.buildmycommand.api;

public final class Flags {
    private Flags() {
    }

    public static FlagSpec<Boolean> bool(String name) {
        return new FlagSpec<>(name, Boolean.class, null, FlagSpec.Kind.FLAG);
    }

    public static <T> FlagSpec<T> option(String name, Class<T> type) {
        return new FlagSpec<>(name, type, null, FlagSpec.Kind.VALUE);
    }
}
