/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.sponge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;

import java.util.List;
import java.util.Objects;

public final class SpongeCommandRegistration<C> {
    private static final String MATCHING_NOTICE =
        "Sponge RegisterCommandEvent aliases are registration aliases; Sponge and Brigadier literals remain exact-case.";

    private final MinecraftBackendProfile profile;
    private final Object pluginContainer;
    private final C command;
    private final List<String> labels;

    public SpongeCommandRegistration(
        MinecraftBackendProfile profile,
        Object pluginContainer,
        C command,
        List<String> labels
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.pluginContainer = Objects.requireNonNull(pluginContainer, "pluginContainer");
        this.command = Objects.requireNonNull(command, "command");
        this.labels = List.copyOf(Objects.requireNonNull(labels, "labels"));
        if (this.labels.isEmpty()) {
            throw new IllegalArgumentException("labels must not be empty");
        }
    }

    public List<String> labels() {
        return labels;
    }

    public C command() {
        return command;
    }

    public Object pluginContainer() {
        return pluginContainer;
    }

    public void register(SpongeCommandRegistrar<C> registrar) {
        Objects.requireNonNull(registrar, "registrar").register(
            pluginContainer,
            command,
            labels.get(0),
            labels.subList(1, labels.size()).toArray(String[]::new)
        );
    }

    public MinecraftCommandRegistrationPlan plan() {
        return new MinecraftCommandRegistrationPlan(profile, labels, 1, true);
    }

    public boolean exactLiteralMatching() {
        return true;
    }

    public String matchingNotice() {
        return MATCHING_NOTICE;
    }
}
