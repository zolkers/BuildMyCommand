/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.adapters;

import dev.riege.buildmycommand.adapters.terminal.TerminalAdapter;
import dev.riege.buildmycommand.examples.basics.BuilderCommandsExample;

import java.io.InputStream;
import java.io.PrintStream;

public final class TerminalAdapterExample {
    private TerminalAdapterExample() {
    }

    public static TerminalAdapter attach(InputStream input, PrintStream output) {
        return TerminalAdapter.attach(BuilderCommandsExample.create())
            .input(input)
            .output(output);
    }
}
