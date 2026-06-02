package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.api.CommandResult;

@FunctionalInterface
public interface MinecraftResultRenderer {
    MinecraftRenderedResult render(CommandResult result);

    static MinecraftResultRenderer defaultRenderer() {
        return result -> switch (result.status()) {
            case SUCCESS -> MinecraftRenderedResult.of(1, result.reply().orElse(null));
            case FAILURE -> MinecraftRenderedResult.of(0, result.reply().orElse(null));
            case SILENT -> MinecraftRenderedResult.of(0, null);
        };
    }
}
