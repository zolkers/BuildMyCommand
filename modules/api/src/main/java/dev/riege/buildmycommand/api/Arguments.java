package dev.riege.buildmycommand.api;

public final class Arguments {
    private Arguments() {
    }

    public static <T> ArgumentSpec<T> required(String name, Class<T> type) {
        return new ArgumentSpec<>(name, type, ArgumentSpec.Kind.REQUIRED);
    }

    public static <T> ArgumentSpec<T> optional(String name, Class<T> type) {
        return new ArgumentSpec<>(name, type, ArgumentSpec.Kind.OPTIONAL);
    }

    public static <T> ArgumentSpec<T> greedy(String name, Class<T> type) {
        return new ArgumentSpec<>(name, type, ArgumentSpec.Kind.GREEDY);
    }

    public static <T> ArgumentSpec<T> greedyOptional(String name, Class<T> type) {
        return new ArgumentSpec<>(name, type, ArgumentSpec.Kind.OPTIONAL_GREEDY);
    }
}
