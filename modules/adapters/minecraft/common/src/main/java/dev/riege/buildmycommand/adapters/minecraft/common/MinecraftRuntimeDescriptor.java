package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Objects;
import java.util.Set;

public record MinecraftRuntimeDescriptor(
    String loader,
    String minecraftVersion,
    String apiVersion,
    Set<MinecraftCapability> capabilities
) {
    public MinecraftRuntimeDescriptor {
        loader = requireText(loader, "loader");
        minecraftVersion = requireText(minecraftVersion, "minecraftVersion");
        apiVersion = requireText(apiVersion, "apiVersion");
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
    }

    public boolean supports(MinecraftCapability capability) {
        return capabilities.contains(Objects.requireNonNull(capability, "capability"));
    }

    public String displayName() {
        return loader + " " + minecraftVersion + " (api " + apiVersion + ")";
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
