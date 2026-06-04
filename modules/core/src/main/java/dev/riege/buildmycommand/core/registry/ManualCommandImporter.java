/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.FlagSpec;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;

import java.util.Optional;

public final class ManualCommandImporter {
    private ManualCommandImporter() {
    }

    static RegistryCommandNode importNode(CommandNode node) {
        return importNode(node, CommandMatchingPolicy.strict());
    }

    static RegistryCommandNode importNode(CommandNode node, CommandMatchingPolicy matchingPolicy) {
        SimpleCommandBuilder builder = new SimpleCommandBuilder(node.literal(), matchingPolicy);
        try {
            configure(builder, node);
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException("invalid command tree", exception);
        }
        return builder.node();
    }

    public static CommandNode exportNode(RegistryCommandNode node) {
        CommandNode.Builder builder = Commands.literal(node.literal());
        node.descriptionOptional().ifPresent(builder::description);
        node.permissionOptional().ifPresent(builder::permission);
        builder.metadata(node.metadata());
        builder.aliases(node.aliases().toArray(String[]::new));
        for (RegistryArgumentSpec argument : node.arguments()) {
            builder.argument(exportArgument(argument));
        }
        for (RegistryOptionSpec option : node.options()) {
            builder.flag(exportOption(option));
        }
        for (RegistryCommandNode child : node.uniqueChildren()) {
            builder.child(exportNode(child));
        }
        if (node.isExecutable()) {
            builder.handler(node.executor());
        }
        return builder.build();
    }

    private static void configure(SimpleCommandBuilder builder, CommandNode node) {
        for (String alias : node.aliases()) {
            builder.alias(alias);
        }
        node.description().ifPresent(builder::description);
        node.permission().ifPresent(builder::permission);
        applyMetadata(builder, node.metadata());
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

    private static void applyMetadata(SimpleCommandBuilder builder, CommandMetadata metadata) {
        if (metadata.hidden()) {
            builder.hidden();
        }
        metadata.usage().ifPresent(builder::usage);
        metadata.examples().forEach(builder::example);
        metadata.cooldown().ifPresent(builder::cooldown);
        metadata.requirement().ifPresent(builder::requirement);
        metadata.group().ifPresent(builder::group);
        builder.suggestAliases(metadata.suggestAliases());
        metadata.middlewares().forEach(builder::middleware);
    }

    private static void applyArgument(SimpleCommandBuilder builder, ArgumentSpec<?> argument) {
        if (argument.kind() == ArgumentSpec.Kind.REQUIRED) {
            builder.argument(argument.name(), argument.type());
        } else if (argument.kind() == ArgumentSpec.Kind.OPTIONAL) {
            builder.optionalArgument(argument.name(), argument.type());
        } else if (argument.kind() == ArgumentSpec.Kind.GREEDY) {
            builder.greedyArgument(argument.name(), argument.type());
        } else {
            builder.optionalGreedyArgument(argument.name(), argument.type());
        }
    }

    private static void applyFlag(SimpleCommandBuilder builder, FlagSpec<?> flag) {
        String alias = flag.aliasOptional().orElse(null);
        if (flag.kind() == FlagSpec.Kind.FLAG) {
            builder.flag(flag.name(), alias);
        } else {
            builder.option(flag.name(), flag.type(), alias);
        }
    }

    private static ArgumentSpec<?> exportArgument(RegistryArgumentSpec argument) {
        return switch (argument.kind()) {
            case REQUIRED -> new ArgumentSpec<>(argument.name(), argument.type(), ArgumentSpec.Kind.REQUIRED);
            case OPTIONAL -> new ArgumentSpec<>(argument.name(), argument.type(), ArgumentSpec.Kind.OPTIONAL);
            case GREEDY -> new ArgumentSpec<>(argument.name(), argument.type(), ArgumentSpec.Kind.GREEDY);
            case OPTIONAL_GREEDY -> new ArgumentSpec<>(argument.name(), argument.type(), ArgumentSpec.Kind.OPTIONAL_GREEDY);
        };
    }

    private static FlagSpec<?> exportOption(RegistryOptionSpec option) {
        FlagSpec<?> spec = switch (option.kind()) {
            case FLAG -> new FlagSpec<>(option.name(), Boolean.class, null, FlagSpec.Kind.FLAG);
            case VALUE -> new FlagSpec<>(option.name(), option.type(), null, FlagSpec.Kind.VALUE);
        };
        Optional<String> alias = option.aliasOptional();
        return alias.isPresent() ? spec.alias(alias.get()) : spec;
    }
}
