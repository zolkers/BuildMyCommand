/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import org.jetbrains.plugins.textmate.api.TextMateBundleProvider;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BuildMyCommandTextMateBundleProvider implements TextMateBundleProvider {
    private static final String BUNDLE_RESOURCE = "/textmate/buildmycommand-route";

    @Override
    public List<PluginBundle> getBundles() {
        var url = Objects.requireNonNull(
            BuildMyCommandTextMateBundleProvider.class.getResource(BUNDLE_RESOURCE),
            BUNDLE_RESOURCE
        );
        return List.of(bundle("BuildMyCommand Route DSL", url));
    }

    static PluginBundle bundle(String name, java.net.URL url) {
        try {
            if ("jar".equals(url.getProtocol())) {
                return new PluginBundle(name, extractedJarBundle(url));
            }
            return new PluginBundle(name, Path.of(url.toURI()));
        } catch (IOException | URISyntaxException | RuntimeException exception) {
            throw new IllegalStateException("Invalid BuildMyCommand TextMate bundle path", exception);
        }
    }

    private static Path extractedJarBundle(java.net.URL url) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        String entryName = Objects.requireNonNull(connection.getEntryName(), "Missing TextMate bundle entry");
        Path target = Files.createTempDirectory("buildmycommand-textmate-");
        try (var jar = new JarFile(jarFilePath(connection).toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(entryName + "/") || entry.isDirectory()) {
                    continue;
                }
                Path relative = Path.of(name.substring(entryName.length() + 1));
                Path output = target.resolve(relative).normalize();
                if (!output.startsWith(target)) {
                    throw new IOException("Invalid TextMate bundle entry: " + name);
                }
                Files.createDirectories(output.getParent());
                try (var input = jar.getInputStream(entry)) {
                    Files.copy(input, output);
                }
            }
        }
        return target;
    }

    private static Path jarFilePath(JarURLConnection connection) throws IOException {
        try {
            return Path.of(connection.getJarFileURL().toURI());
        } catch (URISyntaxException exception) {
            throw new IOException("Invalid TextMate bundle jar URL", exception);
        }
    }
}
