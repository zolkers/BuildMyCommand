package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        assertTrue(pluginXml.contains("BuildMyCommandRouteInjector"));
        assertTrue(pluginXml.contains("BuildMyCommandRouteAnnotator"));
        assertTrue(pluginXml.contains("additionalTextAttributes"));
        assertTrue(injections.contains("dev.riege.buildmycommand.annotation.Command"));
        assertTrue(injections.contains("dev.riege.buildmycommand.annotation.Route"));
        assertTrue(injections.contains("dev.riege.buildmycommand.annotation.Subcommand"));
        assertTrue(injections.contains("dev.riege.buildmycommand.api.CommandRegistry"));
        assertTrue(injections.contains("language=\"BuildMyCommandRoute\""));
        assertTrue(injections.contains("route"));
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
        assertTrue(grammar.contains("String|Integer|int|Long|long|Double|double|Boolean|boolean|UUID"));
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
        assertTrue(dark.contains("<attributes>"));
        assertTrue(!dark.contains("<scheme"));
        assertTrue(dark.contains("TEXTMATE_SOURCE_BUILDMYCOMMAND_ROUTE"));
    }

    @Test
    void textMateBundleProviderReturnsBundledGrammarDirectory() {
        BuildMyCommandTextMateBundleProvider provider = new BuildMyCommandTextMateBundleProvider();

        List<org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle> bundles = provider.getBundles();

        assertTrue(bundles.stream().anyMatch(bundle -> bundle.getName().equals("BuildMyCommand Route DSL")));
        assertTrue(bundles.stream().anyMatch(bundle -> bundle.getPath().toString().contains("buildmycommand-route")));
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
    void projectCanDeclareAndGenerateRequiredIntelliJPlugin() throws IOException {
        Path root = Path.of("").toAbsolutePath().getParent().getParent();
        String externalDependencies = Files.readString(root.resolve(".idea/externalDependencies.xml"));
        String powerShellScript = Files.readString(root.resolve("scripts/setup-intellij-plugin.ps1"));
        String shellScript = Files.readString(root.resolve("scripts/setup-intellij-plugin.sh"));
        String buildScript = Files.readString(root.resolve("build.gradle.kts"));
        String intellijBuildScript = Files.readString(root.resolve("modules/intellij-plugin/build.gradle.kts"));

        assertTrue(externalDependencies.contains("<component name=\"ExternalDependencies\">"));
        assertTrue(externalDependencies.contains("<plugin id=\"dev.riege.buildmycommand.intellij\" min-version=\"0.1.0\" />"));
        assertTrue(powerShellScript.contains("dev.riege.buildmycommand.intellij"));
        assertTrue(powerShellScript.contains(":intellij-plugin:buildPlugin"));
        assertTrue(powerShellScript.contains("$Install"));
        assertTrue(powerShellScript.contains("Expand-Archive"));
        assertTrue(shellScript.contains("dev.riege.buildmycommand.intellij"));
        assertTrue(shellScript.contains(":intellij-plugin:buildPlugin"));
        assertTrue(shellScript.contains("skip-build"));
        assertTrue(shellScript.contains("--install"));
        assertTrue(shellScript.contains("unzip -q"));
        assertTrue(buildScript.contains("setupIntellijPlugin"));
        assertTrue(buildScript.contains("installIntellijPluginLocal"));
        assertTrue(intellijBuildScript.contains("tasks.patchPluginXml"));
        assertTrue(intellijBuildScript.contains("sinceBuild.set(\"241\")"));
        assertTrue(intellijBuildScript.contains("untilBuild.set(\"\")"));
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
                    java.util.Arrays.stream(highlights).anyMatch(highlight -> highlight.getExternalName().equals(key)),
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
