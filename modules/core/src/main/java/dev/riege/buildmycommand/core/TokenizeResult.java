package dev.riege.buildmycommand.core;

import java.util.List;
import java.util.Optional;

record TokenizeResult(List<String> tokens, Optional<String> failure) {
    static TokenizeResult success(List<String> tokens) {
        return new TokenizeResult(List.copyOf(tokens), Optional.empty());
    }

    static TokenizeResult failure(String failure) {
        return new TokenizeResult(List.of(), Optional.of(failure));
    }
}
