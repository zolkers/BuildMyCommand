/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.neoforge;

public enum NeoForgeCommandApi {
    NEOFORGE_MODERN("net.neoforged.neoforge.event.RegisterCommandsEvent", "NeoForge RegisterCommandsEvent");

    private final String eventClassName;
    private final String displayName;

    NeoForgeCommandApi(String eventClassName, String displayName) {
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
