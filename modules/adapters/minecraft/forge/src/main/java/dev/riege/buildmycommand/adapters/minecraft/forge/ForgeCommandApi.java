/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.forge;

public enum ForgeCommandApi {
    FORGE_1_16_5("net.minecraftforge.event.RegisterCommandsEvent", "Forge 1.16.5 RegisterCommandsEvent"),
    FORGE_MODERN("net.minecraftforge.event.RegisterCommandsEvent", "Modern Forge RegisterCommandsEvent");

    private final String eventClassName;
    private final String displayName;

    ForgeCommandApi(String eventClassName, String displayName) {
        this.eventClassName = eventClassName;
        this.displayName = displayName;
    }

    public String eventClassName() {
        return eventClassName;
    }

    public String displayName() {
        return displayName;
    }
}
