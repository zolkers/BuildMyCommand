package dev.riege.buildmycommand.adapters;

import java.util.Objects;

public record AdapterConfig(
    String adapterId,
    String displayName,
    AdapterCapabilities capabilities
) {
    public AdapterConfig {
        adapterId = requireText(adapterId, "adapterId");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(capabilities, "capabilities");
    }

    public static AdapterConfig of(String adapterId, String displayName, AdapterCapabilities capabilities) {
        return new AdapterConfig(adapterId, displayName, capabilities);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
