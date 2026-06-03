package dev.riege.buildmycommand.adapters.minecraft.minestom;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftAdapterContracts;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;

import java.util.List;
import java.util.Objects;

public final class MinestomCommandRegistration {
    private static final String MATCHING_NOTICE =
        "Minestom command names and aliases are registered through Command(name, aliases...) and are exact-case.";

    private final MinecraftBackendProfile profile;
    private final IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter;
    private MinestomNativeCommand registeredCommand;

    public MinestomCommandRegistration(
        MinecraftBackendProfile profile,
        IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter() {
        return adapter;
    }

    public List<String> labels() {
        return MinecraftAdapterContracts.rootLabels(adapter);
    }

    public MinestomNativeCommand command() {
        if (registeredCommand == null) {
            registeredCommand = MinestomMinecraftAdapter.nativeCommand(adapter);
        }
        return registeredCommand;
    }

    public MinestomNativeCommand register(MinestomCommandRegistrar registrar) {
        Objects.requireNonNull(registrar, "registrar").register(command());
        return registeredCommand;
    }

    public MinestomCommandRegistration unregister(MinestomCommandRegistrar registrar) {
        Objects.requireNonNull(registrar, "registrar").unregister(command());
        return this;
    }

    public MinecraftCommandRegistrationPlan plan() {
        return new MinecraftCommandRegistrationPlan(profile, labels(), 1, true);
    }

    public boolean exactLiteralMatching() {
        return true;
    }

    public String matchingNotice() {
        return MATCHING_NOTICE;
    }
}
