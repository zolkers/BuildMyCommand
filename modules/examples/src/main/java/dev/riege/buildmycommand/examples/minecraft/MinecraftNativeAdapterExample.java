package dev.riege.buildmycommand.examples.minecraft;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Set;

public final class MinecraftNativeAdapterExample {
    private MinecraftNativeAdapterExample() {
    }

    public static MinecraftRenderedResult execute() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("home|h set <name:String>")
            .permission("home.set")
            .executes(ctx -> Results.success("Home set: " + ctx.arg("name", String.class)));
        MinecraftNativeCommandAdapter<FakeSender> adapter = new MinecraftNativeCommandAdapter<>(
            framework,
            sender -> new CommandSource() {
                @Override
                public boolean hasPermission(String permission) {
                    return sender.permissions().contains(permission);
                }
            }
        );
        return adapter.execute(
            new FakeSender(Set.of("home.set")),
            MinecraftInvocation.labelAndArgs("h", new String[] {"set", "base"}, 1)
        );
    }

    public record FakeSender(Set<String> permissions) {
    }
}
