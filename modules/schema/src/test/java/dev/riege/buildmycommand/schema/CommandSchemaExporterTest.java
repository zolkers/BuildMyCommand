package dev.riege.buildmycommand.schema;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
