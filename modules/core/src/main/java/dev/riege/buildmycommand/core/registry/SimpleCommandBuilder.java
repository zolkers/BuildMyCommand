package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class SimpleCommandBuilder implements CommandRegistry.CommandBuilder {
    private final String literal;
    private final CommandMatchingPolicy matchingPolicy;
    private String description;
    private String permission;
    private final List<String> aliases = new ArrayList<>();
    private final List<RegistryArgumentSpec> arguments = new ArrayList<>();
    private final List<RegistryOptionSpec> options = new ArrayList<>();
    private final Map<String, RegistryCommandNode> children = new LinkedHashMap<>();
    private CommandRegistry.CommandExecutor executor = SimpleCommandRegistry.DEFAULT_EXECUTOR;

    public SimpleCommandBuilder(String literal) {
        this(literal, CommandMatchingPolicy.strict());
    }

    SimpleCommandBuilder(String literal, CommandMatchingPolicy matchingPolicy) {
        this.literal = Validators.literal(literal, "literal");
        this.matchingPolicy = Objects.requireNonNull(matchingPolicy, "matchingPolicy");
    }

    @Override
    public CommandRegistry.CommandBuilder description(String description) {
        this.description = Validators.metadata(description, "description");
        return this;
    }

    @Override
    public CommandRegistry.CommandBuilder permission(String permission) {
        this.permission = Validators.metadata(permission, "permission");
        return this;
    }

    @Override
    public CommandRegistry.CommandBuilder alias(String alias) {
        String validatedAlias = Validators.literal(alias, "alias");
        if (validatedAlias.equals(literal) || aliases.contains(validatedAlias)) {
            throw new IllegalArgumentException("alias already registered: " + validatedAlias);
        }
        aliases.add(validatedAlias);
        return this;
    }

    @Override
    public CommandRegistry.CommandBuilder aliases(String... aliases) {
        Objects.requireNonNull(aliases, "aliases");
        for (String alias : aliases) {
            alias(alias);
        }
        return this;
    }

    @Override
    public CommandRegistry.CommandBuilder subcommand(String literal, Consumer<CommandRegistry.CommandBuilder> configure) {
        String validatedLiteral = Validators.literal(literal, "literal");
        Objects.requireNonNull(configure, "configure");

        SimpleCommandBuilder builder = new SimpleCommandBuilder(validatedLiteral, matchingPolicy);
        configure.accept(builder);
        RegistryCommandNode child = builder.node();
        RegistryNodeMerger.registerAll(children, child.literals(), child, "subcommand already registered: ",
            matchingPolicy);
        return this;
    }

    @Override
    public <T> CommandRegistry.CommandBuilder argument(String name, Class<T> type) {
        validateCanAdd(name, RegistryArgumentKind.REQUIRED);
        arguments.add(new RegistryArgumentSpec(name, type, RegistryArgumentKind.REQUIRED));
        return this;
    }

    @Override
    public <T> CommandRegistry.CommandBuilder optionalArgument(String name, Class<T> type) {
        validateCanAdd(name, RegistryArgumentKind.OPTIONAL);
        arguments.add(new RegistryArgumentSpec(name, type, RegistryArgumentKind.OPTIONAL));
        return this;
    }

    @Override
    public <T> CommandRegistry.CommandBuilder greedyArgument(String name, Class<T> type) {
        validateCanAdd(name, RegistryArgumentKind.GREEDY);
        arguments.add(new RegistryArgumentSpec(name, type, RegistryArgumentKind.GREEDY));
        return this;
    }

    @Override
    public <T> CommandRegistry.CommandBuilder optionalGreedyArgument(String name, Class<T> type) {
        validateCanAdd(name, RegistryArgumentKind.OPTIONAL_GREEDY);
        arguments.add(new RegistryArgumentSpec(name, type, RegistryArgumentKind.OPTIONAL_GREEDY));
        return this;
    }

    @Override
    public CommandRegistry.CommandBuilder flag(String name) {
        return flag(name, null);
    }

    @Override
    public CommandRegistry.CommandBuilder flag(String name, String alias) {
        options.add(new RegistryOptionSpec(name, Boolean.class, alias, RegistryOptionKind.FLAG));
        validateOptionNames();
        return this;
    }

    @Override
    public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type) {
        return option(name, type, null);
    }

    @Override
    public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type, String alias) {
        options.add(new RegistryOptionSpec(name, type, alias, RegistryOptionKind.VALUE));
        validateOptionNames();
        return this;
    }

    @Override
    public CommandRegistry.CommandBuilder executes(CommandRegistry.CommandExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
        return this;
    }

    RegistryCommandNode node() {
        return new RegistryCommandNode(literal, description, permission, aliases, executor, arguments, options, children);
    }

    private void validateCanAdd(String nextName, RegistryArgumentKind nextKind) {
        if (arguments.stream().anyMatch(argument -> argument.name().equals(nextName))) {
            throw new IllegalStateException("argument already declared: " + nextName);
        }
        if (options.stream().anyMatch(option -> option.name().equals(nextName))) {
            throw new IllegalStateException("argument conflicts with flag or option: " + nextName);
        }

        boolean hasOptional = arguments.stream()
            .anyMatch(argument -> argument.kind() == RegistryArgumentKind.OPTIONAL
                || argument.kind() == RegistryArgumentKind.OPTIONAL_GREEDY);
        if (nextKind == RegistryArgumentKind.REQUIRED && hasOptional) {
            throw new IllegalStateException("required arguments must be declared before optional arguments");
        }

        boolean hasGreedy = arguments.stream()
            .anyMatch(argument -> argument.kind() == RegistryArgumentKind.GREEDY
                || argument.kind() == RegistryArgumentKind.OPTIONAL_GREEDY);
        if (hasGreedy) {
            throw new IllegalStateException("greedy argument must be the last argument");
        }
    }

    private void validateOptionNames() {
        List<String> seenNames = new ArrayList<>();
        List<String> seenAliases = new ArrayList<>();
        for (RegistryOptionSpec option : options) {
            if (arguments.stream().anyMatch(argument -> argument.name().equals(option.name()))) {
                throw new IllegalStateException("flag or option conflicts with argument: " + option.name());
            }
            if (seenNames.contains(option.name())) {
                throw new IllegalStateException("flag or option already declared: " + option.name());
            }
            seenNames.add(option.name());
            option.aliasOptional().ifPresent(alias -> {
                if (seenAliases.contains(alias)) {
                    throw new IllegalStateException("flag or option alias already declared: " + alias);
                }
                seenAliases.add(alias);
            });
        }
    }

}
