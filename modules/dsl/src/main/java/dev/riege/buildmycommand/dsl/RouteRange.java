package dev.riege.buildmycommand.dsl;

public record RouteRange(Long min, Long max) {
    public RouteRange {
        if (min == null && max == null) {
            throw new IllegalArgumentException("route range must declare at least one bound");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("route range min must be <= max");
        }
    }

    String display() {
        String minText = min == null ? "" : String.valueOf(min);
        String maxText = max == null ? "" : String.valueOf(max);
        return "{" + minText + ".." + maxText + "}";
    }
}
