package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.FlagSpec;

public final class ManualCommandImporter {
    private ManualCommandImporter() {
    }

    static RegistryCommandNode importNode(CommandNode node) {
        SimpleCommandBuilder builder = new SimpleCommandBuilder(node.literal());
        try {
            configure(builder, node);
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException("invalid command tree", exception);
        }
        return builder.node();
    }

    private static void configure(SimpleCommandBuilder builder, CommandNode node) {
        for (String alias : node.aliases()) {
            builder.alias(alias);
        }
        node.description().ifPresent(builder::description);
        node.permission().ifPresent(builder::permission);
        for (ArgumentSpec<?> argument : node.arguments()) {
            applyArgument(builder, argument);
        }
        for (FlagSpec<?> flag : node.flags()) {
            applyFlag(builder, flag);
        }
        for (CommandNode child : node.children()) {
            builder.subcommand(child.literal(), childBuilder -> configure((SimpleCommandBuilder) childBuilder, child));
        }
        node.executor().ifPresent(builder::executes);
    }

    private static void applyArgument(SimpleCommandBuilder builder, ArgumentSpec<?> argument) {
        switch (argument.kind()) {
            case REQUIRED -> builder.argument(argument.name(), argument.type());
            case OPTIONAL -> builder.optionalArgument(argument.name(), argument.type());
            case GREEDY -> builder.greedyArgument(argument.name(), argument.type());
            case OPTIONAL_GREEDY -> builder.optionalGreedyArgument(argument.name(), argument.type());
        }
    }

    private static void applyFlag(SimpleCommandBuilder builder, FlagSpec<?> flag) {
        String alias = flag.aliasOptional().orElse(null);
        switch (flag.kind()) {
            case FLAG -> builder.flag(flag.name(), alias);
            case VALUE -> builder.option(flag.name(), flag.type(), alias);
        }
    }
}
