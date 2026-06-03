package dev.riege.buildmycommand.schema;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.Arguments;
import dev.riege.buildmycommand.api.Flags;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandSchemaExporterTest {
    @Test
    void exportsJsonSchemaWithAliasesArgumentsOptionsAndMetadata() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String> [--silent|-s]")
            .description("Punish a player")
            .permission("mod.ban")
            .hidden()
            .usage("/ban <target>")
            .example("/ban Ada")
            .cooldown(Duration.ofSeconds(5))
            .requirement("mod.ban && mod.audit")
            .group("moderation")
            .executes(ctx -> Results.silent());

        String json = new CommandSchemaExporter().exportJson(framework);

        assertTrue(json.contains("\"path\":\"ban\""));
        assertTrue(json.contains("\"aliases\":[\"block\"]"));
        assertTrue(json.contains("\"name\":\"target\""));
        assertTrue(json.contains("\"alias\":\"s\""));
        assertTrue(json.contains("\"permission\":\"mod.ban\""));
        assertTrue(json.contains("\"hidden\":true"));
        assertTrue(json.contains("\"examples\":[\"/ban Ada\"]"));
        assertTrue(json.contains("\"require\":\"mod.ban && mod.audit\""));
    }

    @Test
    void inspectsRouteAndExportsMermaidGraph() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("admin|adm user delete <target:String>")
            .executes(ctx -> Results.silent());

        RouteInspection inspection = new RouteInspector().inspect(framework, "adm user delete Ada");
        String mermaid = new CommandSchemaExporter().exportMermaid(framework);

        assertEquals(List.of("adm", "user", "delete", "Ada"), inspection.tokens());
        assertEquals(List.of("admin", "user", "delete"), inspection.matchedPath());
        assertTrue(inspection.executable());
        assertTrue(mermaid.contains("graph TD"));
        assertTrue(mermaid.contains("[\"admin user delete\"]"));
    }

    @Test
    void exportsEmptyGraphsEscapedValuesAndMultipleSiblings() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("say", command -> command
            .description("Say \"hi\" on C:\\tmp")
            .executes(ctx -> Results.silent()));
        framework.registry().command("stop", command -> command.executes(ctx -> Results.silent()));
        CommandSchemaExporter exporter = new CommandSchemaExporter();

        assertEquals("{\"commands\":[]}", exporter.exportJson(new CommandGraph(List.of())));
        String json = exporter.exportJson(framework);
        String mermaid = exporter.exportMermaid(framework);

        assertTrue(json.contains("Say \\\"hi\\\" on C:\\\\tmp"));
        assertTrue(json.contains("\"path\":\"stop\""));
        assertTrue(mermaid.contains("say[\"say\"]"));
        assertTrue(mermaid.contains("stop[\"stop\"]"));
        assertThrows(NullPointerException.class, () -> exporter.exportJson((CommandFramework) null));
        assertThrows(NullPointerException.class, () -> exporter.exportJson((CommandGraph) null));
        assertThrows(NullPointerException.class, () -> exporter.exportMermaid(null));
    }

    @Test
    void exportsMultipleArgumentsOptionsExamplesAndFindsFallbackPaths() throws Exception {
        CommandGraph graph = new CommandGraph(List.of(
            Commands.literal("root")
                .alias("r")
                .aliases("main")
                .child(Commands.literal("child")
                    .argument(Arguments.required("target", String.class))
                    .argument(Arguments.optional("reason", String.class))
                    .flag(Flags.bool("silent").alias("s"))
                    .flag(Flags.option("amount", Integer.class).alias("a"))
                    .metadata(new dev.riege.buildmycommand.api.CommandMetadata.Builder()
                        .example("/root child Ada")
                        .example("/r child Ada")
                        .build())
                    .build())
                .build()
        ));
        CommandSchemaExporter exporter = new CommandSchemaExporter();
        Method pathFor = CommandSchemaExporter.class.getDeclaredMethod("pathFor", CommandGraph.class,
            dev.riege.buildmycommand.api.CommandNode.class);
        Method findPath = CommandSchemaExporter.class.getDeclaredMethod("findPath",
            dev.riege.buildmycommand.api.CommandNode.class,
            dev.riege.buildmycommand.api.CommandNode.class,
            List.class);
        pathFor.setAccessible(true);
        findPath.setAccessible(true);

        String json = exporter.exportJson(graph);
        Object fallback = pathFor.invoke(null, new CommandGraph(List.of()), Commands.literal("orphan").build());
        Object notFound = findPath.invoke(null, graph.roots().get(0), Commands.literal("missing").build(), List.of());

        assertTrue(json.contains("\"arguments\":[{\"name\":\"target\""));
        assertTrue(json.contains("{\"name\":\"reason\""));
        assertTrue(json.contains("\"options\":[{\"name\":\"silent\""));
        assertTrue(json.contains("{\"name\":\"amount\""));
        assertTrue(json.contains("\"aliases\":[\"r\",\"main\"]"));
        assertTrue(json.contains("\"examples\":[\"/root child Ada\",\"/r child Ada\"]"));
        assertEquals(List.of("orphan"), fallback);
        assertEquals(List.of(), notFound);
    }


    @Test
    void routeInspectorHandlesAliasesUnknownTokensAndNullInputs() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("admin", admin -> admin
            .alias("adm")
            .subcommand("reload", reload -> reload
                .executes(ctx -> Results.success("ok")))
            .subcommand("status", status -> { }));
        RouteInspector inspector = new RouteInspector();

        RouteInspection alias = inspector.inspect(framework, "adm reload now");
        RouteInspection unknown = inspector.inspect(framework, "missing token");
        RouteInspection nonExecutable = inspector.inspect(framework, "admin status");

        assertEquals(List.of("adm", "reload", "now"), alias.tokens());
        assertEquals(List.of("admin", "reload"), alias.matchedPath());
        assertTrue(alias.executable());
        assertEquals(List.of(), unknown.matchedPath());
        assertEquals(List.of(), unknown.arguments());
        assertEquals(List.of(), unknown.options());
        assertFalse(unknown.executable());
        assertEquals(List.of("admin", "status"), nonExecutable.matchedPath());
        assertFalse(nonExecutable.executable());
        assertThrows(NullPointerException.class, () -> inspector.inspect(null, "x"));
        assertThrows(NullPointerException.class, () -> inspector.inspect(framework, null));
        assertThrows(NullPointerException.class, () -> new RouteInspection(null, List.of(), List.of(), List.of(),
            false));
    }

    @Test
    void conflictReportsSnapshotAndExposeConflictPresence() {
        List<String> values = new ArrayList<>(List.of("ban <x>", "ban <y>"));
        ConflictReport report = new ConflictReport(values);
        values.clear();

        assertTrue(report.hasConflicts());
        assertEquals(List.of("ban <x>", "ban <y>"), report.conflicts());
        assertFalse(new ConflictReport(List.of()).hasConflicts());
        assertThrows(UnsupportedOperationException.class, () -> report.conflicts().add("other"));
        assertThrows(NullPointerException.class, () -> new ConflictReport(null));
        assertEquals("{\"commands\":[{\"path\":\"orphan\",\"aliases\":[],\"arguments\":[],\"options\":[],"
            + "\"hidden\":false,\"examples\":[]}]}",
            new CommandSchemaExporter().exportJson(new CommandGraph(List.of(Commands.literal("orphan").build()))));
    }

    @Test
    void exporterDetectsConflictsFromFrameworkAndManualGraphs() {
        CommandSchemaExporter exporter = new CommandSchemaExporter();
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban <target:String> [--silent|-s]").executes(ctx -> Results.silent());
        framework.registry().route("kick <target:String>").executes(ctx -> Results.silent());

        CommandGraph conflictGraph = new CommandGraph(List.of(
            Commands.literal("ban")
                .alias("block")
                .child(Commands.literal("user")
                    .argument(Arguments.required("target", String.class))
                    .flag(Flags.bool("silent").alias("s"))
                    .handler(ctx -> Results.silent())
                    .build())
                .build(),
            Commands.literal("block")
                .child(Commands.literal("user")
                    .argument(Arguments.required("player", String.class))
                    .flag(Flags.bool("silent").alias("s"))
                    .handler(ctx -> Results.silent())
                    .build())
                .build(),
            Commands.literal("ignoredParent")
                .child(Commands.literal("shape")
                    .argument(Arguments.required("amount", Integer.class))
                    .build())
                .build(),
            Commands.literal("ignoredParent")
                .child(Commands.literal("shape")
                    .argument(Arguments.required("other", Integer.class))
                    .build())
                .build()
        ));

        ConflictReport empty = exporter.detectConflicts(framework);
        ConflictReport conflicts = exporter.detectConflicts(conflictGraph);

        assertFalse(empty.hasConflicts());
        assertTrue(conflicts.hasConflicts());
        assertEquals(List.of("ban|block user <target:String> [--silent|-s] conflicts with block user <player:String> [--silent|-s]"),
            conflicts.conflicts());
        assertThrows(NullPointerException.class, () -> exporter.detectConflicts((CommandFramework) null));
        assertThrows(NullPointerException.class, () -> exporter.detectConflicts((CommandGraph) null));
    }

    @Test
    void exporterDetectsOptionValueAndRootExecutableConflicts() {
        CommandSchemaExporter exporter = new CommandSchemaExporter();
        CommandGraph graph = new CommandGraph(List.of(
            Commands.literal("root")
                .argument(Arguments.optional("reason", String.class))
                .flag(Flags.option("amount", Integer.class).alias("a"))
                .handler(ctx -> Results.silent())
                .build(),
            Commands.literal("root")
                .argument(Arguments.optional("message", String.class))
                .flag(Flags.option("amount", Integer.class).alias("a"))
                .handler(ctx -> Results.silent())
                .build()
        ));

        ConflictReport report = exporter.detectConflicts(graph);

        assertEquals(List.of("root [reason:String] [--amount:Integer|-a] conflicts with root [message:String] [--amount:Integer|-a]"),
            report.conflicts());
    }

    @Test
    void exporterDetectsGreedyArgumentConflicts() {
        CommandSchemaExporter exporter = new CommandSchemaExporter();
        CommandGraph graph = new CommandGraph(List.of(
            Commands.literal("say")
                .argument(Arguments.greedy("message", String.class))
                .handler(ctx -> Results.silent())
                .build(),
            Commands.literal("say")
                .argument(Arguments.greedy("text", String.class))
                .handler(ctx -> Results.silent())
                .build(),
            Commands.literal("note")
                .argument(Arguments.greedyOptional("message", String.class))
                .handler(ctx -> Results.silent())
                .build(),
            Commands.literal("note")
                .argument(Arguments.greedyOptional("text", String.class))
                .handler(ctx -> Results.silent())
                .build()
        ));

        ConflictReport report = exporter.detectConflicts(graph);

        assertEquals(List.of(
            "say <message:String...> conflicts with say <text:String...>",
            "note [message:String...] conflicts with note [text:String...]"
        ), report.conflicts());
    }
}
