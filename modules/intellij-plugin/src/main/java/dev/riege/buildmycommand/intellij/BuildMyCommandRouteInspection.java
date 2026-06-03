package dev.riege.buildmycommand.intellij;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;

public final class BuildMyCommandRouteInspection extends AbstractBaseJavaLocalInspectionTool {
    static final String ROUTE_CONTEXT_REQUIRED =
        "Route DSL methods must declare exactly one @RouteCtx CommandContext parameter";
    static final String ROUTE_CONTEXT_TYPE_REQUIRED =
        "@RouteCtx parameter must be dev.riege.buildmycommand.api.CommandContext";
    static final String SUB_ROUTE_OWNER_REQUIRED =
        "@SubRoute methods must be declared inside a @Command class";
    static final String SUBCOMMAND_OWNER_REQUIRED =
        "@Subcommand must be declared inside a @Command class or nested @Subcommand group";
    static final String COMMAND_LITERAL_ONLY =
        "@Command only accepts one literal; use @Route for route DSL";
    static final String SUBCOMMAND_LITERAL_ONLY =
        "@Subcommand only accepts one literal; use @SubRoute for route DSL";
    static final String ROUTE_CTX_FORBIDDEN_OUTSIDE_ROUTE_DSL =
        "@RouteCtx is only valid on @Route/@SubRoute methods";
    static final String PERMISSION_BOOLEAN_EXPRESSION =
        "@Permission accepts one permission node; use @Require for boolean expressions";
    private static final String ROUTE = "dev.riege.buildmycommand.annotation.Route";
    private static final String SUB_ROUTE = "dev.riege.buildmycommand.annotation.SubRoute";
    private static final String ROUTE_CTX = "dev.riege.buildmycommand.annotation.RouteCtx";
    private static final String COMMAND = "dev.riege.buildmycommand.annotation.Command";
    private static final String SUBCOMMAND = "dev.riege.buildmycommand.annotation.Subcommand";
    private static final String PERMISSION = "dev.riege.buildmycommand.annotation.Permission";
    private static final String REQUIRE = "dev.riege.buildmycommand.annotation.Require";
    private static final String COMMAND_CONTEXT = "dev.riege.buildmycommand.api.CommandContext";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitClass(@NotNull PsiClass psiClass) {
                PsiAnnotation subcommand = findAnnotation(psiClass.getModifierList().getAnnotations(), SUBCOMMAND);
                if (subcommand == null) {
                    return;
                }
                inspectLiteralOnly(subcommand, SUBCOMMAND_LITERAL_ONLY, holder);
                if (!hasCommandTreeOwner(psiClass)) {
                    holder.registerProblem(psiClass.getNameIdentifier(), SUBCOMMAND_OWNER_REQUIRED);
                }
            }

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
                    if (!hasCommandTreeOwner(method)) {
                        holder.registerProblem(method.getNameIdentifier(), SUBCOMMAND_OWNER_REQUIRED);
                    }
                }
                if (subRoute && !hasCommandTreeOwner(method)) {
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
                }
                if (routeContextCount != 1) {
                    holder.registerProblem(method.getNameIdentifier(), ROUTE_CONTEXT_REQUIRED);
                }
            }

            @Override
            public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
                if (BuildMyCommandRouteLiteralMatcher.isRouteLiteral(expression)) {
                    TextRange range = BuildMyCommandRouteLiteralMatcher.contentRange(expression);
                    String route = expression.getText().substring(range.getStartOffset(), range.getEndOffset());
                    for (BuildMyCommandRouteDsl.Issue issue : BuildMyCommandRouteDsl.validate(route)) {
                        holder.registerProblem(
                            expression,
                            TextRange.create(range.getStartOffset() + issue.start(), range.getStartOffset() + issue.end()),
                            issue.message()
                        );
                    }
                    return;
                }
                inspectAccessLiteral(expression, holder);
            }

            private void inspectAccessLiteral(PsiLiteralExpression expression, ProblemsHolder holder) {
                if (!(expression.getValue() instanceof String value)) {
                    return;
                }
                AccessLiteral accessLiteral = accessLiteral(expression);
                if (accessLiteral == AccessLiteral.NONE) {
                    return;
                }
                TextRange range = BuildMyCommandRouteLiteralMatcher.contentRange(expression);
                if (accessLiteral == AccessLiteral.PERMISSION && BuildMyCommandRequirementDsl.looksBoolean(value)) {
                    holder.registerProblem(
                        expression,
                        range,
                        PERMISSION_BOOLEAN_EXPRESSION,
                        new ConvertPermissionToRequireFix()
                    );
                }
                if (accessLiteral == AccessLiteral.REQUIREMENT) {
                    for (BuildMyCommandRequirementDsl.Issue issue : BuildMyCommandRequirementDsl.validate(value)) {
                        holder.registerProblem(
                            expression,
                            TextRange.create(range.getStartOffset() + issue.start(), range.getStartOffset() + issue.end()),
                            issue.message()
                        );
                    }
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

    private static boolean hasCommandTreeOwner(PsiMethod method) {
        PsiClass owner = method.getContainingClass();
        return owner != null && hasCommandTreeOwner(owner);
    }

    private static boolean hasCommandTreeOwner(PsiClass owner) {
        PsiClass current = owner;
        boolean sawSubcommandGroup = false;
        while (current != null) {
            if (hasAnnotation(current.getModifierList().getAnnotations(), COMMAND)) {
                return true;
            }
            if (current != owner && !hasAnnotation(current.getModifierList().getAnnotations(), SUBCOMMAND)) {
                return false;
            }
            sawSubcommandGroup = sawSubcommandGroup || hasAnnotation(current.getModifierList().getAnnotations(), SUBCOMMAND);
            current = current.getContainingClass();
        }
        return sawSubcommandGroup && false;
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

    private static AccessLiteral accessLiteral(PsiLiteralExpression expression) {
        PsiElement parent = expression.getParent();
        PsiAnnotation annotation = parentAnnotation(expression);
        if (annotation != null) {
            if (hasAnnotationName(annotation, PERMISSION)) {
                return AccessLiteral.PERMISSION;
            }
            if (hasAnnotationName(annotation, REQUIRE)) {
                return AccessLiteral.REQUIREMENT;
            }
        }
        if (!(parent instanceof PsiExpressionList arguments) || !(arguments.getParent() instanceof PsiMethodCallExpression call)) {
            return AccessLiteral.NONE;
        }
        PsiExpression[] expressions = arguments.getExpressions();
        if (expressions.length == 0 || expressions[0] != expression) {
            return AccessLiteral.NONE;
        }
        PsiReferenceExpression methodExpression = call.getMethodExpression();
        String method = methodExpression.getReferenceName();
        if ("permission".equals(method)) {
            return AccessLiteral.PERMISSION;
        }
        if ("requirement".equals(method)) {
            return AccessLiteral.REQUIREMENT;
        }
        return AccessLiteral.NONE;
    }

    private static PsiAnnotation parentAnnotation(PsiElement element) {
        PsiElement current = element.getParent();
        while (current != null && !(current instanceof PsiAnnotation) && !(current instanceof PsiExpressionList)) {
            current = current.getParent();
        }
        return current instanceof PsiAnnotation annotation ? annotation : null;
    }

    private static boolean hasAnnotationName(PsiAnnotation annotation, String qualifiedName) {
        String shortName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        String actual = annotation.getQualifiedName();
        String referenceName = annotation.getNameReferenceElement().getReferenceName();
        return qualifiedName.equals(actual) || shortName.equals(actual) || shortName.equals(referenceName);
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

    private enum AccessLiteral {
        NONE,
        PERMISSION,
        REQUIREMENT
    }

    @CoverageGenerated
    private static final class ConvertPermissionToRequireFix implements LocalQuickFix {
        @Override
        public @NotNull String getFamilyName() {
            return "Use @Require";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiElement parent = element.getParent();
            if (!(parent instanceof PsiNameValuePair pair) || !(pair.getParent() instanceof PsiAnnotation annotation)) {
                return;
            }
            String replacementText = annotation.getText().replaceFirst("Permission", "Require");
            PsiAnnotation replacement = JavaPsiFacade.getElementFactory(project)
                .createAnnotationFromText(replacementText, annotation);
            annotation.replace(replacement);
        }
    }
}
