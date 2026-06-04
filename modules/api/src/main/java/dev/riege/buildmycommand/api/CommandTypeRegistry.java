/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

/**
 * Registry for user-defined route DSL type aliases.
 *
 * <p>Use this when a command route should expose a short, readable type name while parsing
 * into an application-specific Java type.</p>
 *
 * <pre>{@code
 * CommandFramework framework = CommandFramework.builder()
 *     .types(types -> types.register("Material", Material.class, new MaterialParser()))
 *     .build();
 *
 * @SubRoute("give <item:Material>")
 * CommandResult give(@RouteCtx CommandContext ctx) {
 *     Material item = ctx.arg("item", Material.class);
 *     return Results.success("giving " + item);
 * }
 * }</pre>
 */
public interface CommandTypeRegistry {
    /**
     * Registers a route DSL alias, runtime type, and parser.
     *
     * <p>The alias is the token used after {@code :} in route DSL arguments and options. For
     * example, registering {@code "Material"} allows {@code <item:Material>}.</p>
     *
     * @param alias route DSL type alias
     * @param type runtime Java type returned by {@link CommandContext#arg(String, Class)}
     * @param parser parser used to convert raw command input into {@code type}
     * @param <T> runtime value type
     * @return this registry
     * @throws IllegalArgumentException when the alias or type is already registered
     */
    <T> CommandTypeRegistry register(String alias, Class<T> type, ArgumentParser<? extends T> parser);
}
