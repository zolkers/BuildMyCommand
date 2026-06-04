/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierRegistration;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;

import java.util.Objects;

public record FabricBrigadierRegistration<N>(
    MinecraftBackendProfile backend,
    FabricCommandApi api,
    BrigadierCommandAdapter<N> bridge
) {
    public FabricBrigadierRegistration {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(bridge, "bridge");
    }

    public BrigadierRegistration<N> registerInto(CommandDispatcher<N> dispatcher) {
        BrigadierRegistration<N> registration = bridge.registration();
        registration.register(dispatcher);
        return registration;
    }

    public MinecraftCommandRegistrationPlan plan() {
        return MinecraftCommandRegistrationPlans.from(backend, bridge);
    }

    public boolean legacyApi() {
        return api == FabricCommandApi.COMMAND_API_V1;
    }
}
