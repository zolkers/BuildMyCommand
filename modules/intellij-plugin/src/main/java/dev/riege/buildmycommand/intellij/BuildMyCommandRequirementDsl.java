package dev.riege.buildmycommand.intellij;

import java.util.ArrayList;
import java.util.List;

public final class BuildMyCommandRequirementDsl {
    static final String MISSING_OPERAND = "Requirement expression is missing an operand";
    static final String MISSING_OPERATOR = "Requirement expression is missing an operator";
    static final String UNCLOSED_GROUP = "Requirement expression has an unclosed group";
    static final String UNEXPECTED_GROUP_END = "Requirement expression has an unexpected group end";

    private BuildMyCommandRequirementDsl() {
    }

    public static List<Issue> validate(String expression) {
        Parser parser = new Parser(expression);
        parser.parseExpression();
        parser.skipWhitespace();
        if (parser.issues.isEmpty() && !parser.atEnd()) {
            if (parser.peek() == ')') {
                parser.issue(UNEXPECTED_GROUP_END);
            } else {
                parser.issue(MISSING_OPERATOR);
            }
        }
        return parser.issues;
    }

    public static boolean looksBoolean(String expression) {
        return expression.contains("&&")
            || expression.contains("||")
            || expression.contains("!")
            || expression.contains("(")
            || expression.contains(")");
    }

    public record Issue(int start, int end, String message) {
    }

    private static final class Parser {
        private final String expression;
        private final List<Issue> issues = new ArrayList<>();
        private int index;

        private Parser(String expression) {
            this.expression = expression;
        }

        private void parseExpression() {
            parseOr();
        }

        private void parseOr() {
            parseAnd();
            while (issues.isEmpty()) {
                skipWhitespace();
                if (!consume("||")) {
                    return;
                }
                parseAnd();
            }
        }

        private void parseAnd() {
            parseUnary();
            while (issues.isEmpty()) {
                skipWhitespace();
                if (!consume("&&")) {
                    return;
                }
                parseUnary();
            }
        }

        private void parseUnary() {
            skipWhitespace();
            while (consume("!")) {
                skipWhitespace();
            }
            parsePrimary();
        }

        private void parsePrimary() {
            skipWhitespace();
            if (atEnd()) {
                issue(MISSING_OPERAND);
                return;
            }
            if (consume("(")) {
                parseExpression();
                if (!issues.isEmpty()) {
                    return;
                }
                skipWhitespace();
                if (!consume(")")) {
                    issue(UNCLOSED_GROUP);
                }
                return;
            }
            if (peek() == ')' || startsWith("&&") || startsWith("||")) {
                issue(MISSING_OPERAND);
                return;
            }
            parsePermission();
        }

        private void parsePermission() {
            int start = index;
            while (!atEnd()) {
                char character = peek();
                if (Character.isWhitespace(character) || character == '(' || character == ')'
                    || startsWith("&&") || startsWith("||") || character == '!') {
                    break;
                }
                index++;
            }
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
            while (!atEnd() && Character.isWhitespace(peek())) {
                index++;
            }
        }

        private char peek() {
            return expression.charAt(index);
        }

        private boolean atEnd() {
            return index >= expression.length();
        }

        private void issue(String message) {
            int start = Math.min(index, expression.length());
            int end = Math.min(expression.length(), start + 1);
            issues.add(new Issue(start, end, message));
        }
    }
}
