/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;

@FunctionalInterface
public interface AdapterInputMapper<S, I> {
    CommandInput map(S source, I input, AdapterRuntime runtime, AdapterSourceMapper<S> sourceMapper);
}
