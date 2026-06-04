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
 * Controls whether aliases are offered in completions.
 *
 * <p>Aliases always parse when they are declared. This annotation only controls
 * suggestion visibility. It is useful when an alias is a convenience shortcut, but
 * the public UX should promote the canonical literal.</p>
 *
 * <pre>{@code
 * @SuggestAliases(false)
 * @SubRoute("bang|b <target:String>")
 * CommandResult bang(@RouteCtx CommandContext route) {
 *     return Results.success("ok");
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SuggestAliases {
    /**
     * {@code true} to suggest aliases, {@code false} to parse aliases silently.
     */
    boolean value() default true;
}
