/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandSource;

@FunctionalInterface
public interface AdapterSourceMapper<S> {
    CommandSource map(S source);
}
