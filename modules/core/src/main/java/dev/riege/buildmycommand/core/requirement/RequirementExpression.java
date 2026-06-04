/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.requirement;

import java.util.Objects;
import java.util.function.Predicate;

public final class RequirementExpression {
    private RequirementExpression() {
    }

    public static boolean evaluate(String expression, Predicate<String> permissionChecker) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        try {
            Parser parser = new Parser(expression, permissionChecker);
            boolean result = parser.parseExpression();
            parser.skipWhitespace();
            return result && parser.atEnd();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static final class Parser {
        private final String expression;
        private final Predicate<String> permissionChecker;
        private int index;

        private Parser(String expression, Predicate<String> permissionChecker) {
            this.expression = expression;
            this.permissionChecker = permissionChecker;
        }

        private boolean parseExpression() {
            return parseOr();
        }

        private boolean parseOr() {
            boolean result = parseAnd();
            while (true) {
                skipWhitespace();
                if (!consume("||")) {
                    return result;
                }
                boolean right = parseAnd();
                result = result || right;
            }
        }

        private boolean parseAnd() {
            boolean result = parseUnary();
            while (true) {
                skipWhitespace();
                if (!consume("&&")) {
                    return result;
                }
                boolean right = parseUnary();
                result = result && right;
            }
        }

        private boolean parseUnary() {
            skipWhitespace();
            if (consume("!")) {
                return !parseUnary();
            }
            return parsePrimary();
        }

        private boolean parsePrimary() {
            skipWhitespace();
            if (consume("(")) {
                boolean result = parseExpression();
                skipWhitespace();
                require(consume(")"));
                return result;
            }
            String permission = parsePermission();
            require(!permission.isBlank());
            return permissionChecker.test(permission);
        }

        private String parsePermission() {
            int start = index;
            while (!atEnd()) {
                char character = expression.charAt(index);
                if (Character.isWhitespace(character) || character == '(' || character == ')'
                    || startsWith("&&") || startsWith("||") || character == '!') {
                    break;
                }
                index++;
            }
            return expression.substring(start, index);
        }

        private boolean consume(String token) {
            if (!startsWith(token)) {
                return false;
            }
            index += token.length();
            return true;
        }

        private boolean startsWith(String token) {
            return expression.startsWith(token, index);
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
        }

        private boolean atEnd() {
            return index >= expression.length();
        }

        private static void require(boolean condition) {
            if (!condition) {
                throw new IllegalArgumentException("invalid requirement expression");
            }
        }
    }
}
