package dev.riege.buildmycommand.intellij;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntellijPluginResourcesTest {
    @Test
    void pluginDeclaresIntelliLangInjectionConfiguration() throws IOException {
        String pluginXml = resource("META-INF/plugin.xml");
        String injections = resource("buildmycommandInjections.xml");

        assertTrue(pluginXml.contains("org.intellij.intelliLang"));
        assertTrue(pluginXml.contains("buildmycommandInjections.xml"));
        assertTrue(injections.contains("dev.riege.buildmycommand.annotation.Command"));
        assertTrue(injections.contains("dev.riege.buildmycommand.api.CommandRegistry"));
        assertTrue(injections.contains("route"));
    }

    private static String resource(String path) throws IOException {
        var stream = IntellijPluginResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
