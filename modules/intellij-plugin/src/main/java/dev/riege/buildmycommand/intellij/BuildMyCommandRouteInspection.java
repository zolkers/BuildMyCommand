package dev.riege.buildmycommand.intellij;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

public final class BuildMyCommandRouteInspection extends AbstractBaseJavaLocalInspectionTool {
    static final String ROUTE_CONTEXT_REQUIRED =
        "Route DSL methods must declare exactly one @RouteCtx CommandContext parameter";
    static final String ROUTE_PARAMETER_ANNOTATIONS_FORBIDDEN =
        "Route DSL methods should read values from @RouteCtx CommandContext instead of @Arg/@Option/@Flag parameters";
    private static final String ROUTE = "dev.riege.buildmycommand.annotation.Route";
    private static final String SUBCOMMAND = "dev.riege.buildmycommand.annotation.Subcommand";
    private static final String ROUTE_CTX = "dev.riege.buildmycommand.annotation.RouteCtx";
    private static final String ARG = "dev.riege.buildmycommand.annotation.Arg";
    private static final String OPTION = "dev.riege.buildmycommand.annotation.Option";
    private static final String FLAG = "dev.riege.buildmycommand.annotation.Flag";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                if (!hasAnnotation(method, ROUTE) && !hasAnnotation(method, SUBCOMMAND)) {
                    return;
                }

                int routeContextCount = 0;
                for (PsiParameter parameter : method.getParameterList().getParameters()) {
                    if (hasAnnotation(parameter, ROUTE_CTX)) {
                        routeContextCount++;
                    }
                    if (hasAnnotation(parameter, ARG) || hasAnnotation(parameter, OPTION) || hasAnnotation(parameter, FLAG)) {
                        holder.registerProblem(parameter, ROUTE_PARAMETER_ANNOTATIONS_FORBIDDEN);
                    }
                }
                if (routeContextCount != 1) {
                    holder.registerProblem(method.getNameIdentifier() == null ? method : method.getNameIdentifier(),
                        ROUTE_CONTEXT_REQUIRED);
                }
            }

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

    private static boolean hasAnnotation(PsiMethod method, String qualifiedName) {
        return hasAnnotation(method.getModifierList().getAnnotations(), qualifiedName);
    }

    private static boolean hasAnnotation(PsiParameter parameter, String qualifiedName) {
        return hasAnnotation(parameter.getModifierList().getAnnotations(), qualifiedName);
    }

    private static boolean hasAnnotation(PsiAnnotation[] annotations, String qualifiedName) {
        String shortName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        for (PsiAnnotation annotation : annotations) {
            String actual = annotation.getQualifiedName();
            if (qualifiedName.equals(actual) || shortName.equals(actual)) {
                return true;
            }
        }
        return false;
    }
}
