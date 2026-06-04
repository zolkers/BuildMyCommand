/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a route DSL path below a class-level {@link Command} root.
 *
 * <p>This is the preferred style for large command trees. The class names the root
 * command once, and each method describes its executable branch with route DSL. This
 * avoids deeply nested {@link Subcommand} classes while still supporting arbitrarily
 * deep command paths.</p>
 *
 * <pre>{@code
 * @Command("admin")
 * static final class AdminCommands {
 *     @SubRoute("moderation|mod punish temporary|temp add <target:String>")
 *     CommandResult add(@RouteCtx CommandContext route) {
 *         return Results.success(route.arg("target", String.class));
 *     }
 * }
 * }</pre>
 *
 * <p>{@code @SubRoute} values are relative to the nearest {@link Command} class.
 * Do not repeat the root literal in the value unless you intentionally want that
 * literal to appear twice in the command path.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubRoute {
    /**
     * A relative route DSL string below the owning {@link Command} root.
     */
    String value();
}
