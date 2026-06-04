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
 * Changes matching policy for literals and/or options below the annotated command
 * declaration.
 *
 * <p>Case-insensitive literals let users type command words with different casing.
 * Case-insensitive options let users type flags and options such as
 * {@code --Silent} when the route defines {@code --silent}. The actual canonical
 * names exposed through help and command context remain the names declared in the
 * route or builder.</p>
 *
 * <p>Apply this on a {@link Command} class for a whole tree, or on a route method
 * when only one command needs relaxed matching. Platform adapters should preserve
 * this policy even when the underlying command engine has stricter literal matching.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @Command("wecc")
 * @CaseInsensitive(literals = true, options = true)
 * final class WeccCommands {
 *     @SubRoute("bang <target:String> [--silent|-s]")
 *     CommandResult bang(@RouteCtx CommandContext route) {
 *         return Results.success(route.arg("target", String.class));
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CaseInsensitive {
    /**
     * Whether command literals and literal aliases should match ignoring case.
     */
    boolean literals() default true;

    /**
     * Whether long option names and short option aliases should match ignoring case.
     */
    boolean options() default true;
}
