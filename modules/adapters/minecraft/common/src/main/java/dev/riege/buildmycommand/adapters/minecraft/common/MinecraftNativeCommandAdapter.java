package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterMatchingPolicy;
import dev.riege.buildmycommand.adapters.AdapterRegistrationLabels;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class MinecraftNativeCommandAdapter<S>
    implements CommandAdapter<S, MinecraftInvocation, MinecraftRenderedResult> {
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

    public List<String> rootLabels() {
        return MinecraftAdapterContracts.rootLabels(this);
    }

    @Override
    public AdapterRegistrationLabels registrationLabels() {
        return bridge.registrationLabels();
    }

    @Override
    public AdapterCapabilities capabilities() {
        return config.capabilities();
    }

    @Override
    public AdapterRuntime runtime() {
        return bridge.runtime();
    }

    @Override
    public AdapterConfig config() {
        return config;
    }

    @Override
    public AdapterRenderer<MinecraftRenderedResult> renderer() {
        return resultRenderer::render;
    }

    @Override
    public CommandSource mapSource(S source) {
        return bridge.mapSource(source);
    }

    @Override
    public CommandInput mapInput(S source, MinecraftInvocation invocation) {
        return bridge.mapInput(source, invocation);
    }

    @Override
    public CommandResult dispatch(S source, MinecraftInvocation invocation) {
        return bridge.dispatch(source, invocation);
    }

    @Override
    public AdapterMatchingPolicy matchingPolicy() {
        return bridge.matchingPolicy();
    }

    @Override
    public MinecraftRenderedResult render(CommandResult result) {
        return renderer().render(result);
    }

    @Override
    public MinecraftRenderedResult execute(S source, MinecraftInvocation invocation) {
        return CommandAdapter.super.execute(source, invocation);
    }

    @Override
    public List<Suggestion> suggestRich(S source, MinecraftInvocation invocation, int cursor) {
        return bridge.suggestRich(source, invocation, cursor);
    }

    public List<String> suggest(S source, MinecraftInvocation invocation) {
        return suggest(source, invocation, invocation.cursor());
    }

    @Override
    public List<String> suggest(S source, MinecraftInvocation invocation, int cursor) {
        return bridge.suggest(source, invocation, cursor);
    }

    public boolean canUseRootLabel(S source, String label) {
        return MinecraftAdapterContracts.canUseRootLabel(this, source, label);
    }

    public boolean honorsCaseInsensitiveLiterals() {
        return bridge.caseInsensitiveLiterals();
    }

    public boolean honorsCaseInsensitiveOptions() {
        return bridge.caseInsensitiveOptions();
    }
}
