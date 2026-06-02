package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandSource;

@FunctionalInterface
public interface AdapterSourceMapper<S> {
    CommandSource map(S source);
}
