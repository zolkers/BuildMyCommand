package dev.riege.buildmycommand.core.help;

import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.parse.CommandTokenizer;
import dev.riege.buildmycommand.core.registry.RegistryArgumentKind;
import dev.riege.buildmycommand.core.registry.RegistryArgumentSpec;
import dev.riege.buildmycommand.core.registry.RegistryOptionKind;
import dev.riege.buildmycommand.core.registry.RegistryOptionSpec;
import dev.riege.buildmycommand.core.registry.SimpleCommandRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HelpFormattingCoverageTest {
    @Test
    void commandFormattingCoversAllArgumentOptionKindsAndPrimitiveTypeNames() throws Exception {
        assertEquals("<target:String>", CommandFormatting.usageArgument(
            new RegistryArgumentSpec("target", String.class, RegistryArgumentKind.REQUIRED)));
        assertEquals("[count:int]", CommandFormatting.usageArgument(
            new RegistryArgumentSpec("count", int.class, RegistryArgumentKind.OPTIONAL)));
        assertEquals("<reason:String...>", CommandFormatting.usageArgument(
            new RegistryArgumentSpec("reason", String.class, RegistryArgumentKind.GREEDY)));
        assertEquals("[tail:String...]", CommandFormatting.usageArgument(
            new RegistryArgumentSpec("tail", String.class, RegistryArgumentKind.OPTIONAL_GREEDY)));
        assertEquals("[--silent|-s]", CommandFormatting.usageOption(
            new RegistryOptionSpec("silent", boolean.class, "s", RegistryOptionKind.FLAG)));
        assertEquals("[--amount:long]", CommandFormatting.usageOption(
            new RegistryOptionSpec("amount", long.class, null, RegistryOptionKind.VALUE)));
        assertEquals("double", CommandFormatting.typeName(double.class));
        assertEquals("boolean", CommandFormatting.typeName(boolean.class));
        assertEquals("required", CommandFormatting.schemaArgumentKind(RegistryArgumentKind.REQUIRED));
        assertEquals("optional", CommandFormatting.schemaArgumentKind(RegistryArgumentKind.OPTIONAL));
        assertEquals("greedy", CommandFormatting.schemaArgumentKind(RegistryArgumentKind.GREEDY));
        assertEquals("optional-greedy", CommandFormatting.schemaArgumentKind(RegistryArgumentKind.OPTIONAL_GREEDY));
        assertEquals("flag", CommandFormatting.schemaOptionKind(RegistryOptionKind.FLAG));
        assertEquals("value", CommandFormatting.schemaOptionKind(RegistryOptionKind.VALUE));

        Constructor<CommandFormatting> constructor = CommandFormatting.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void schemaExporterIncludesMetadataArgumentsOptionsSuggestionsAndChildren() {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("admin|a punish <target:String> [reason:String...] [--duration:Integer|-d] [--silent|-s]")
            .description("Punish a player")
            .permission("admin.punish")
            .hidden()
            .group("moderation")
            .usage("/admin punish <target> [reason]")
            .example("/admin punish Ada spam")
            .cooldown(Duration.ofSeconds(3))
            .requirement("staff && online")
            .argumentSuggestions("target", "players", ctx -> List.of("Ada"))
            .optionSuggestions("duration", "durations", ctx -> List.of("60"))
            .executes(ctx -> Results.silent());
        registry.route("admin pardon <target:String>").executes(ctx -> Results.silent());

        String schema = new SchemaExporter().schema(registry);

        assertEquals("""
            command admin
              child punish
              child pardon
            command admin punish
              description Punish a player
              permission admin.punish
              hidden true
              group moderation
              usage /admin punish <target> [reason]
              example /admin punish Ada spam
              cooldown PT3S
              require staff && online
              argument target:String required suggest=players
              argument reason:String optional-greedy
              option duration:Integer value alias=d suggest=durations
              option silent:Boolean flag alias=s
            command admin pardon
              argument target:String required""", schema);
    }

    @Test
    void helpGeneratorHandlesUnknownHiddenDeniedAndGeneratedUsage() {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("ban <target:String> [reason:String...] [--silent|-s]")
            .description("Ban a player")
            .example("/ban Ada")
            .permission("ban.use")
            .executes(ctx -> Results.silent());
        registry.route("secret").hidden().executes(ctx -> Results.silent());
        HelpGenerator help = new HelpGenerator(registry, new CommandTokenizer());

        assertEquals("Unknown command: ", help.help(source(Set.of("ban.use")), ""));
        assertEquals("Unknown command: \"oops", help.help(source(Set.of("ban.use")), "\"oops"));
        assertEquals("Unknown command: missing", help.help(source(Set.of("ban.use")), "missing"));
        assertEquals("Unknown command: secret", help.help(source(Set.of("ban.use")), "secret"));
        assertEquals("Missing permission: ban.use", help.help(source(Set.of()), "ban"));
        assertEquals("""
            Usage: ban <target:String> [reason:String...] [--silent|-s]
            Description: Ban a player
            Example: /ban Ada""", help.help(source(Set.of("ban.use")), "ban"));
    }

    private static CommandSource source(Set<String> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
    }
}
