package dev.riege.buildmycommand.intellij;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;

public final class BuildMyCommandRouteInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
                if (!BuildMyCommandRouteLiteralMatcher.isRouteLiteral(expression)) {
                    return;
                }
                TextRange range = BuildMyCommandRouteLiteralMatcher.contentRange(expression);
                String route = expression.getText().substring(range.getStartOffset(), range.getEndOffset());
                for (BuildMyCommandRouteDsl.Issue issue : BuildMyCommandRouteDsl.validate(route)) {
                    holder.registerProblem(
                        expression,
                        TextRange.create(range.getStartOffset() + issue.start(), range.getStartOffset() + issue.end()),
                        issue.message()
                    );
                }
            }
        };
    }
}
