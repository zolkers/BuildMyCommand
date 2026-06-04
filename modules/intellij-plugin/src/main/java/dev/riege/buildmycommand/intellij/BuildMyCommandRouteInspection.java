/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

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
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    static final String DEEP_SUBCOMMAND_TREE_DISCOURAGED =
        "Deep @Subcommand nesting is supported but @SubRoute route DSL is recommended for command paths";
    static final String PATH_LITERAL_ONLY =
        "Builder path() only accepts literal segments; use subRoute() for route DSL arguments, options, or aliases";
    static final String SUGGEST_TARGET_NOT_FOUND =
        "@Suggest name does not match any @Route/@SubRoute argument or option in this class";
    static final String COMMAND_METHOD_VISIBILITY_REQUIRED =
        "Annotated command methods must be public or package-private";
    static final String COMMAND_METHOD_RETURN_REQUIRED =
        "Annotated command methods must return dev.riege.buildmycommand.api.CommandResult";
    static final String COMMAND_METHOD_PARAMETER_REQUIRED =
        "Annotated command method parameters must be dev.riege.buildmycommand.api.CommandContext or @RouteCtx CommandContext";
    static final String SUGGEST_PROVIDER_SIGNATURE_REQUIRED =
        "@Suggest providers must return java.util.List or SuggestionSet and accept zero args, ArgumentParseContext, or SuggestionContext";
    static final String SUGGEST_VALUE_REQUIRED =
        "@Suggest value must not be blank";
    static final String COOLDOWN_POSITIVE_REQUIRED =
        "@Cooldown value must be positive";
    static final String METADATA_NOT_BLANK =
        "BuildMyCommand annotation metadata must not be blank";
    static final String MIDDLEWARE_TYPE_REQUIRED =
        "@Middleware classes must implement dev.riege.buildmycommand.api.CommandMiddleware";
    static final String MIDDLEWARE_NO_ARG_CONSTRUCTOR_REQUIRED =
        "@Middleware classes must declare a no-arg constructor";
    private static final String ROUTE = "dev.riege.buildmycommand.annotation.Route";
    private static final String SUB_ROUTE = "dev.riege.buildmycommand.annotation.SubRoute";
    private static final String ROUTE_CTX = "dev.riege.buildmycommand.annotation.RouteCtx";
    private static final String COMMAND = "dev.riege.buildmycommand.annotation.Command";
    private static final String SUBCOMMAND = "dev.riege.buildmycommand.annotation.Subcommand";
    private static final String PERMISSION = "dev.riege.buildmycommand.annotation.Permission";
    private static final String REQUIRE = "dev.riege.buildmycommand.annotation.Require";
    private static final String SUGGEST = "dev.riege.buildmycommand.annotation.Suggest";
    private static final String COMMAND_CONTEXT = "dev.riege.buildmycommand.api.CommandContext";
    private static final String COMMAND_RESULT = "dev.riege.buildmycommand.api.CommandResult";
    private static final String ARGUMENT_PARSE_CONTEXT = "dev.riege.buildmycommand.api.ArgumentParseContext";
    private static final String SUGGESTION_CONTEXT = "dev.riege.buildmycommand.api.SuggestionContext";
    private static final String SUGGESTION_SET = "dev.riege.buildmycommand.api.SuggestionSet";
    private static final String COMMAND_MIDDLEWARE = "dev.riege.buildmycommand.api.CommandMiddleware";
    private static final String COOLDOWN = "dev.riege.buildmycommand.annotation.Cooldown";
    private static final String MIDDLEWARE = "dev.riege.buildmycommand.annotation.Middleware";
    private static final Set<String> SINGLE_STRING_METADATA = Set.of(
        "dev.riege.buildmycommand.annotation.Description",
        "dev.riege.buildmycommand.annotation.Permission",
        "dev.riege.buildmycommand.annotation.Require",
        "dev.riege.buildmycommand.annotation.Usage",
        "dev.riege.buildmycommand.annotation.CommandGroup",
        "dev.riege.buildmycommand.annotation.Command",
        "dev.riege.buildmycommand.annotation.Subcommand",
        "dev.riege.buildmycommand.annotation.Route",
        "dev.riege.buildmycommand.annotation.SubRoute"
    );
    private static final Set<String> ARRAY_STRING_METADATA = Set.of(
        "dev.riege.buildmycommand.annotation.Alias",
        "dev.riege.buildmycommand.annotation.Example"
    );

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitClass(@NotNull PsiClass psiClass) {
                inspectClassAnnotationContracts(psiClass, holder);
                PsiAnnotation subcommand = findAnnotation(psiClass.getModifierList().getAnnotations(), SUBCOMMAND);
                if (subcommand == null) {
                    return;
                }
                inspectLiteralOnly(subcommand, SUBCOMMAND_LITERAL_ONLY, holder);
                if (!hasCommandTreeOwner(psiClass)) {
                    holder.registerProblem(psiClass.getNameIdentifier(), SUBCOMMAND_OWNER_REQUIRED);
                }
                if (isNestedSubcommandGroup(psiClass)) {
                    holder.registerProblem(psiClass.getNameIdentifier(), DEEP_SUBCOMMAND_TREE_DISCOURAGED);
                }
            }

            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                boolean route = hasAnnotation(method, ROUTE);
                boolean subRoute = hasAnnotation(method, SUB_ROUTE);
                PsiAnnotation command = findAnnotation(method.getModifierList().getAnnotations(), COMMAND);
                PsiAnnotation subcommand = findAnnotation(method.getModifierList().getAnnotations(), SUBCOMMAND);
                inspectMethodAnnotationContracts(method, holder);
                if (command != null) {
                    inspectLiteralOnly(command, COMMAND_LITERAL_ONLY, holder);
                }
                if (subcommand != null) {
                    inspectLiteralOnly(subcommand, SUBCOMMAND_LITERAL_ONLY, holder);
                    if (!hasCommandTreeOwner(method)) {
                        holder.registerProblem(method.getNameIdentifier(), SUBCOMMAND_OWNER_REQUIRED);
                    }
                    if (isDeepSubcommandLeaf(method)) {
                        holder.registerProblem(method.getNameIdentifier(), DEEP_SUBCOMMAND_TREE_DISCOURAGED);
                    }
                }
                if (subRoute && !hasCommandTreeOwner(method)) {
                    holder.registerProblem(method.getNameIdentifier(), SUB_ROUTE_OWNER_REQUIRED);
                }
                inspectSuggestBinding(method, holder);
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
                    for (BuildMyCommandRouteDsl.Issue issue : routeIssues(expression, route)) {
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

    private static void inspectClassAnnotationContracts(PsiClass psiClass, ProblemsHolder holder) {
        inspectMetadataAnnotations(psiClass.getModifierList().getAnnotations(), holder);
        inspectCooldown(psiClass.getModifierList().getAnnotations(), holder);
        inspectMiddleware(psiClass.getModifierList().getAnnotations(), psiClass, holder);
    }

    private static void inspectMethodAnnotationContracts(PsiMethod method, ProblemsHolder holder) {
        inspectMetadataAnnotations(method.getModifierList().getAnnotations(), holder);
        inspectCooldown(method.getModifierList().getAnnotations(), holder);
        inspectMiddleware(method.getModifierList().getAnnotations(), method, holder);
        inspectSuggestProvider(method, holder);
        if (isAnnotatedCommandMethod(method)) {
            inspectCommandMethod(method, holder);
        }
    }

    private static void inspectCommandMethod(PsiMethod method, ProblemsHolder holder) {
        PsiElement methodElement = problemElement(method.getNameIdentifier(), method);
        if (method.hasModifierProperty(PsiModifier.PRIVATE) || method.hasModifierProperty(PsiModifier.PROTECTED)) {
            holder.registerProblem(methodElement, COMMAND_METHOD_VISIBILITY_REQUIRED);
        }
        PsiType returnType = method.getReturnType();
        if (returnType == null || !COMMAND_RESULT.equals(returnType.getCanonicalText())) {
            holder.registerProblem(methodElement, COMMAND_METHOD_RETURN_REQUIRED);
        }
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (!COMMAND_CONTEXT.equals(parameter.getType().getCanonicalText())) {
                holder.registerProblem(parameter, COMMAND_METHOD_PARAMETER_REQUIRED);
            }
        }
    }

    private static void inspectSuggestProvider(PsiMethod method, ProblemsHolder holder) {
        PsiAnnotation suggest = findAnnotation(method.getModifierList().getAnnotations(), SUGGEST);
        if (suggest == null) {
            return;
        }
        String value = annotationValue(suggest);
        if (value == null || value.isBlank()) {
            holder.registerProblem(suggest, SUGGEST_VALUE_REQUIRED);
        }
        PsiType returnType = method.getReturnType();
        boolean validReturn = returnType != null
            && (SUGGESTION_SET.equals(returnType.getCanonicalText())
            || returnType.getCanonicalText().startsWith("java.util.List"));
        PsiParameter[] parameters = method.getParameterList().getParameters();
        boolean validParameters = parameters.length == 0
            || (parameters.length == 1 && isSuggestionParameter(parameters[0]));
        if (!validReturn || !validParameters) {
            holder.registerProblem(problemElement(method.getNameIdentifier(), method), SUGGEST_PROVIDER_SIGNATURE_REQUIRED);
        }
    }

    private static boolean isSuggestionParameter(PsiParameter parameter) {
        String canonicalText = parameter.getType().getCanonicalText();
        return ARGUMENT_PARSE_CONTEXT.equals(canonicalText) || SUGGESTION_CONTEXT.equals(canonicalText);
    }

    private static boolean isAnnotatedCommandMethod(PsiMethod method) {
        return hasAnnotation(method, ROUTE)
            || hasAnnotation(method, SUB_ROUTE)
            || hasAnnotation(method, COMMAND)
            || hasAnnotation(method, SUBCOMMAND);
    }

    private static void inspectCooldown(PsiAnnotation[] annotations, ProblemsHolder holder) {
        PsiAnnotation cooldown = findAnnotation(annotations, COOLDOWN);
        if (cooldown == null) {
            return;
        }
        PsiAnnotationMemberValue value = cooldown.findDeclaredAttributeValue("value");
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof Number number
            && number.longValue() <= 0) {
            holder.registerProblem(literal, COOLDOWN_POSITIVE_REQUIRED);
        }
    }

    private static void inspectMiddleware(PsiAnnotation[] annotations, PsiElement owner, ProblemsHolder holder) {
        PsiAnnotation middleware = findAnnotation(annotations, MIDDLEWARE);
        if (middleware == null) {
            return;
        }
        for (PsiClass middlewareClass : middlewareClasses(middleware)) {
            if (!isMiddlewareClass(middlewareClass, owner)) {
                holder.registerProblem(middleware.getNameReferenceElement(), MIDDLEWARE_TYPE_REQUIRED);
            }
            if (!hasNoArgConstructor(middlewareClass)) {
                holder.registerProblem(middleware.getNameReferenceElement(), MIDDLEWARE_NO_ARG_CONSTRUCTOR_REQUIRED);
            }
        }
    }

    private static List<PsiClass> middlewareClasses(PsiAnnotation annotation) {
        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
        if (value instanceof PsiClassObjectAccessExpression classObject) {
            return classFrom(classObject);
        }
        if (value instanceof PsiArrayInitializerMemberValue initializer) {
            return java.util.Arrays.stream(initializer.getInitializers())
                .filter(PsiClassObjectAccessExpression.class::isInstance)
                .map(PsiClassObjectAccessExpression.class::cast)
                .flatMap(classObject -> classFrom(classObject).stream())
                .toList();
        }
        return List.of();
    }

    private static List<PsiClass> classFrom(PsiClassObjectAccessExpression classObject) {
        PsiType type = classObject.getOperand().getType();
        PsiClass psiClass = com.intellij.psi.util.PsiTypesUtil.getPsiClass(type);
        return psiClass == null ? List.of() : List.of(psiClass);
    }

    private static boolean isMiddlewareClass(PsiClass psiClass, PsiElement context) {
        PsiClass middleware = JavaPsiFacade.getInstance(context.getProject())
            .findClass(COMMAND_MIDDLEWARE, context.getResolveScope());
        return middleware == null || psiClass.isInheritor(middleware, true);
    }

    private static boolean hasNoArgConstructor(PsiClass psiClass) {
        PsiMethod[] constructors = psiClass.getConstructors();
        if (constructors.length == 0) {
            return true;
        }
        for (PsiMethod constructor : constructors) {
            if (constructor.getParameterList().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void inspectMetadataAnnotations(PsiAnnotation[] annotations, ProblemsHolder holder) {
        for (PsiAnnotation annotation : annotations) {
            if (matchesAny(annotation, SINGLE_STRING_METADATA)) {
                inspectBlankStringValue(annotation.findDeclaredAttributeValue("value"), holder);
            }
            if (matchesAny(annotation, ARRAY_STRING_METADATA)) {
                inspectBlankStringValues(annotation.findDeclaredAttributeValue("value"), holder);
            }
        }
    }

    private static void inspectBlankStringValues(PsiAnnotationMemberValue value, ProblemsHolder holder) {
        if (value instanceof PsiArrayInitializerMemberValue initializer) {
            for (PsiAnnotationMemberValue member : initializer.getInitializers()) {
                inspectBlankStringValue(member, holder);
            }
            return;
        }
        inspectBlankStringValue(value, holder);
    }

    private static void inspectBlankStringValue(PsiAnnotationMemberValue value, ProblemsHolder holder) {
        if (value instanceof PsiLiteralExpression literal
            && literal.getValue() instanceof String string
            && string.isBlank()) {
            holder.registerProblem(literal, METADATA_NOT_BLANK);
        }
    }

    private static boolean matchesAny(PsiAnnotation annotation, Set<String> qualifiedNames) {
        for (String qualifiedName : qualifiedNames) {
            if (hasAnnotationName(annotation, qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    private static void inspectSuggestBinding(PsiMethod method, ProblemsHolder holder) {
        PsiAnnotation suggest = findAnnotation(method.getModifierList().getAnnotations(), SUGGEST);
        if (suggest == null) {
            return;
        }
        String value = annotationValue(suggest);
        if (value == null || value.isBlank()) {
            return;
        }
        PsiClass owner = method.getContainingClass();
        if (owner == null) {
            return;
        }
        Set<String> available = routeBindingNames(owner);
        if (!available.isEmpty() && !available.contains(value)) {
            holder.registerProblem(suggest, SUGGEST_TARGET_NOT_FOUND);
        }
    }

    private static Set<String> routeBindingNames(PsiClass owner) {
        Set<String> names = new HashSet<>();
        for (PsiMethod method : owner.getMethods()) {
            routeValue(method, ROUTE).ifPresent(route -> names.addAll(BuildMyCommandRouteDsl.bindingNames(route)));
            routeValue(method, SUB_ROUTE).ifPresent(route -> names.addAll(BuildMyCommandRouteDsl.bindingNames(route)));
        }
        return Set.copyOf(names);
    }

    private static java.util.Optional<String> routeValue(PsiMethod method, String annotationName) {
        PsiAnnotation annotation = findAnnotation(method.getModifierList().getAnnotations(), annotationName);
        return annotation == null ? java.util.Optional.empty() : java.util.Optional.ofNullable(annotationValue(annotation));
    }

    private static Iterable<BuildMyCommandRouteDsl.Issue> routeIssues(PsiLiteralExpression expression, String route) {
        if (!BuildMyCommandRouteLiteralMatcher.isLiteralPathLiteral(expression)) {
            return BuildMyCommandRouteDsl.validate(route);
        }
        TextRange pathDslRange = pathDslRange(route);
        if (pathDslRange.isEmpty()) {
            return BuildMyCommandRouteDsl.validate(route);
        }
        return java.util.List.of(new BuildMyCommandRouteDsl.Issue(
            pathDslRange.getStartOffset(),
            pathDslRange.getEndOffset(),
            PATH_LITERAL_ONLY
        ));
    }

    private static TextRange pathDslRange(String route) {
        int start = -1;
        int end = -1;
        for (int index = 0; index < route.length(); index++) {
            char character = route.charAt(index);
            if (character == '<' || character == '>' || character == '[' || character == ']'
                || character == '|') {
                if (start < 0) {
                    start = index;
                }
                end = index + 1;
            }
        }
        return start < 0 ? TextRange.EMPTY_RANGE : TextRange.create(start, end);
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

    private static boolean isNestedSubcommandGroup(PsiClass owner) {
        PsiClass current = owner.getContainingClass();
        while (current != null) {
            if (hasAnnotation(current.getModifierList().getAnnotations(), SUBCOMMAND)) {
                return true;
            }
            if (hasAnnotation(current.getModifierList().getAnnotations(), COMMAND)) {
                return false;
            }
            current = current.getContainingClass();
        }
        return false;
    }

    private static boolean isDeepSubcommandLeaf(PsiMethod method) {
        PsiClass current = method.getContainingClass();
        while (current != null) {
            if (hasAnnotation(current.getModifierList().getAnnotations(), SUBCOMMAND)) {
                return true;
            }
            if (hasAnnotation(current.getModifierList().getAnnotations(), COMMAND)) {
                return false;
            }
            current = current.getContainingClass();
        }
        return false;
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

    private static PsiElement problemElement(PsiElement preferred, PsiElement fallback) {
        return preferred == null ? fallback : preferred;
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
