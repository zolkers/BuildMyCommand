package dev.riege.buildmycommand.intellij;

import com.intellij.lang.Language;

public final class BuildMyCommandRouteLanguage extends Language {
    public static final BuildMyCommandRouteLanguage INSTANCE = new BuildMyCommandRouteLanguage();

    private BuildMyCommandRouteLanguage() {
        super("BuildMyCommandRoute");
    }
}
