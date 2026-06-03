package dev.riege.buildmycommand.adapters.minecraft.fabric;

public enum FabricCommandApi {
    COMMAND_API_V1("net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback", "Minecraft 1.16.5 style"),
    COMMAND_API_V2("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback", "Modern Fabric style");

    private final String callbackClassName;
    private final String displayName;

    FabricCommandApi(String callbackClassName, String displayName) {
        this.callbackClassName = callbackClassName;
        this.displayName = displayName;
    }

    public String callbackClassName() {
        return callbackClassName;
    }

    public String displayName() {
        return displayName;
    }
}
