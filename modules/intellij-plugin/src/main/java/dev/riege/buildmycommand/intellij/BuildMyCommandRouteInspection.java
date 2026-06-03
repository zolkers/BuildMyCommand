package dev.riege.buildmycommand.intellij;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

public final class BuildMyCommandRouteInspection extends AbstractBaseJavaLocalInspectionTool {
    static final String ROUTE_CONTEXT_REQUIRED =
        "Route DSL methods must declare exactly one @RouteCtx CommandContext parameter";
    static final String ROUTE_CONTEXT_TYPE_REQUIRED =
        "@RouteCtx parameter must be dev.riege.buildmycommand.api.CommandContext";
    static final String ROUTE_PARAMETER_ANNOTATIONS_FORBIDDEN =
        "Route DSL methods should read values from @RouteCtx CommandContext instead of @Arg/@Option/@Flag parameters";
    static final String SUB_ROUTE_OWNER_REQUIRED =
        "@SubRoute methods must be declared inside a @Command class";
    static final String SUBCOMMAND_OWNER_REQUIRED =
        "@Subcommand methods must be declared inside a @Command class";
    static final String COMMAND_LITERAL_ONLY =
        "@Command only accepts one literal; use @Route for route DSL";
    static final String SUBCOMMAND_LITERAL_ONLY =
        "@Subcommand only accepts one literal; use @SubRoute for route DSL";
    static final String ROUTE_CTX_FORBIDDEN_OUTSIDE_ROUTE_DSL =
        "@RouteCtx is only valid on @Route/@SubRoute methods";
    private static final String ROUTE = "dev.riege.buildmycommand.annotation.Route";
    private static final String SUB_ROUTE = "dev.riege.buildmycommand.annotation.SubRoute";
    private static final String ROUTE_CTX = "dev.riege.buildmycommand.annotation.RouteCtx";
    private static final String ARG = "dev.riege.buildmycommand.annotation.Arg";
    private static final String OPTION = "dev.riege.buildmycommand.annotation.Option";
    private static final String FLAG = "dev.riege.buildmycommand.annotation.Flag";
    private static final String COMMAND = "dev.riege.buildmycommand.annotation.Command";
    private static final String SUBCOMMAND = "dev.riege.buildmycommand.annotation.Subcommand";
    private static final String COMMAND_CONTEXT = "dev.riege.buildmycommand.api.CommandContext";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                boolean route = hasAnnotation(method, ROUTE);
                boolean subRoute = hasAnnotation(method, SUB_ROUTE);
                PsiAnnotation command = findAnnotation(method.getModifierList().getAnnotations(), COMMAND);
                PsiAnnotation subcommand = findAnnotation(method.getModifierList().getAnnotations(), SUBCOMMAND);
                if (command != null) {
                    inspectLiteralOnly(command, COMMAND_LITERAL_ONLY, holder);
                }
                if (subcommand != null) {
                    inspectLiteralOnly(subcommand, SUBCOMMAND_LITERAL_ONLY, holder);
                    if (!hasOwnerCommand(method)) {
                        holder.registerProblem(method.getNameIdentifier(), SUBCOMMAND_OWNER_REQUIRED);
                    }
                }
                if (subRoute && !hasOwnerCommand(method)) {
                    holder.registerProblem(method.getNameIdentifier(), SUB_ROUTE_OWNER_REQUIRED);
                }
                if (!route && !subRoute) {
                    inspectRouteContextOutsideRouteDsl(method, holder);
                    return;
                }

                int routeContextCount = 0;
                for (PsiParameter parameter : method.getParameterList().getParameters()) {
                    if (hasAnnotation(parameter, ROUTE_CTX)) {
                        routeContextCount++;
                        if (!COMMAND_CONTEXT.equals(parameter.getType().getCanonicalText())) {
                            holder.registerProblem(parameter, ROUTE_CONTEXT_TYPE_REQUIRED);
                        }
                    }
                    if (hasAnnotation(parameter, ARG) || hasAnnotation(parameter, OPTION) || hasAnnotation(parameter, FLAG)) {
                        holder.registerProblem(parameter, ROUTE_PARAMETER_ANNOTATIONS_FORBIDDEN);
                    }
                }
                if (routeContextCount != 1) {
                    holder.registerProblem(method.getNameIdentifier(), ROUTE_CONTEXT_REQUIRED);
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

    private static void inspectRouteContextOutsideRouteDsl(PsiMethod method, ProblemsHolder holder) {
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (hasAnnotation(parameter, ROUTE_CTX)) {
                holder.registerProblem(parameter, ROUTE_CTX_FORBIDDEN_OUTSIDE_ROUTE_DSL);
            }
        }
    }

    private static void inspectLiteralOnly(PsiAnnotation annotation, String message, ProblemsHolder holder) {
        String value = annotationValue(annotation);
        if (value != null && isRouteDsl(value)) {
            holder.registerProblem(annotation, message);
        }
    }

    private static boolean hasOwnerCommand(PsiMethod method) {
        PsiClass owner = method.getContainingClass();
        return owner != null && hasAnnotation(owner.getModifierList().getAnnotations(), COMMAND);
    }

    private static boolean hasAnnotation(PsiMethod method, String qualifiedName) {
        return hasAnnotation(method.getModifierList().getAnnotations(), qualifiedName);
    }

    private static boolean hasAnnotation(PsiParameter parameter, String qualifiedName) {
        return hasAnnotation(parameter.getModifierList().getAnnotations(), qualifiedName);
    }

    private static boolean hasAnnotation(PsiAnnotation[] annotations, String qualifiedName) {
        return findAnnotation(annotations, qualifiedName) != null;
    }

    private static PsiAnnotation findAnnotation(PsiAnnotation[] annotations, String qualifiedName) {
        String shortName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        for (PsiAnnotation annotation : annotations) {
            String actual = annotation.getQualifiedName();
            if (qualifiedName.equals(actual) || shortName.equals(actual)) {
                return annotation;
            }
        }
        return null;
    }

    private static String annotationValue(PsiAnnotation annotation) {
        PsiLiteralExpression literal = null;
        if (annotation.findDeclaredAttributeValue("value") instanceof PsiLiteralExpression valueLiteral) {
            literal = valueLiteral;
        }
        return literal != null && literal.getValue() instanceof String value ? value : null;
    }

    private static boolean isRouteDsl(String value) {
        String trimmed = value.trim();
        return trimmed.isBlank()
            || !trimmed.equals(value)
            || trimmed.split("\\s+").length != 1
            || trimmed.contains("<")
            || trimmed.contains(">")
            || trimmed.contains("[")
            || trimmed.contains("]")
            || trimmed.contains("|");
    }
}
