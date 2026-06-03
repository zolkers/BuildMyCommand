package dev.riege.buildmycommand.intellij;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandRequirementTokenType extends IElementType {
    static final BuildMyCommandRequirementTokenType PERMISSION = new BuildMyCommandRequirementTokenType("PERMISSION");
    static final BuildMyCommandRequirementTokenType OPERATOR = new BuildMyCommandRequirementTokenType("OPERATOR");
    static final BuildMyCommandRequirementTokenType NEGATION = new BuildMyCommandRequirementTokenType("NEGATION");
    static final BuildMyCommandRequirementTokenType GROUPING = new BuildMyCommandRequirementTokenType("GROUPING");

    private BuildMyCommandRequirementTokenType(@NotNull @NonNls String debugName) {
        super(debugName, BuildMyCommandRequirementLanguage.INSTANCE);
    }
}
