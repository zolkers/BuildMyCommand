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
 * Requires one permission node before a command can execute.
 *
 * <p>{@code @Permission} is exact by default: it represents a single permission
 * string such as {@code "admin.reload"} or {@code "team.member.grant"}. Set
 * {@link #regex()} to {@code true} when the value is a Java regular expression
 * that should match one permission owned by the command source. The annotation is
 * inherited through the command path according to the scanner/compiler rules:
 * group-level permissions can protect a whole subtree, while route-level permissions
 * protect one executable leaf.</p>
 *
 * <p>Do not put boolean expressions here. For expressions such as
 * {@code "staff || owner"} or {@code "staff && !banned"}, use {@link Require}
 * instead. The IntelliJ plugin intentionally warns when {@code @Permission} looks
 * like a boolean requirement.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @SubRoute("reload")
 * @Permission("admin.reload")
 * CommandResult reload(@RouteCtx CommandContext route) {
 *     return Results.success("reloaded");
 * }
 *
 * @SubRoute("audit")
 * @Permission(value = "admin\\.audit\\..*", regex = true)
 * CommandResult audit(@RouteCtx CommandContext route) {
 *     return Results.success("audit");
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {
    /**
     * A single permission node, or a Java regular expression when {@link #regex()}
     * is enabled.
     */
    String value();

    /**
     * Whether {@link #value()} is interpreted as a Java regular expression.
     */
    boolean regex() default false;
}
