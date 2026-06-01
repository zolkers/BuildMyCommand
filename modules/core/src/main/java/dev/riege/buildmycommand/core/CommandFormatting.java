package dev.riege.buildmycommand.core;

final class CommandFormatting {
    private CommandFormatting() {
    }

    static String usageArgument(RegistryArgumentSpec argument) {
        String body = argument.name() + ":" + typeName(argument.type());
        return switch (argument.kind()) {
            case REQUIRED -> "<" + body + ">";
            case OPTIONAL -> "[" + body + "]";
            case GREEDY -> "<" + body + "...>";
            case OPTIONAL_GREEDY -> "[" + body + "...]";
        };
    }

    static String usageOption(RegistryOptionSpec option) {
        StringBuilder builder = new StringBuilder("[--").append(option.name());
        if (option.kind() == RegistryOptionKind.VALUE) {
            builder.append(":").append(typeName(option.type()));
        }
        option.aliasOptional().ifPresent(alias -> builder.append("|-").append(alias));
        return builder.append("]").toString();
    }

    static String schemaArgumentKind(RegistryArgumentKind kind) {
        return switch (kind) {
            case REQUIRED -> "required";
            case OPTIONAL -> "optional";
            case GREEDY -> "greedy";
            case OPTIONAL_GREEDY -> "optional-greedy";
        };
    }

    static String schemaOptionKind(RegistryOptionKind kind) {
        return switch (kind) {
            case FLAG -> "flag";
            case VALUE -> "value";
        };
    }

    static String typeName(Class<?> type) {
        if (type == int.class) {
            return "int";
        }
        if (type == long.class) {
            return "long";
        }
        if (type == double.class) {
            return "double";
        }
        if (type == boolean.class) {
            return "boolean";
        }
        return type.getSimpleName();
    }

    static void appendLine(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
