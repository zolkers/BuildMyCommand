package dev.riege.buildmycommand.core.registry;

import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandLifecycleListener;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.FlagSpec;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.riege.buildmycommand.core.requirement.RequirementExpression;
import dev.riege.buildmycommand.core.route.ArgumentToken;
import dev.riege.buildmycommand.core.route.RoutePatternParser;
import dev.riege.buildmycommand.core.support.Validators;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryModelCoverageTest {
    @Test
    void matchingPolicySupportsStrictAndCaseInsensitiveKeys() {
        CommandMatchingPolicy policy = CommandMatchingPolicy.strict();

        assertFalse(policy.caseInsensitiveLiterals());
        assertFalse(policy.caseInsensitiveOptions());
        assertEquals("Warp", policy.literalKey("Warp"));
        assertTrue(policy.optionEquals("--Silent", "--Silent"));
        assertFalse(policy.optionEquals("--Silent", "--silent"));

        policy.enableCaseInsensitiveLiterals();
        policy.enableCaseInsensitiveOptions();

        assertTrue(policy.caseInsensitiveLiterals());
        assertTrue(policy.caseInsensitiveOptions());
        assertEquals("warp", policy.literalKey("Warp"));
        assertTrue(policy.optionEquals("--Silent", "--silent"));
        assertThrows(NullPointerException.class, () -> policy.literalKey(null));
        assertThrows(NullPointerException.class, () -> policy.optionEquals(null, "--x"));
        assertThrows(NullPointerException.class, () -> policy.optionEquals("--x", null));
    }

    @Test
    void validatorsRejectNullBlankAndInvalidNames() throws Exception {
        assertEquals("warp", Validators.literal("warp", "literal"));
        assertEquals("usage", Validators.metadata("usage", "usage"));
        assertEquals("player_name-1", Validators.name("player_name-1", "name"));
        assertThrows(NullPointerException.class, () -> Validators.literal(null, "literal"));
        assertThrows(IllegalArgumentException.class, () -> Validators.literal(" ", "literal"));
        assertThrows(NullPointerException.class, () -> Validators.metadata(null, "usage"));
        assertThrows(IllegalArgumentException.class, () -> Validators.metadata(" ", "usage"));
        assertThrows(NullPointerException.class, () -> Validators.name(null, "name"));
        assertThrows(IllegalArgumentException.class, () -> Validators.name(" ", "name"));
        assertThrows(IllegalArgumentException.class, () -> Validators.name("bad.name", "name"));

        Constructor<Validators> constructor = Validators.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void registryArgumentAndOptionSpecsValidateAndAttachSuggestionProviders() {
        RegistryArgumentSpec argument = new RegistryArgumentSpec("target", String.class, RegistryArgumentKind.REQUIRED);
        RegistryArgumentSpec suggestedArgument = argument.suggestions("players", ctx -> List.of("Ada"));
        RegistryArgumentSpec unnamedSuggestedArgument = argument.suggestions(ctx -> List.of("Bob"));
        RegistryOptionSpec option = new RegistryOptionSpec("duration", Integer.class, "d", RegistryOptionKind.VALUE);
        RegistryOptionSpec suggestedOption = option.suggestions("durations", ctx -> List.of("10"));
        RegistryOptionSpec unnamedSuggestedOption = option.suggestions(ctx -> List.of("20"));
        RegistryOptionSpec flag = new RegistryOptionSpec("silent", boolean.class, null, RegistryOptionKind.FLAG);

        assertTrue(argument.suggestionProviderOptional().isEmpty());
        assertEquals("players", suggestedArgument.suggestionProviderName());
        assertTrue(unnamedSuggestedArgument.suggestionProviderName() == null);
        assertEquals(List.of("Bob"), unnamedSuggestedArgument.suggestionProviderOptional().orElseThrow().suggestions(null));
        assertEquals(List.of("Ada"), suggestedArgument.suggestionProviderOptional().orElseThrow().suggestions(null));
        assertEquals("d", option.aliasOptional().orElseThrow());
        assertTrue(flag.aliasOptional().isEmpty());
        assertEquals("durations", suggestedOption.suggestionProviderName());
        assertTrue(unnamedSuggestedOption.suggestionProviderName() == null);
        assertEquals(List.of("20"), unnamedSuggestedOption.suggestionProviderOptional().orElseThrow().suggestions(null));
        assertEquals(List.of("10"), suggestedOption.suggestionProviderOptional().orElseThrow().suggestions(null));
        assertThrows(NullPointerException.class, () -> new RegistryArgumentSpec(null, String.class, RegistryArgumentKind.REQUIRED));
        assertThrows(IllegalArgumentException.class, () -> new RegistryArgumentSpec(" ", String.class, RegistryArgumentKind.REQUIRED));
        assertThrows(NullPointerException.class, () -> new RegistryArgumentSpec("target", null, RegistryArgumentKind.REQUIRED));
        assertThrows(NullPointerException.class, () -> new RegistryArgumentSpec("target", String.class, null));
        assertThrows(NullPointerException.class, () -> argument.suggestions(null));
        assertThrows(NullPointerException.class, () -> new RegistryOptionSpec(null, String.class, null, RegistryOptionKind.VALUE));
        assertThrows(IllegalArgumentException.class, () -> new RegistryOptionSpec(" ", String.class, null, RegistryOptionKind.VALUE));
        assertThrows(NullPointerException.class, () -> new RegistryOptionSpec("duration", null, null, RegistryOptionKind.VALUE));
        assertThrows(IllegalArgumentException.class, () -> new RegistryOptionSpec("duration", Integer.class, " ", RegistryOptionKind.VALUE));
        assertThrows(NullPointerException.class, () -> new RegistryOptionSpec("duration", Integer.class, null, null));
        assertThrows(NullPointerException.class, () -> option.suggestions(null));
    }

    @Test
    void commandNodesPathsAndArgumentTokenExposeModelBehavior() {
        RegistryCommandNode child = node("child", List.of(), Map.of(), ctx -> Results.success("child"));
        Map<String, RegistryCommandNode> children = new LinkedHashMap<>();
        children.put("child", child);
        children.put("alias", child);
        RegistryCommandNode root = node("root", List.of("r"), children, SimpleCommandRegistry.DEFAULT_EXECUTOR);
        RegistryCommandPath path = new RegistryCommandPath(List.of("root", "child"), List.of(root, child));
        ArgumentToken token = new ArgumentToken("reason", String.class, true);

        assertEquals(List.of("root", "r"), root.literals());
        assertTrue(root.descriptionOptional().isEmpty());
        assertTrue(root.permissionOptional().isEmpty());
        assertFalse(root.isExecutable());
        assertEquals(List.of(child), root.uniqueChildren());
        assertTrue(child.isExecutable());
        assertEquals(child, path.node());
        assertEquals("reason", token.name());
        assertEquals(String.class, token.type());
        assertTrue(token.greedy());
        assertThrows(NullPointerException.class, () -> new RegistryCommandPath(null, List.of(root)));
        assertThrows(NullPointerException.class, () -> new RegistryCommandPath(List.of("root"), null));
        assertThrows(IllegalArgumentException.class, () -> new RegistryCommandPath(List.of("root"), List.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode(null, null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), CommandMetadata.empty(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RegistryCommandNode("root", " ", null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), CommandMetadata.empty(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RegistryCommandNode("root", null, " ", List.of(), ctx -> Results.silent(),
            List.of(), List.of(), CommandMetadata.empty(), Map.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode("root", null, null, null, ctx -> Results.silent(),
            List.of(), List.of(), CommandMetadata.empty(), Map.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode("root", null, null, List.of(), null,
            List.of(), List.of(), CommandMetadata.empty(), Map.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode("root", null, null, List.of(), ctx -> Results.silent(),
            null, List.of(), CommandMetadata.empty(), Map.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode("root", null, null, List.of(), ctx -> Results.silent(),
            List.of(), null, CommandMetadata.empty(), Map.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode("root", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), null, Map.of()));
        assertThrows(NullPointerException.class, () -> new RegistryCommandNode("root", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), CommandMetadata.empty(), null));
    }

    @Test
    void routePatternParserConvertsSupportedDslAndRejectsAnalysisOnlyTypes() throws Exception {
        assertEquals("ban", RoutePatternParser.parse("ban|block <target:String> [reason:String...] [--silent|-s]").rootLiteral());
        assertThrows(IllegalArgumentException.class, () -> RoutePatternParser.parse("mode <type:Enum(SURVIVAL,CREATIVE)>"));
        assertThrows(IllegalArgumentException.class, () -> RoutePatternParser.parse("give <amount:Integer{1..64}>"));

        Constructor<RoutePatternParser> constructor = RoutePatternParser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void simpleCommandBuilderCoversMetadataSuggestionsPathAndValidationBranches() {
        SimpleCommandBuilder builder = new SimpleCommandBuilder("root");
        builder.description("Root command")
            .permission("root.use")
            .hidden()
            .usage("/root")
            .example("/root child")
            .cooldown(Duration.ofSeconds(1))
            .requirement("root.use")
            .group("admin")
            .alias("r")
            .aliases("rr")
            .argument("target", String.class)
            .optionalGreedyArgument("notes", String.class)
            .argumentSuggestions("target", "players", ctx -> List.of("Ada"))
            .argumentSuggestions("notes", ctx -> List.of("because"))
            .flag("silent")
            .option("duration", Integer.class, "d")
            .optionSuggestions("duration", "durations", ctx -> List.of("60"))
            .flag("verbose", "v")
            .optionSuggestions("verbose", ctx -> List.of("true"))
            .path("deep child", child -> child.executes(ctx -> Results.success("deep")))
            .executes(ctx -> Results.success("root"));

        RegistryCommandNode node = builder.node();

        assertEquals(List.of("root", "r", "rr"), node.literals());
        assertEquals("Root command", node.descriptionOptional().orElseThrow());
        assertEquals("root.use", node.permissionOptional().orElseThrow());
        assertTrue(node.metadata().hidden());
        assertEquals(Optional.of("/root"), node.metadata().usage());
        assertEquals(List.of("/root child"), node.metadata().examples());
        assertEquals(Optional.of(Duration.ofSeconds(1)), node.metadata().cooldown());
        assertEquals(Optional.of("root.use"), node.metadata().requirement());
        assertEquals(Optional.of("admin"), node.metadata().group());
        assertEquals("players", node.arguments().get(0).suggestionProviderName());
        assertTrue(node.arguments().get(1).suggestionProviderName() == null);
        assertEquals("durations", node.options().get(1).suggestionProviderName());
        assertTrue(node.options().get(2).suggestionProviderName() == null);
        assertTrue(node.children().containsKey("deep"));

        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder(null));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder(" "));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").alias("x"));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").aliases("a", "a"));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").aliases((String[]) null));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").subcommand("child", null));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x")
            .subcommand("child", child -> child.executes(ctx -> Results.silent()))
            .subcommand("child", child -> child.executes(ctx -> Results.silent())));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").path("a b", null));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").path(null, child -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").path(" ", child -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").path("child|alias", child -> {
        }));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .optionalArgument("a", String.class)
            .argument("b", String.class));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .greedyArgument("a", String.class)
            .argument("b", String.class));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .optionalGreedyArgument("a", String.class)
            .argument("b", String.class));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .optionalGreedyArgument("a", String.class)
            .greedyArgument("b", String.class));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .argument("a", String.class)
            .argument("a", String.class));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .flag("a")
            .argument("a", String.class));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .argument("a", String.class)
            .flag("a"));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .flag("a")
            .flag("a"));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x")
            .flag("a", "x")
            .option("b", String.class, "x"));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").path("bad<token", child -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").path("bad>token", child -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").path("bad[token", child -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCommandBuilder("x").path("bad]token", child -> {
        }));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").executes(null));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").argumentSuggestions("a", null));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x").argumentSuggestions("a", ctx -> List.of()));
        assertThrows(NullPointerException.class, () -> new SimpleCommandBuilder("x").optionSuggestions("a", null));
        assertThrows(IllegalStateException.class, () -> new SimpleCommandBuilder("x").optionSuggestions("a", ctx -> List.of()));
    }

    @Test
    void simpleCommandRegistryCoversLifecycleUpdateFindAndUnregisterBranches() {
        RecordingLifecycleListener listener = new RecordingLifecycleListener();
        SimpleCommandRegistry registry = new SimpleCommandRegistry(CommandMatchingPolicy.strict(), List.of(listener));
        registry.caseInsensitiveLiterals().caseInsensitiveOptions();

        registry.command("Root", command -> command.alias("r").subcommand("keep", child -> child.executes(ctx -> Results.silent())));
        registry.command("Root", command -> command.path("deep leaf", leaf -> leaf.executes(ctx -> Results.silent())));

        assertTrue(new SimpleCommandRegistry().find("missing") == null);
        assertTrue(registry.find("root").children().containsKey("deep"));
        assertTrue(registry.findPath(List.of()) == null);
    }

    @Test
    void simpleCommandRegistryFindsAndUnregistersRootsAndNestedPaths() {
        RecordingLifecycleListener listener = new RecordingLifecycleListener();
        SimpleCommandRegistry registry = new SimpleCommandRegistry(CommandMatchingPolicy.strict(), List.of(listener));

        registry.command("root", command -> command
            .subcommand("parent", parent -> parent
                .subcommand("leaf", leaf -> leaf.executes(ctx -> Results.silent()))
                .subcommand("sibling", sibling -> sibling.executes(ctx -> Results.silent()))));
        registry.command("solo", command -> command.executes(ctx -> Results.silent()));

        assertTrue(registry.findPath(List.of("root", "parent", "leaf")).nodes().size() == 3);
        assertTrue(registry.findPath(List.of("root", "missing")) == null);
        assertThrows(NullPointerException.class, () -> registry.findPath(null));
        assertThrows(NullPointerException.class, () -> registry.unregister(null));
        assertThrows(IllegalArgumentException.class, () -> registry.unregister(" "));
        assertFalse(registry.unregister("root missing"));

        assertTrue(registry.unregister("root parent leaf"));
        assertTrue(registry.findPath(List.of("root", "parent", "sibling")) != null);
        assertTrue(registry.unregister("root parent sibling"));
        assertTrue(registry.find("root") == null);
        registry.command("executableParent", command -> command
            .executes(ctx -> Results.silent())
            .subcommand("leaf", leaf -> leaf.executes(ctx -> Results.silent())));
        assertTrue(registry.unregister("executableParent leaf"));
        assertTrue(registry.find("executableParent") != null);
        assertTrue(registry.unregister("solo"));
        assertEquals(List.of("registered:root", "rebuilt:1", "registered:solo", "rebuilt:2",
            "unregistered:root parent leaf", "rebuilt:2", "unregistered:root parent sibling", "rebuilt:1",
            "registered:executableParent", "rebuilt:2", "unregistered:executableParent leaf", "rebuilt:2",
            "unregistered:solo", "rebuilt:1"), listener.events);
    }

    @Test
    void defaultExecutorAndNamedSuggestionProviderDelegateAreExecutable() {
        assertEquals(Results.silent(), SimpleCommandRegistry.DEFAULT_EXECUTOR.execute(null));
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("root <target:String> [--amount:Integer]")
            .argumentSuggestions("target", "players", ctx -> List.of("Ada"))
            .optionSuggestions("amount", "amounts", ctx -> List.of("1"))
            .executes(ctx -> Results.silent());

        RegistryCommandNode root = registry.find("root");
        assertEquals(List.of("Ada"), root.arguments().get(0).suggestionProviderOptional().orElseThrow().suggestions(null));
        assertEquals(List.of("1"), root.options().get(0).suggestionProviderOptional().orElseThrow().suggestions(null));
    }

    @Test
    void routeBuilderCoversMetadataSuggestionOverloadsAndUpdateEvents() {
        RecordingLifecycleListener listener = new RecordingLifecycleListener();
        SimpleCommandRegistry registry = new SimpleCommandRegistry(CommandMatchingPolicy.strict(), List.of(listener));

        registry.route("root child <target:String> [--duration:Integer|-d]")
            .description("Child command")
            .permission("root.child")
            .hidden()
            .usage("/root child")
            .example("/root child Ada")
            .cooldown(Duration.ofSeconds(2))
            .requirement("root.child")
            .group("admin")
            .argumentSuggestions("target", ctx -> List.of("Ada"))
            .optionSuggestions("duration", ctx -> List.of("10"))
            .executes(ctx -> Results.silent());
        registry.command("root", command -> command.path("sibling child", child -> child.executes(ctx -> Results.silent())));
        registry.route("root sibling").description("updated").executes(ctx -> Results.silent());

        RegistryCommandNode child = registry.findPath(List.of("root", "child")).node();
        assertEquals("Child command", child.description());
        assertEquals("root.child", child.permission());
        assertTrue(child.metadata().hidden());
        assertTrue(child.arguments().get(0).suggestionProviderName() == null);
        assertTrue(child.options().get(0).suggestionProviderName() == null);
        assertTrue(listener.events.contains("updated:root sibling"));
        assertThrows(IllegalArgumentException.class, () -> registry.route("broken [a:String] <b:String>")
            .executes(ctx -> Results.silent()));
    }

    @Test
    void builderSupportsDeepSubcommandNestingWithoutArtificialDepthLimit() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("root", root -> root
            .subcommand("l1", l1 -> l1
                .subcommand("l2", l2 -> l2
                    .subcommand("l3", l3 -> l3
                        .subcommand("l4", l4 -> l4
                            .subcommand("l5", l5 -> l5
                                .subcommand("l6", l6 -> l6
                                    .subcommand("l7", l7 -> l7
                                        .subcommand("l8", l8 -> l8
                                            .argument("target", String.class)
                                            .executes(ctx -> Results.success(
                                                "deep " + ctx.arg("target", String.class))))))))))));

        assertEquals(Optional.of("deep Ada"), framework.dispatch(source(Set.of()), "root l1 l2 l3 l4 l5 l6 l7 l8 Ada").reply());
        SimpleCommandRegistry registry = (SimpleCommandRegistry) framework.registry();
        assertEquals("l8", registry.findPath(List.of("root", "l1", "l2", "l3", "l4", "l5", "l6", "l7", "l8"))
            .node()
            .literal());
    }

    @Test
    void simpleCommandRegistryPrivateFallbackIsStableForUnreachableRouteLookup() throws Exception {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        RegistryCommandNode fallback = node("fallback", List.of(), Map.of(), ctx -> Results.silent());
        Method findEventNode = SimpleCommandRegistry.class.getDeclaredMethod("findEventNode", List.class, RegistryCommandNode.class);
        findEventNode.setAccessible(true);

        assertEquals(fallback, findEventNode.invoke(registry, List.of("missing"), fallback));
        registry.command("present", command -> command.executes(ctx -> Results.silent()));
        assertEquals(registry.find("present"), findEventNode.invoke(registry, List.of("present"), fallback));
    }

    @Test
    void manualCommandImporterCoversAllShapesAndInvalidTrees() throws Exception {
        Constructor<ManualCommandImporter> constructor = ManualCommandImporter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());

        CommandNode child = Commands.literal("child")
            .argument(new ArgumentSpec<>("message", String.class, ArgumentSpec.Kind.GREEDY))
            .flag(new FlagSpec<>("verbose", Boolean.class, "v", FlagSpec.Kind.FLAG))
            .flag(new FlagSpec<>("amount", Integer.class, "a", FlagSpec.Kind.VALUE))
            .handler(ctx -> Results.silent())
            .build();
        CommandNode optionalTailChild = Commands.literal("optionalTail")
            .argument(new ArgumentSpec<>("tail", String.class, ArgumentSpec.Kind.OPTIONAL_GREEDY))
            .build();
        CommandNode node = Commands.literal("root")
            .description("Root")
            .permission("root.use")
            .aliases("r")
            .argument(new ArgumentSpec<>("target", String.class, ArgumentSpec.Kind.REQUIRED))
            .argument(new ArgumentSpec<>("count", Integer.class, ArgumentSpec.Kind.OPTIONAL))
            .metadata(new CommandMetadata.Builder()
                .hidden()
                .usage("/root")
                .example("/root Ada")
                .cooldown(Duration.ofSeconds(3))
                .requirement("root.use")
                .group("general")
                .build())
            .child(child)
            .child(optionalTailChild)
            .handler(ctx -> Results.silent())
            .build();

        RegistryCommandNode imported = ManualCommandImporter.importNode(node);
        CommandNode exported = ManualCommandImporter.exportNode(imported);

        assertEquals("Root", exported.description().orElseThrow());
        assertEquals("root.use", exported.permission().orElseThrow());
        assertEquals(List.of("r"), exported.aliases());
        assertEquals(2, exported.arguments().size());
        assertEquals(2, exported.children().size());
        assertEquals(1, exported.children().get(0).arguments().size());
        assertEquals(2, exported.children().get(0).flags().size());
        assertTrue(exported.metadata().hidden());

        CommandNode invalid = Commands.literal("bad")
            .argument(new ArgumentSpec<>("maybe", String.class, ArgumentSpec.Kind.OPTIONAL))
            .argument(new ArgumentSpec<>("required", String.class, ArgumentSpec.Kind.REQUIRED))
            .build();
        assertThrows(IllegalArgumentException.class, () -> ManualCommandImporter.importNode(invalid));
    }

    @Test
    void commandPermissionsCoverDiscoveryRequirementsAndLiteralDescendants() {
        RegistryCommandNode visible = node("visible", List.of(), Map.of(), ctx -> Results.silent());
        RegistryCommandNode hidden = new RegistryCommandNode("hidden", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), new CommandMetadata.Builder().hidden().build(), Map.of());
        RegistryCommandNode required = new RegistryCommandNode("required", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), new CommandMetadata.Builder().requirement("a && b || c").build(), Map.of());
        RegistryCommandNode parent = node("parent", List.of(), Map.of("visible", visible), SimpleCommandRegistry.DEFAULT_EXECUTOR);

        assertEquals(List.of(parent, visible), CommandPermissions.literalDescendantPath(parent,
            List.of("missing", "visible"),
            List.of(parent)));
        assertFalse(CommandPermissions.canDiscover(source(Set.of()), List.of(hidden), hidden));
        assertFalse(CommandPermissions.canDiscover(source(Set.of()), List.of(required), required));
        assertTrue(CommandPermissions.canDiscover(source(Set.of("c")), List.of(required), required));
        assertTrue(CommandPermissions.canDiscover(source(Set.of()), List.of(parent), parent));
        assertEquals(Optional.of("a && b || c"), CommandPermissions.deniedPermission(source(Set.of()), List.of(required)));
        assertEquals(Optional.empty(), CommandPermissions.deniedPermission(source(Set.of("a", "b")), List.of(required)));
        RegistryCommandNode emptyRequirementTerm = new RegistryCommandNode("empty", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), new CommandMetadata.Builder().requirement("a && ").build(), Map.of());
        assertEquals(Optional.of("a && "), CommandPermissions.deniedPermission(source(Set.of("a")), List.of(emptyRequirementTerm)));
    }

    @Test
    void commandRequirementsEvaluateBooleanExpressionsWithPrecedenceNegationAndParentheses() {
        RegistryCommandNode required = new RegistryCommandNode("secure", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), new CommandMetadata.Builder()
                .requirement("staff && (!banned || owner)")
                .build(), Map.of());
        RegistryCommandNode malformed = new RegistryCommandNode("malformed", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), new CommandMetadata.Builder()
                .requirement("staff && (owner || )")
                .build(), Map.of());

        assertEquals(Optional.empty(), CommandPermissions.deniedPermission(source(Set.of("staff")), List.of(required)));
        assertEquals(Optional.empty(), CommandPermissions.deniedPermission(source(Set.of("staff", "banned", "owner")),
            List.of(required)));
        assertEquals(Optional.of("staff && (!banned || owner)"),
            CommandPermissions.deniedPermission(source(Set.of("staff", "banned")), List.of(required)));
        assertEquals(Optional.of("staff && (owner || )"),
            CommandPermissions.deniedPermission(source(Set.of("staff", "owner")), List.of(malformed)));
    }

    @Test
    void requirementExpressionCoversValidationEdgesAndNullContracts() throws Exception {
        assertTrue(RequirementExpression.evaluate("staff", permission -> permission.equals("staff")));
        assertTrue(RequirementExpression.evaluate(" staff && (!banned || owner) ",
            permission -> Set.of("staff").contains(permission)));
        assertTrue(RequirementExpression.evaluate("staff && (banned || owner)",
            permission -> Set.of("staff", "owner").contains(permission)));
        assertFalse(RequirementExpression.evaluate("staff && banned",
            permission -> Set.of("staff").contains(permission)));
        assertFalse(RequirementExpression.evaluate("staff &&", permission -> true));
        assertFalse(RequirementExpression.evaluate("staff && (owner || )", permission -> true));
        assertFalse(RequirementExpression.evaluate("staff owner", permission -> true));
        assertFalse(RequirementExpression.evaluate("staff!owner", permission -> true));
        assertFalse(RequirementExpression.evaluate(")", permission -> true));
        assertThrows(NullPointerException.class, () -> RequirementExpression.evaluate(null, permission -> true));
        assertThrows(NullPointerException.class, () -> RequirementExpression.evaluate("staff", null));

        Constructor<RequirementExpression> constructor = RequirementExpression.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void registryNodeMergerCoversMergeSuccessAndConflictBranches() throws Exception {
        Constructor<RegistryNodeMerger> constructor = RegistryNodeMerger.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());

        CommandMatchingPolicy policy = CommandMatchingPolicy.strict();
        Map<String, RegistryCommandNode> roots = new LinkedHashMap<>();
        RegistryCommandNode base = node("root", List.of("r"), Map.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR);
        RegistryNodeMerger.registerAll(roots, base.literals(), base, "dup: ", policy);
        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.registerAll(roots, List.of("root"), base, "dup: ", policy));

        RegistryCommandNode executable = node("root", List.of(), Map.of(), ctx -> Results.success("ok"));
        RegistryNodeMerger.mergeRoot(roots, executable, policy);
        assertTrue(roots.get("root").isExecutable());

        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.mergeRoot(roots, executable, policy));
        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.mergeChild(new LinkedHashMap<>(Map.of("other", base)),
            node("other", List.of(), Map.of(), ctx -> Results.silent()),
            policy));
        RegistryCommandNode described = new RegistryCommandNode("root", "one", null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        RegistryCommandNode otherDescription = new RegistryCommandNode("root", "two", null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("root", described)),
            otherDescription,
            policy));

        Map<String, RegistryCommandNode> childMap = new LinkedHashMap<>();
        RegistryNodeMerger.mergeChild(childMap, node("child", List.of("c"), Map.of(), ctx -> Results.silent()), policy);
        assertTrue(childMap.containsKey("child"));
        assertTrue(childMap.containsKey("c"));

        RegistryCommandNode metadataBase = new RegistryCommandNode("meta", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), new CommandMetadata.Builder().usage("/meta").build(), Map.of());
        RegistryCommandNode sameMetadataExecutable = new RegistryCommandNode("meta", null, null, List.of(), ctx -> Results.silent(),
            List.of(), List.of(), new CommandMetadata.Builder().usage("/meta").build(), Map.of());
        RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("meta", metadataBase)), sameMetadataExecutable, policy);
        RegistryCommandNode conflictingMetadata = new RegistryCommandNode("meta", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), new CommandMetadata.Builder().usage("/other").build(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("meta", metadataBase)),
            conflictingMetadata,
            policy));
        RegistryCommandNode emptyMetadata = new RegistryCommandNode("meta", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("meta", metadataBase)), emptyMetadata, policy);
        RegistryCommandNode permissioned = new RegistryCommandNode("perm", null, "one", List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        RegistryCommandNode otherPermission = new RegistryCommandNode("perm", null, "two", List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("perm", permissioned)),
            otherPermission,
            policy));
        RegistryCommandNode noPermission = new RegistryCommandNode("perm", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("perm", permissioned)), noPermission, policy);
        RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("perm", permissioned)), permissioned, policy);
        RegistryCommandNode argsOne = new RegistryCommandNode("shape", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(new RegistryArgumentSpec("a", String.class, RegistryArgumentKind.REQUIRED)), List.of(), CommandMetadata.empty(), Map.of());
        RegistryCommandNode argsTwo = new RegistryCommandNode("shape", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(new RegistryArgumentSpec("b", String.class, RegistryArgumentKind.REQUIRED)), List.of(), CommandMetadata.empty(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("shape", argsOne)),
            argsTwo,
            policy));
        RegistryCommandNode argsEmpty = new RegistryCommandNode("shape", null, null, List.of(), SimpleCommandRegistry.DEFAULT_EXECUTOR,
            List.of(), List.of(), CommandMetadata.empty(), Map.of());
        RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("shape", argsOne)), argsEmpty, policy);
        RegistryNodeMerger.mergeRoot(new LinkedHashMap<>(Map.of("shape", argsOne)), argsOne, policy);

        Method mergeCommandMetadata = RegistryNodeMerger.class.getDeclaredMethod(
            "mergeCommandMetadata",
            CommandMetadata.class,
            CommandMetadata.class
        );
        mergeCommandMetadata.setAccessible(true);
        assertEquals(CommandMetadata.empty(), mergeCommandMetadata.invoke(null, CommandMetadata.empty(), CommandMetadata.empty()));
    }

    private static final class RecordingLifecycleListener implements CommandLifecycleListener {
        private final List<String> events = new ArrayList<>();

        @Override
        public void commandRegistered(CommandNode command, List<String> path) {
            events.add("registered:" + String.join(" ", path));
        }

        @Override
        public void commandUpdated(CommandNode command, List<String> path) {
            events.add("updated:" + String.join(" ", path));
        }

        @Override
        public void commandUnregistered(List<String> path) {
            events.add("unregistered:" + String.join(" ", path));
        }

        @Override
        public void registryRebuilt(List<CommandNode> roots) {
            events.add("rebuilt:" + roots.size());
        }
    }

    private static RegistryCommandNode node(
        String literal,
        List<String> aliases,
        Map<String, RegistryCommandNode> children,
        CommandRegistry.CommandExecutor executor
    ) {
        return new RegistryCommandNode(
            literal,
            null,
            null,
            aliases,
            executor,
            List.of(),
            List.of(),
            CommandMetadata.empty(),
            children
        );
    }

    private static dev.riege.buildmycommand.api.CommandSource source(Set<String> permissions) {
        return new dev.riege.buildmycommand.api.CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
    }
}
