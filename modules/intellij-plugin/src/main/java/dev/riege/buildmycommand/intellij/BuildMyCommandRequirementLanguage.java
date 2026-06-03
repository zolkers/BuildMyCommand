package dev.riege.buildmycommand.intellij;

import com.intellij.lang.Language;

public final class BuildMyCommandRequirementLanguage extends Language {
    public static final BuildMyCommandRequirementLanguage INSTANCE = new BuildMyCommandRequirementLanguage();

    private BuildMyCommandRequirementLanguage() {
        super("BuildMyCommandRequirement");
    }
}
