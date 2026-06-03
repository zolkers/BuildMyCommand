package dev.riege.buildmycommand.adapters.minecraft.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftAdapterContracts;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class VelocitySimpleCommand implements SimpleCommand {
    private final IAdapter<com.velocitypowered.api.command.CommandSource, MinecraftInvocation, MinecraftRenderedResult> adapter;

    public VelocitySimpleCommand(
        IAdapter<com.velocitypowered.api.command.CommandSource, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public IAdapter<com.velocitypowered.api.command.CommandSource, MinecraftInvocation, MinecraftRenderedResult> adapter() {
        return adapter;
    }

    @Override
    public void execute(Invocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        MinecraftRenderedResult result = adapter.execute(
            invocation.source(),
            VelocityMinecraftIntegration.simpleCommandInput(invocation.alias(), invocation.arguments())
        );
        result.message().ifPresent(message -> invocation.source().sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        MinecraftInvocation input = VelocityMinecraftIntegration.simpleCommandInput(invocation.alias(), invocation.arguments());
        return adapter.suggest(
            invocation.source(),
            input,
            input.cursor()
        );
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return MinecraftAdapterContracts.canUseRootLabel(adapter, invocation.source(), invocation.alias());
    }
}
