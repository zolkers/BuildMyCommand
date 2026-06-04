/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.CommandMiddleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attaches command middleware classes to a command group or route.
 *
 * <p>Middleware is executed around the command executor and can enforce policy,
 * transform replies, add logging, measure timings, or delegate to another platform
 * service. Type-level middleware applies to the annotated command group; method-level
 * middleware applies to that executable command. When several middleware entries are
 * present along a command path, the compiled command keeps their declared order from
 * group to leaf.</p>
 *
 * <pre>{@code
 * @Middleware(StaffOnlyMiddleware.class)
 * @SubRoute("punish <target:String>")
 * CommandResult punish(@RouteCtx CommandContext route) {
 *     return Results.success("punished");
 * }
 * }</pre>
 *
 * <p>Use {@link Require} for simple permission expressions. Use middleware when the
 * check needs custom Java logic, async/platform state, side effects, or reply
 * wrapping.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Middleware {
    /**
     * Middleware classes instantiated by the annotation scanner/compiler.
     */
    Class<? extends CommandMiddleware>[] value();
}
