package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class MinecraftNativeCommandAdapter<S> {
    private final MinecraftCommandBridge<S> bridge;
    private final MinecraftResultRenderer resultRenderer;

    public MinecraftNativeCommandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        this(new MinecraftCommandBridge<>(framework, sourceMapper), MinecraftResultRenderer.defaultRenderer());
    }

    public MinecraftNativeCommandAdapter(
        MinecraftCommandBridge<S> bridge,
        MinecraftResultRenderer resultRenderer
    ) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.resultRenderer = Objects.requireNonNull(resultRenderer, "resultRenderer");
    }

    public MinecraftAdapterMode mode() {
        return MinecraftAdapterMode.NATIVE_COMMAND;
    }

    public List<String> registrationLabels() {
        return bridge.rootLabels();
    }

    public MinecraftRenderedResult execute(S source, MinecraftInvocation invocation) {
        CommandResult result = bridge.dispatch(source, invocation);
        return resultRenderer.render(result);
    }

    public List<String> suggest(S source, MinecraftInvocation invocation) {
        return bridge.suggest(source, invocation);
    }

    public boolean honorsCaseInsensitiveLiterals() {
        return bridge.caseInsensitiveLiterals();
    }

    public boolean honorsCaseInsensitiveOptions() {
        return bridge.caseInsensitiveOptions();
    }
}
