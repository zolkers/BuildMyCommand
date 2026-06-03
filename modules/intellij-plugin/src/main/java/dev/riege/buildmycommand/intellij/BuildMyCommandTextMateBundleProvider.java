package dev.riege.buildmycommand.intellij;

import org.jetbrains.plugins.textmate.api.TextMateBundleProvider;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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
            return new PluginBundle(name, Path.of(url.toURI()));
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid BuildMyCommand TextMate bundle path", exception);
        }
    }
}
