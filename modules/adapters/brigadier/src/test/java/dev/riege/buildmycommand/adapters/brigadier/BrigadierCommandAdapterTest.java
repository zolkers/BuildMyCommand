package dev.riege.buildmycommand.adapters.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrigadierCommandAdapterTest {
    @Test
    void buildsBrigadierTreeFromFrameworkGraphAndDispatchesThroughCore() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);

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
        assertTrue(target.getChild("_bmc_input").getName().startsWith("_bmc_input"));
        assertEquals(1, result);
        assertEquals("Ada:true", executed.get());
    }

    @Test
    void registersRootAliasAsRedirectNode() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);

        framework.registry().command("ping", command -> command
            .alias("p")
            .executes(ctx -> Results.success("pong")));

        LiteralCommandNode<NativeSource> alias = bridge.roots().get(1);

        assertEquals("p", alias.getName());
        assertEquals("ping", alias.getRedirect().getName());
    }

    @Test
    void dispatcherExecutesNestedLiteralAliasesThroughFramework() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("user rank|roles set|put <target:String>")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class));
                return Results.success("ok");
            });
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(1, dispatcher.execute("user roles put Ada", new NativeSource()));
        assertEquals("Ada", executed.get());
    }

    @Test
    void exposesGenericAdapterSdkForRawBrigadierInput() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("pong")));

        CommandAdapter<NativeSource, String, Integer> adapter = bridge;
        CommandInput input = adapter.mapInput(new NativeSource(), "/ping");

        assertEquals("brigadier", adapter.config().adapterId());
        assertEquals("brigadier", adapter.runtime().platform().id());
        assertEquals("/ping", input.rawInput());
        assertEquals("ping", input.normalizedInput());
        assertEquals(1, adapter.execute(new NativeSource(), "/ping"));
    }

    @Test
    void rejectsNullMappedSource() {
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(
            CommandFramework.create(),
            source -> null
        );

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> bridge.mapSource(new NativeSource()));

        assertEquals("mapped source", exception.getMessage());
    }

    @Test
    void acceptsCustomPlatformAndAdapterConfig() {
        CommandPlatform platform = new CommandPlatform("minecraft", "Minecraft", false, true, true);
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(
            CommandFramework.create(),
            NativeSource::source,
            platform,
            new AdapterConfig("minecraft-brigadier", "Minecraft Brigadier", AdapterCapabilities.from(platform))
        );

        assertEquals("minecraft-brigadier", bridge.config().adapterId());
        assertEquals("minecraft", bridge.runtime().platform().id());
    }

    @Test
    void registersProjectedRootsIntoAnyBrigadierDispatcher() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.alias("p").executes(ctx -> Results.success("pong")));

        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(Set.of("ping", "p"), bridge.registration().register(dispatcher));
        assertNotNull(dispatcher.getRoot().getChild("ping"));
        assertNotNull(dispatcher.getRoot().getChild("p"));
        assertEquals("ping", dispatcher.getRoot().getChild("p").getRedirect().getName());
    }

    @Test
    void dispatcherHonorsFrameworkCaseInsensitiveLiteralsThroughFallbackTunnel() throws Exception {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        AtomicReference<String> executed = new AtomicReference<>();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("admin ban <target:String> [--silent|-s]")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class) + ":" + ctx.flag("silent"));
                return Results.success("ok");
            });
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertNotNull(dispatcher.getRoot().getChild("_bmc_input"));
        assertEquals(1, dispatcher.execute("ADMIN ban Ada --SILENT", new NativeSource()));
        assertEquals("Ada:true", executed.get());
        assertEquals(1, dispatcher.execute("admin BAN Ada -S", new NativeSource()));
        assertEquals("Ada:true", executed.get());
    }

    @Test
    void dispatcherFallbackTunnelDelegatesSuggestionsToFramework() throws Exception {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("admin ban <target:String>")
            .executes(ctx -> Results.success("ok"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(List.of("admin"), suggestions(dispatcher, "A"));
        assertEquals(List.of("ban"), suggestions(dispatcher, "admin B"));
    }

    @Test
    void dispatcherSuggestionsIncludeAliasesAndShortOptions() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("give|grant <target:String> [--silent|-s] [--amount:Integer|-a]")
            .executes(ctx -> Results.success("ok"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(List.of("give", "grant"), suggestions(dispatcher, "g"));
        assertEquals(Set.of("--silent", "-s", "--amount", "-a"),
            Set.copyOf(suggestions(dispatcher, "give Ada -")));
    }

    @Test
    void dispatcherDelegatesStrictFailuresToFramework() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("pong")));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertNotNull(dispatcher.getRoot().getChild("_bmc_input"));
        assertEquals(0, dispatcher.execute("PING", new NativeSource()));
    }

    @Test
    void dispatcherDelegatesDeniedInvalidArgumentInputToFrameworkPermissionCheck() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("user rank set <level:Integer>")
            .permission("rank.set")
            .executes(ctx -> Results.success("ok"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(0, dispatcher.execute("user rank set nope", new NativeSource(false)));
    }

    @Test
    void renderMapsOnlySuccessToBrigadierSuccessCode() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("quiet", command -> command.executes(ctx -> Results.silent()));

        assertEquals(0, bridge.execute(new NativeSource(), "missing"));
        assertEquals(0, bridge.execute(new NativeSource(), "quiet"));
    }

    private static LiteralCommandNode<NativeSource> literal(LiteralCommandNode<NativeSource> parent, String name) {
        return assertInstanceOf(LiteralCommandNode.class, parent.getChild(name));
    }

    private static List<String> suggestions(CommandDispatcher<NativeSource> dispatcher, String input) throws Exception {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, new NativeSource()))
            .get()
            .getList()
            .stream()
            .map(com.mojang.brigadier.suggestion.Suggestion::getText)
            .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CommandContext<NativeSource> brigadierContext(NativeSource source, String input) {
        return new CommandContext(source, input, Map.of(), null, null, List.of(),
            StringRange.at(0), null, null, false);
    }

    private record NativeSource(boolean allowed) {
        private NativeSource() {
            this(true);
        }

        CommandSource source() {
            return new CommandSource() {
                @Override
                public boolean hasPermission(String permission) {
                    return allowed;
                }
            };
        }
    }
}
