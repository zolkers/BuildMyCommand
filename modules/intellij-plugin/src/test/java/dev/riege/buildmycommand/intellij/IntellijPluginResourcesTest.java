/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntellijPluginResourcesTest {
    @Test
    void pluginDeclaresIntelliLangInjectionConfiguration() throws IOException {
        String pluginXml = resource("META-INF/plugin.xml");
        String injections = resource("buildmycommandInjections.xml");

        assertTrue(pluginXml.contains("org.intellij.intelliLang"));
        assertTrue(pluginXml.contains("org.jetbrains.plugins.textmate"));
        assertTrue(pluginXml.contains("buildmycommandInjections.xml"));
        assertTrue(pluginXml.contains("textmate.bundleProvider"));
        assertTrue(pluginXml.contains("lang.syntaxHighlighterFactory"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteSyntaxHighlighterFactory"));
        assertTrue(pluginXml.contains("BuildMyCommandRequirementSyntaxHighlighterFactory"));
        assertTrue(pluginXml.contains("BuildMyCommandPermissionRegexSyntaxHighlighterFactory"));
        assertTrue(pluginXml.contains("multiHostInjector"));
        assertTrue(!pluginXml.contains("<languageInjector"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteInjector"));
        assertTrue(pluginXml.contains("BuildMyCommandRequirementInjector"));
        assertTrue(pluginXml.contains("BuildMyCommandPermissionRegexInjector"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteAnnotator"));
        assertTrue(pluginXml.contains("BuildMyCommandRequirementAnnotator"));
        assertTrue(pluginXml.contains("BuildMyCommandPermissionRegexAnnotator"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteCompletionContributor"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteInspection"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteParserDefinition"));
        assertTrue(pluginXml.contains("BuildMyCommandRequirementParserDefinition"));
        assertTrue(pluginXml.contains("BuildMyCommandPermissionRegexParserDefinition"));
        assertTrue(pluginXml.contains("localInspection"));
        assertTrue(pluginXml.contains("completion.contributor"));
        assertTrue(pluginXml.contains("additionalTextAttributes"));
        assertTrue(injections.contains("dev.riege.buildmycommand.annotation.Route"));
        assertTrue(injections.contains("dev.riege.buildmycommand.annotation.SubRoute"));
        assertTrue(injections.contains("dev.riege.buildmycommand.api.CommandRegistry"));
        assertTrue(injections.contains("language=\"BuildMyCommandRoute\""));
        assertTrue(injections.contains("route"));
        assertTrue(injections.contains("subRoute"));
        assertTrue(injections.contains("path"));
    }

    @Test
    void textMateBundleResourcesArePresent() throws IOException {
        String manifest = resource("textmate/buildmycommand-route/package.json");
        String grammar = resource("textmate/buildmycommand-route/syntaxes/buildmycommand-route.tmLanguage.json");

        assertTrue(manifest.contains("\"buildmycommand-route\""));
        assertTrue(manifest.contains("\"source.buildmycommand.route\""));
        assertTrue(manifest.contains("\"./syntaxes/buildmycommand-route.tmLanguage.json\""));
        assertTrue(grammar.contains("\"scopeName\": \"source.buildmycommand.route\""));
        assertTrue(grammar.contains("\"begin\": \"\\\\[(?=--)\""));
        assertTrue(grammar.contains("\"begin\": \"\\\\[(?!--)\""));
        assertTrue(grammar.contains("String|Integer|int|Long|long|Double|double|Float|float|Boolean|boolean|UUID"));
        assertTrue(grammar.contains("Duration|LocalDate|LocalDateTime|Path|URI|URL"));
        assertTrue(grammar.contains("entity.name.option.long.buildmycommand.route"));
        assertTrue(grammar.contains("keyword.operator.greedy.buildmycommand.route"));
    }

    @Test
    void colorSchemesDeclareBuildMyCommandTextMateScopes() throws IOException {
        String light = resource("colorSchemes/BuildMyCommandRoute.xml");
        String dark = resource("colorSchemes/BuildMyCommandRouteDarcula.xml");

        assertTrue(light.contains("TEXTMATE_SOURCE_BUILDMYCOMMAND_ROUTE"));
        assertTrue(light.contains("<attributes>"));
        assertTrue(!light.contains("<scheme"));
        assertTrue(light.contains("ENTITY_NAME_FUNCTION_LITERAL_BUILDMYCOMMAND_ROUTE"));
        assertTrue(light.contains("VARIABLE_PARAMETER_ARGUMENT_BUILDMYCOMMAND_ROUTE"));
        assertTrue(light.contains("STORAGE_TYPE_BUILDMYCOMMAND_ROUTE"));
        assertTrue(light.contains("ENTITY_NAME_OPTION_LONG_BUILDMYCOMMAND_ROUTE"));
        assertTrue(light.contains("ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REQUIRE"));
        assertTrue(light.contains("KEYWORD_OPERATOR_BUILDMYCOMMAND_REQUIRE"));
        assertTrue(light.contains("PUNCTUATION_GROUP_BUILDMYCOMMAND_REQUIRE"));
        assertTrue(light.contains("ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REGEX"));
        assertTrue(light.contains("CONSTANT_CHARACTER_ESCAPE_BUILDMYCOMMAND_REGEX"));
        assertTrue(light.contains("KEYWORD_OPERATOR_QUANTIFIER_BUILDMYCOMMAND_REGEX"));
        assertTrue(dark.contains("<attributes>"));
        assertTrue(!dark.contains("<scheme"));
        assertTrue(dark.contains("TEXTMATE_SOURCE_BUILDMYCOMMAND_ROUTE"));
        assertTrue(dark.contains("ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REQUIRE"));
        assertTrue(dark.contains("ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REGEX"));
    }

    @Test
    void textMateBundleProviderReturnsBundledGrammarDirectory() {
        BuildMyCommandTextMateBundleProvider provider = new BuildMyCommandTextMateBundleProvider();

        List<org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle> bundles = provider.getBundles();

        assertTrue(bundles.stream().anyMatch(bundle -> bundle.getName().equals("BuildMyCommand Route DSL")));
        assertTrue(bundles.stream().anyMatch(bundle -> bundle.getPath().toString().contains("buildmycommand-route")));
    }

    @Test
    void textMateBundleProviderExtractsBundleDirectoriesFromPluginJars() throws IOException {
        Path jar = Files.createTempFile("buildmycommand-textmate", ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("ignored.txt"));
            output.write("ignored".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new JarEntry("textmate/buildmycommand-route/syntaxes/"));
            output.closeEntry();
            output.putNextEntry(new JarEntry("textmate/buildmycommand-route/package.json"));
            output.write("{\"name\":\"buildmycommand-route\"}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new JarEntry("textmate/buildmycommand-route/syntaxes/buildmycommand-route.tmLanguage.json"));
            output.write("{\"scopeName\":\"source.buildmycommand.route\"}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        var bundle = BuildMyCommandTextMateBundleProvider.bundle(
            "BuildMyCommand Route DSL",
            new java.net.URL("jar:" + jar.toUri() + "!/textmate/buildmycommand-route")
        );

        assertTrue(Files.isDirectory(bundle.getPath()));
        assertTrue(Files.readString(bundle.getPath().resolve("package.json")).contains("buildmycommand-route"));
        assertTrue(Files.exists(bundle.getPath().resolve("syntaxes/buildmycommand-route.tmLanguage.json")));
    }

    @Test
    void textMateBundleProviderRejectsJarUrlsWithoutBundleEntry() throws IOException {
        Path jar = Files.createTempFile("buildmycommand-textmate-empty", ".jar");
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(jar))) {
        }

        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> BuildMyCommandTextMateBundleProvider.bundle(
                "BuildMyCommand Route DSL",
                new java.net.URL("jar:" + jar.toUri() + "!/")
            ));

        assertTrue(exception.getMessage().contains("Invalid BuildMyCommand TextMate bundle path"));
    }

    @Test
    void textMateBundleProviderWrapsInvalidJarFileUrls() throws IOException {
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> BuildMyCommandTextMateBundleProvider.bundle(
                "BuildMyCommand Route DSL",
                new java.net.URL("jar:file:/broken[plugin.jar!/textmate/buildmycommand-route")
            ));

        assertTrue(exception.getMessage().contains("Invalid BuildMyCommand TextMate bundle path"));
    }

    @Test
    void textMateBundleProviderRejectsEscapingJarEntries() throws IOException {
        Path jar = Files.createTempFile("buildmycommand-textmate-escape", ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("textmate/buildmycommand-route/../evil.txt"));
            output.write("nope".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> BuildMyCommandTextMateBundleProvider.bundle(
                "BuildMyCommand Route DSL",
                new java.net.URL("jar:" + jar.toUri() + "!/textmate/buildmycommand-route")
            ));

        assertTrue(exception.getMessage().contains("Invalid BuildMyCommand TextMate bundle path"));
    }

    @Test
    void routeLexerHighlightsArgumentsTypesOptionsAndGreedyMarkers() {
        BuildMyCommandRouteSyntaxHighlighter highlighter = new BuildMyCommandRouteSyntaxHighlighter();
        Lexer lexer = highlighter.getHighlightingLexer();

        lexer.start("give|grant <target:String> <reason:String...> [--duration:Integer|-d]");

        assertHighlights(lexer, highlighter, "give", "ENTITY_NAME_FUNCTION_LITERAL_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "grant", "ENTITY_NAME_FUNCTION_LITERAL_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "target", "VARIABLE_PARAMETER_ARGUMENT_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "String", "STORAGE_TYPE_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "...", "KEYWORD_OPERATOR_GREEDY_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "--duration", "ENTITY_NAME_OPTION_LONG_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "Integer", "STORAGE_TYPE_BUILDMYCOMMAND_ROUTE");
        assertHighlights(lexer, highlighter, "-d", "ENTITY_NAME_OPTION_ALIAS_BUILDMYCOMMAND_ROUTE");
    }

    @Test
    void requirementLexerHighlightsPermissionNodesOperatorsAndGroups() {
        BuildMyCommandRequirementSyntaxHighlighter highlighter = new BuildMyCommandRequirementSyntaxHighlighter();
        Lexer lexer = highlighter.getHighlightingLexer();

        lexer.start("staff && (!banned || owner)");

        assertRequirementHighlights(lexer, highlighter, "staff", "ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REQUIRE");
        assertRequirementHighlights(lexer, highlighter, "&&", "KEYWORD_OPERATOR_BUILDMYCOMMAND_REQUIRE");
        assertRequirementHighlights(lexer, highlighter, "(", "PUNCTUATION_GROUP_BUILDMYCOMMAND_REQUIRE");
        assertRequirementHighlights(lexer, highlighter, "!", "KEYWORD_OPERATOR_BUILDMYCOMMAND_REQUIRE");
    }

    @Test
    void routeDslValidationFindsMalformedTypesAliasesAndOrdering() {
        List<BuildMyCommandRouteDsl.Issue> issues = BuildMyCommandRouteDsl.validate(
            "ban|block <target:Player> [duration:Integer] <reason:Integer...> [--duration:Unknown|-d] [--silent|-d]"
        );

        assertTrue(issues.stream().anyMatch(issue -> issue.message().equals("Unknown argument type: Player")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().equals("Required argument cannot follow an optional argument")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().equals("Greedy arguments must use String")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().equals("Unknown option type: Unknown")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().equals("Duplicate alias: -d")));
        assertTrue(BuildMyCommandRouteDsl.bindingNames(
            "ban <target:String> [reason:String...] [--duration:Integer|-d] [--silent|-s]"
        ).containsAll(List.of("target", "reason", "duration", "silent")));
        assertTrue(BuildMyCommandRouteDsl.bindingNames("<> [] [--]").isEmpty());
    }

    @Test
    void routeInspectionDocumentsRouteCtxContract() throws IOException {
        Path root = Path.of("").toAbsolutePath().getParent().getParent();
        String inspection = Files.readString(root.resolve(
            "modules/intellij-plugin/src/main/java/dev/riege/buildmycommand/intellij/BuildMyCommandRouteInspection.java"
        ));

        assertTrue(inspection.contains("RouteCtx"));
        assertTrue(inspection.contains(BuildMyCommandRouteInspection.ROUTE_CONTEXT_REQUIRED));
        assertTrue(inspection.contains(BuildMyCommandRouteInspection.ROUTE_CONTEXT_TYPE_REQUIRED));
        assertTrue(inspection.contains(BuildMyCommandRouteInspection.ROUTE_CTX_FORBIDDEN_OUTSIDE_ROUTE_DSL));
        assertTrue(inspection.contains(BuildMyCommandRouteInspection.PATH_LITERAL_ONLY));
    }

    @Test
    void routeDslCompletionSuggestsTypesAndOptionAliases() {
        assertTrue(BuildMyCommandRouteDsl.completionsFor("give <target:", 13).contains("String"));
        assertTrue(BuildMyCommandRouteDsl.completionsFor("give [--amount:Integer|-", 24).contains("a"));
        assertTrue(BuildMyCommandRouteDsl.completionsFor("give [--", 8).contains("duration"));
    }

    @Test
    void projectCanDeclareAndGenerateRequiredIntelliJPlugin() throws IOException {
        Path root = Path.of("").toAbsolutePath().getParent().getParent();
        String externalDependencies = Files.readString(root.resolve(".idea/externalDependencies.xml"));
        String powerShellScript = Files.readString(root.resolve("scripts/setup-intellij-plugin.ps1"));
        String shellScript = Files.readString(root.resolve("scripts/setup-intellij-plugin.sh"));
        String buildScript = Files.readString(root.resolve("build.gradle.kts"));
        String intellijBuildScript = Files.readString(root.resolve("modules/intellij-plugin/build.gradle.kts"));
        String ciWorkflow = Files.readString(root.resolve(".github/workflows/ci.yml"));

        assertTrue(externalDependencies.contains("<component name=\"ExternalDependencies\">"));
        assertTrue(externalDependencies.contains("<plugin id=\"dev.riege.buildmycommand.dsl\" min-version=\"0.3.3\" />"));
        assertTrue(powerShellScript.contains("dev.riege.buildmycommand.dsl"));
        assertTrue(powerShellScript.contains(":intellij-plugin:buildPlugin"));
        assertTrue(powerShellScript.contains("$Install"));
        assertTrue(powerShellScript.contains("Expand-Archive"));
        assertTrue(shellScript.contains("dev.riege.buildmycommand.dsl"));
        assertTrue(shellScript.contains(":intellij-plugin:buildPlugin"));
        assertTrue(shellScript.contains("skip-build"));
        assertTrue(shellScript.contains("--install"));
        assertTrue(shellScript.contains("unzip -q"));
        assertTrue(buildScript.contains("setupIntellijPlugin"));
        assertTrue(buildScript.contains("installIntellijPluginLocal"));
        assertTrue(intellijBuildScript.contains("tasks.patchPluginXml"));
        assertTrue(intellijBuildScript.contains("tasks.runPluginVerifier"));
        assertTrue(intellijBuildScript.contains("pluginVerifierIdeVersions"));
        assertTrue(intellijBuildScript.contains("IC-2024.1"));
        assertTrue(intellijBuildScript.contains("IC-2025.2"));
        assertTrue(intellijBuildScript.contains("sinceBuild.set(\"241\")"));
        assertTrue(intellijBuildScript.contains("untilBuild.set(\"\")"));
        assertTrue(ciWorkflow.contains(":intellij-plugin:runPluginVerifier"));
        assertTrue(ciWorkflow.contains("Verify IntelliJ IDEA Community compatibility"));
    }

    private static String resource(String path) throws IOException {
        var stream = IntellijPluginResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void assertHighlights(
        Lexer lexer,
        BuildMyCommandRouteSyntaxHighlighter highlighter,
        String text,
        String key
    ) {
        while (lexer.getTokenType() != null) {
            String tokenText = lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
            if (tokenText.equals(text)) {
                TextAttributesKey[] highlights = highlighter.getTokenHighlights(lexer.getTokenType());
                assertTrue(
                    Arrays.stream(highlights).anyMatch(highlight -> highlight.getExternalName().equals(key)),
                    "Expected " + text + " to use " + key
                );
                lexer.advance();
                return;
            }
            lexer.advance();
        }
        assertTrue(false, "Missing token: " + text);
    }

    private static void assertRequirementHighlights(
        Lexer lexer,
        BuildMyCommandRequirementSyntaxHighlighter highlighter,
        String text,
        String key
    ) {
        while (lexer.getTokenType() != null) {
            String tokenText = lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
            if (tokenText.equals(text)) {
                TextAttributesKey[] highlights = highlighter.getTokenHighlights(lexer.getTokenType());
                assertTrue(
                    Arrays.stream(highlights).anyMatch(highlight -> highlight.getExternalName().equals(key)),
                    "Expected " + text + " to use " + key
                );
                lexer.advance();
                return;
            }
            lexer.advance();
        }
        assertTrue(false, "Missing token: " + text);
    }
}
