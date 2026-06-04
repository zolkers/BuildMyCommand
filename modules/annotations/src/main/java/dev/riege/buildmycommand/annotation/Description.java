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
 * Human-readable help text for a command node.
 *
 * <p>On {@link Route} and {@link SubRoute} methods, the description is attached to
 * the executable route leaf. On {@link Command} or {@link Subcommand} classes, it is
 * attached to that command group and appears in help output for the group.</p>
 *
 * <p>Keep descriptions operational and short. Use {@link Usage} and {@link Example}
 * for syntax and concrete samples instead of packing everything into this string.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @SubRoute("ping")
 * @Description("Checks whether the command bridge is alive.")
 * CommandResult ping(@RouteCtx CommandContext route) {
 *     return Results.success("pong");
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Description {
    /**
     * Help text shown for the annotated command or group.
     */
    String value();
}
