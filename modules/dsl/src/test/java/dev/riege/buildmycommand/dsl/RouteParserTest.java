package dev.riege.buildmycommand.dsl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteParserTest {
    @Test
    void parsesLiteralAliasesAndRequiredArgument() {
        RoutePattern route = RouteParser.parse("ban|block <target:String>");

        assertEquals("ban", route.rootLiteral());
        assertEquals(List.of("block"), route.rootAliases());
        assertEquals(List.of(new ArgumentRouteStep(
            "target",
            RouteType.runtime("String", String.class),
            RouteArgumentKind.REQUIRED
        )), route.steps());
        assertEquals("ban|block <target:String>", RouteCanonicalizer.display(route));
    }

    @Test
    void canonicalizesNestedLiteralAliases() {
        RoutePattern route = RouteParser.parse("rank|roles set|put");

        assertEquals("rank|roles set|put", RouteCanonicalizer.display(route));
        assertEquals("rank|roles put|set", RouteCanonicalizer.canonical(route));
    }

    @Test
    void exposesInlineEnumMetadataForAnalysis() {
        RoutePattern route = RouteParser.parse("mode <mode:enum(EASY,NORMAL,HARD)>");
        ArgumentRouteStep step = (ArgumentRouteStep) route.steps().get(0);

        assertEquals("mode", step.name());
        assertEquals(RouteArgumentKind.REQUIRED, step.kind());
        assertEquals(List.of("EASY", "NORMAL", "HARD"), step.type().enumValues());
        assertEquals("<mode:enum(EASY,NORMAL,HARD)>", RouteCanonicalizer.displayStep(step));
    }

    @Test
    void exposesNumericRangeMetadataForAnalysis() {
        RoutePattern route = RouteParser.parse("give <amount:int{1..64}>");
        ArgumentRouteStep step = (ArgumentRouteStep) route.steps().get(0);

        assertEquals("amount", step.name());
        assertEquals(int.class, step.type().runtimeType());
        assertEquals(new RouteRange(1L, 64L), step.type().range());
        assertEquals("<amount:int{1..64}>", RouteCanonicalizer.displayStep(step));
    }

    @Test
    void detectsConflictsForRoutesEquivalentViaAliases() {
        List<RouteConflict> conflicts = RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban|block <target:String>"),
            RouteParser.parse("block|ban <target:String>")
        ));

        assertFalse(conflicts.isEmpty());
        assertEquals("ban|block <target:String>", conflicts.get(0).canonicalRoute());
    }
}
