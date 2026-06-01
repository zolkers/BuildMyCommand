package dev.riege.buildmycommand.api;

public final class Commands {
    private Commands() {
    }

    public static CommandNode.Builder literal(String literal) {
        return new CommandNode.Builder(literal);
    }
}
