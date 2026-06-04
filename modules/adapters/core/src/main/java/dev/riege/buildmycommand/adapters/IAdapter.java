/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;

import java.util.List;

public interface IAdapter<S, I, R> {
    AdapterRuntime runtime();

    AdapterConfig config();

    AdapterCapabilities capabilities();

    AdapterMatchingPolicy matchingPolicy();

    AdapterRegistrationLabels registrationLabels();

    AdapterRenderer<R> renderer();

    CommandSource mapSource(S nativeSource);

    CommandInput mapInput(S nativeSource, I nativeInput);

    CommandResult dispatch(S nativeSource, I nativeInput);

    List<Suggestion> suggestRich(S nativeSource, I nativeInput, int cursor);

    List<String> suggest(S nativeSource, I nativeInput, int cursor);

    R render(CommandResult result);

    R execute(S nativeSource, I nativeInput);
}
