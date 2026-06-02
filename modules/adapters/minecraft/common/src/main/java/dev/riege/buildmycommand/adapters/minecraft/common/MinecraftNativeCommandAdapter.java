package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterRegistrationLabels;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class MinecraftNativeCommandAdapter<S> {
    private final MinecraftCommandBridge<S> bridge;
    private final MinecraftResultRenderer resultRenderer;
    private final AdapterConfig config;

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
        this.config = new AdapterConfig(
            "minecraft-native",
            "Minecraft Native Command",
            AdapterCapabilities.from(bridge.runtime().platform())
        );
    }

    public MinecraftAdapterMode mode() {
        return MinecraftAdapterMode.NATIVE_COMMAND;
    }

    public List<String> registrationLabels() {
        return bridge.rootLabels();
    }

    public AdapterRegistrationLabels adapterRegistrationLabels() {
        return bridge.registrationLabels();
    }

    public AdapterCapabilities capabilities() {
        return config.capabilities();
    }

    public AdapterRuntime runtime() {
        return bridge.runtime();
    }

    public AdapterConfig config() {
        return config;
    }

    public AdapterRenderer<MinecraftRenderedResult> renderer() {
        return resultRenderer::render;
    }

    public CommandSource mapSource(S source) {
        return bridge.mapSource(source);
    }

    public CommandInput mapInput(S source, MinecraftInvocation invocation) {
        return bridge.mapInput(source, invocation);
    }

    public MinecraftRenderedResult render(CommandResult result) {
        return renderer().render(result);
    }

    public MinecraftRenderedResult execute(S source, MinecraftInvocation invocation) {
        CommandResult result = bridge.dispatch(source, invocation);
        return render(result);
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
