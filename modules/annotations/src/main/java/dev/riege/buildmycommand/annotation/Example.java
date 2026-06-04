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
 * Adds concrete example inputs to command help metadata.
 *
 * <p>Examples should be copy-pasteable user input, usually including the platform
 * prefix when the target platform has one. Multiple examples can be declared in one
 * annotation value.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @SubRoute("punish <target:String> <reason:String...>")
 * @Example({"/mod punish Ada spam", "/mod punish Linus griefing"})
 * CommandResult punish(@RouteCtx CommandContext route) {
 *     return Results.success("punished");
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Example {
    /**
     * One or more example command inputs.
     */
    String[] value();
}
