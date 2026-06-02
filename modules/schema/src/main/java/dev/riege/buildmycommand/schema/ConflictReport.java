package dev.riege.buildmycommand.schema;

import java.util.List;

public record ConflictReport(List<String> conflicts) {
    public ConflictReport {
        conflicts = List.copyOf(conflicts);
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }
}
