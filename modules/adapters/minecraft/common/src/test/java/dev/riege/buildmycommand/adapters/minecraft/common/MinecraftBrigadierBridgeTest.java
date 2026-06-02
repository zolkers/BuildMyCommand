package dev.riege.buildmycommand.adapters.minecraft.common;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftBrigadierBridgeTest {
    @Test
    void buildsBrigadierTreeFromFrameworkGraphAndDispatchesThroughCore() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        MinecraftBrigadierBridge<NativeSource> bridge = MinecraftBrigadierBridge.create(framework, NativeSource::source);

        framework.registry()
            .route("user rank set <target:String> [--silent|-s]")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class) + ":" + ctx.flag("silent"));
                return Results.success("ok");
            });

        LiteralCommandNode<NativeSource> root = bridge.roots().get(0);
        LiteralCommandNode<NativeSource> set = literal(literal(root, "rank"), "set");
        ArgumentCommandNode<NativeSource, ?> target =
            assertInstanceOf(ArgumentCommandNode.class, set.getChild("target"));

        int result = target.getCommand().run(brigadierContext(new NativeSource(), "user rank set Ada --silent"));

        assertEquals("user", root.getName());
        assertNotNull(target.getCustomSuggestions());
        assertTrue(target.getChild("_bmc_flags").getName().startsWith("_bmc_flags"));
        assertEquals(1, result);
        assertEquals("Ada:true", executed.get());
    }

    @Test
    void registersRootAliasAsRedirectNode() {
        CommandFramework framework = CommandFramework.create();
        MinecraftBrigadierBridge<NativeSource> bridge = MinecraftBrigadierBridge.create(framework, NativeSource::source);

        framework.registry().command("ping", command -> command
            .alias("p")
            .executes(ctx -> Results.success("pong")));

        LiteralCommandNode<NativeSource> alias = bridge.roots().get(1);

        assertEquals("p", alias.getName());
        assertEquals("ping", alias.getRedirect().getName());
    }

    @Test
    void exposesGenericAdapterSdkForRawBrigadierInput() {
        CommandFramework framework = CommandFramework.create();
        MinecraftBrigadierBridge<NativeSource> bridge = MinecraftBrigadierBridge.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("pong")));

        CommandAdapter<NativeSource, String, Integer> adapter = bridge;
        CommandInput input = adapter.mapInput(new NativeSource(), "/ping");

        assertEquals("minecraft-brigadier", adapter.config().adapterId());
        assertEquals("minecraft", adapter.runtime().platform().id());
        assertEquals("/ping", input.rawInput());
        assertEquals("ping", input.normalizedInput());
        assertEquals(1, adapter.execute(new NativeSource(), "/ping"));
    }

    private static LiteralCommandNode<NativeSource> literal(LiteralCommandNode<NativeSource> parent, String name) {
        return assertInstanceOf(LiteralCommandNode.class, parent.getChild(name));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CommandContext<NativeSource> brigadierContext(NativeSource source, String input) {
        return new CommandContext(source, input, java.util.Map.of(), null, null, java.util.List.of(),
            StringRange.at(0), null, null, false);
    }

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
