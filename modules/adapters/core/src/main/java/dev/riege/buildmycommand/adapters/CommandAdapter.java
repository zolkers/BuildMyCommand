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
import java.util.Objects;

public interface CommandAdapter<S, I, R> extends IAdapter<S, I, R> {
    @Override
    default AdapterCapabilities capabilities() {
        return config().capabilities();
    }

    @Override
    default AdapterMatchingPolicy matchingPolicy() {
        return new AdapterMatchingPolicy(
            runtime().framework().caseInsensitiveLiterals(),
            runtime().framework().caseInsensitiveOptions(),
            true
        );
    }

    @Override
    default AdapterRegistrationLabels registrationLabels() {
        return runtime().registrationLabels();
    }

    @Override
    default CommandResult dispatch(S nativeSource, I nativeInput) {
        return runtime().dispatch(mapInput(nativeSource, nativeInput));
    }

    @Override
    default List<Suggestion> suggestRich(S nativeSource, I nativeInput, int cursor) {
        CommandInput input = mapInput(nativeSource, nativeInput);
        CommandInput suggestionInput = new CommandInput(
            input.source(),
            input.rawInput(),
            input.normalizedInput(),
            cursor,
            input.prefix(),
            input.platform()
        );
        return runtime().framework().suggestRich(suggestionInput);
    }

    @Override
    default List<String> suggest(S nativeSource, I nativeInput, int cursor) {
        return suggestRich(nativeSource, nativeInput, cursor).stream()
            .map(Suggestion::value)
            .toList();
    }

    @Override
    default R render(CommandResult result) {
        Objects.requireNonNull(result, "result");
        return renderer().render(result);
    }

    @Override
    default R execute(S nativeSource, I nativeInput) {
        return render(dispatch(nativeSource, nativeInput));
    }
}
