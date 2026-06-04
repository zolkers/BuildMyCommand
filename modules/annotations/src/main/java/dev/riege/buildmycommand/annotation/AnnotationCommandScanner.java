/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.annotation.binding.AnnotationCommandCompiler;
import dev.riege.buildmycommand.api.CommandRegistry;

import java.util.Objects;

public final class AnnotationCommandScanner {
    private AnnotationCommandScanner() {
    }

    public static void register(CommandRegistry registry, Object commands) {
        Objects.requireNonNull(registry, "registry");
        AnnotationCommandCompiler.compile(Objects.requireNonNull(commands, "commands"))
            .register(registry);
    }
}
