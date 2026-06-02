package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.core.registry.*;
import java.util.ArrayList;
import java.util.List;

public final class CommandTokenizer {
    public TokenizeResult tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean tokenStarted = false;

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (escaped) {
                current.append(character);
                tokenStarted = true;
                escaped = false;
                continue;
            }

            if (character == '\\') {
                if (index + 1 >= input.length()) {
                    current.append(character);
                    tokenStarted = true;
                    continue;
                }
                char next = input.charAt(index + 1);
                if (next != '\\' && next != '"' && next != '\'' && !Character.isWhitespace(next)) {
                    current.append(character);
                    tokenStarted = true;
                    continue;
                }
                escaped = true;
                continue;
            }

            if ((character == '"' || character == '\'') && quote == 0) {
                quote = character;
                tokenStarted = true;
                continue;
            }

            if (character == quote) {
                quote = 0;
                tokenStarted = true;
                continue;
            }

            if (Character.isWhitespace(character) && quote == 0) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }

            current.append(character);
            tokenStarted = true;
        }

        if (quote != 0) {
            return TokenizeResult.failure("Unclosed quote");
        }

        if (tokenStarted) {
            tokens.add(current.toString());
        }

        return TokenizeResult.success(tokens);
    }
}
