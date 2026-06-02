package dev.riege.buildmycommand.intellij;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandRouteTokenType extends IElementType {
    static final BuildMyCommandRouteTokenType LITERAL = new BuildMyCommandRouteTokenType("LITERAL");
    static final BuildMyCommandRouteTokenType ARGUMENT = new BuildMyCommandRouteTokenType("ARGUMENT");
    static final BuildMyCommandRouteTokenType TYPE = new BuildMyCommandRouteTokenType("TYPE");
    static final BuildMyCommandRouteTokenType OPTION_LONG = new BuildMyCommandRouteTokenType("OPTION_LONG");
    static final BuildMyCommandRouteTokenType OPTION_ALIAS = new BuildMyCommandRouteTokenType("OPTION_ALIAS");
    static final BuildMyCommandRouteTokenType GREEDY = new BuildMyCommandRouteTokenType("GREEDY");
    static final BuildMyCommandRouteTokenType MARKUP = new BuildMyCommandRouteTokenType("MARKUP");

    private BuildMyCommandRouteTokenType(@NotNull @NonNls String debugName) {
        super(debugName, BuildMyCommandRouteLanguage.INSTANCE);
    }
}
