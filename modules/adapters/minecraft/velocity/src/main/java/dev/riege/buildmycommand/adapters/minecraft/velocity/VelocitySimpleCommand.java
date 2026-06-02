package dev.riege.buildmycommand.adapters.minecraft.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class VelocitySimpleCommand implements SimpleCommand {
    private final MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter;

    public VelocitySimpleCommand(
        MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter
    ) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter() {
        return adapter;
    }

    @Override
    public void execute(Invocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        MinecraftRenderedResult result = adapter.execute(
            invocation.source(),
            VelocityMinecraftAdapter.simpleCommandInput(invocation.alias(), invocation.arguments())
        );
        result.message().ifPresent(message -> invocation.source().sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return adapter.suggest(
            invocation.source(),
            VelocityMinecraftAdapter.simpleCommandInput(invocation.alias(), invocation.arguments())
        );
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return adapter.canUseRootLabel(invocation.source(), invocation.alias());
    }
}
