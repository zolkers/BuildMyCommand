package dev.riege.buildmycommand.dsl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void detectsConflictsWhenOneRouteUsesAliasAsPrimaryLiteral() {
        List<RouteConflict> conflicts = RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban|block <target:String>"),
            RouteParser.parse("block <target:String>")
        ));

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void detectsConflictsWhenNestedLiteralAliasesOverlap() {
        List<RouteConflict> conflicts = RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("user rank|roles set"),
            RouteParser.parse("user roles set")
        ));

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void detectsConflictsForSameArgumentShapeWithDifferentBindingNames() {
        List<RouteConflict> conflicts = RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban <user:String>")
        ));

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void doesNotDetectConflictsForDifferentRuntimeArgumentTypes() {
        List<RouteConflict> conflicts = RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban <target:int>")
        ));

        assertEquals(List.of(), conflicts);
    }

    @Test
    void parsesOptionalGreedyArgumentsFlagsValueOptionsAndOpenRanges() {
        RoutePattern route = RouteParser.parse(
            "give <target:String> [reason:String...] [--silent|-s] [--amount:Integer|-a] <score:long{..100}>"
        );

        assertEquals(new ArgumentRouteStep("reason", RouteType.runtime("String", String.class),
            RouteArgumentKind.OPTIONAL_GREEDY), route.steps().get(1));
        assertEquals(new OptionRouteStep("silent", RouteType.runtime("Boolean", Boolean.class), "s",
            RouteOptionKind.FLAG), route.steps().get(2));
        assertEquals(new OptionRouteStep("amount", RouteType.runtime("Integer", Integer.class), "a",
            RouteOptionKind.VALUE), route.steps().get(3));
        assertEquals(new ArgumentRouteStep("score", RouteType.constrained("long", long.class, new RouteRange(null,
            100L)), RouteArgumentKind.REQUIRED), route.steps().get(4));
        assertEquals("give <target:String> [reason:String...] [--silent|-s] [--amount:Integer|-a] <score:long{..100}>",
            RouteCanonicalizer.display(route));
        assertEquals(RouteArgumentKind.GREEDY,
            ((ArgumentRouteStep) RouteParser.parse("say <message:String...>").steps().get(0)).kind());
        assertEquals(RouteArgumentKind.OPTIONAL,
            ((ArgumentRouteStep) RouteParser.parse("say [message:String]").steps().get(0)).kind());
        assertEquals("team-name",
            ((ArgumentRouteStep) RouteParser.parse("x <team-name:String>").steps().get(0)).name());
        assertEquals("team_name",
            ((ArgumentRouteStep) RouteParser.parse("x <team_name:String>").steps().get(0)).name());
    }

    @Test
    void parsesAllBuiltInRuntimeTypesAndRangeDisplays() {
        List<String> names = List.of(
            "String", "Integer", "int", "Long", "long", "Float", "float", "Double", "double", "Boolean",
            "boolean", "UUID", "Duration", "LocalDate", "LocalDateTime", "Path", "URI", "URL"
        );

        for (String name : names) {
            ArgumentRouteStep step = (ArgumentRouteStep) RouteParser.parse("x <v:" + name + ">").steps().get(0);
            assertEquals(name, step.type().name());
        }

        assertEquals("{1..}", new RouteRange(1L, null).display());
        assertTrue(RouteType.constrained("int", int.class, new RouteRange(1L, null)).constrained());
        assertFalse(RouteType.runtime("String", String.class).constrained());
        assertEquals(Integer.class, ((ArgumentRouteStep) RouteParser.parse("x <v:Integer{1..2}>").steps().get(0))
            .type().runtimeType());
        assertEquals(Long.class, ((ArgumentRouteStep) RouteParser.parse("x <v:Long{1..2}>").steps().get(0))
            .type().runtimeType());
        assertEquals(Float.class, ((ArgumentRouteStep) RouteParser.parse("x <v:Float{1..2}>").steps().get(0))
            .type().runtimeType());
        assertEquals(float.class, ((ArgumentRouteStep) RouteParser.parse("x <v:float{1..2}>").steps().get(0))
            .type().runtimeType());
        assertEquals(Double.class, ((ArgumentRouteStep) RouteParser.parse("x <v:Double{1..2}>").steps().get(0))
            .type().runtimeType());
        assertEquals(double.class, ((ArgumentRouteStep) RouteParser.parse("x <v:double{1..2}>").steps().get(0))
            .type().runtimeType());
    }

    @Test
    void rejectsMalformedPatternsNamesTypesOptionsRangesAndEnums() {
        List<String> invalid = List.of(
            "",
            "   ",
            "<target:String>",
            "cmd <target:String",
            "cmd target:String>",
            "cmd [name:String",
            "cmd name:String]",
            "cmd <bad>",
            "cmd <:String>",
            "cmd <name:>",
            "cmd <name:String:extra>",
            "cmd <name:int...>",
            "cmd [--]",
            "cmd [--name|-long]",
            "cmd [--name|--n]",
            "cmd [--name|-]",
            "cmd [--name|-n|-x]",
            "cmd [--amount:]",
            "cmd [--amount:int:extra]",
            "cmd [--bad name]",
            "cmd [--amount:String{1..2}]",
            "cmd <mode:enum(A>",
            "cmd <amount:int{1..2>",
            "cmd <amount:int{1..2..3}>",
            "cmd <amount:int{..}>",
            "cmd <amount:int{3..1}>",
            "cmd <mode:enum()>",
            "cmd <mode:enum(EASY,)>",
            "cmd <mode:enum(BAD VALUE)>",
            "cmd <x:MissingType>",
            "cmd [--flag] literal"
        );

        invalid.forEach(pattern -> assertThrows(IllegalArgumentException.class, () -> RouteParser.parse(pattern),
            pattern));
        assertThrows(NullPointerException.class, () -> RouteParser.parse(null));
        assertThrows(NullPointerException.class, () -> new LiteralRouteStep(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new LiteralRouteStep("literal", List.of("")));
        assertThrows(NullPointerException.class, () -> new LiteralRouteStep("literal", null));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentRouteStep("bad name",
            RouteType.runtime("String", String.class), RouteArgumentKind.REQUIRED));
        assertThrows(NullPointerException.class, () -> new ArgumentRouteStep("name", null,
            RouteArgumentKind.REQUIRED));
        assertThrows(NullPointerException.class, () -> new ArgumentRouteStep("name",
            RouteType.runtime("String", String.class), null));
        assertThrows(IllegalArgumentException.class, () -> new OptionRouteStep("flag",
            RouteType.runtime("Boolean", Boolean.class), "bad alias", RouteOptionKind.FLAG));
        assertThrows(NullPointerException.class, () -> new OptionRouteStep("flag", RouteType.runtime("Boolean",
            Boolean.class), null, null));
        assertThrows(NullPointerException.class, () -> new RoutePattern("root", null, List.of()));
        assertThrows(NullPointerException.class, () -> new RoutePattern("root", List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new RoutePattern("", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RoutePattern("root", List.of(""), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RouteType("", String.class, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new RouteType("broken", null, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new RouteType("enum", null, List.of("A"),
            new RouteRange(1L, 1L)));
        assertThrows(NullPointerException.class, () -> RouteType.runtime("String", null));
        assertThrows(NullPointerException.class, () -> RouteType.constrained("int", int.class, null));
        assertThrows(NullPointerException.class, () -> RouteType.inlineEnum(null));
    }

    @Test
    void canonicalizesOptionsAndDetectsOnlyActualOverlaps() {
        RoutePattern optionRoute = RouteParser.parse("give [--amount:int|-a]");
        OptionRouteStep option = (OptionRouteStep) optionRoute.steps().get(0);

        assertEquals("[--amount:int|-a]", RouteCanonicalizer.canonicalStep(option));
        assertEquals("[--silent]", RouteCanonicalizer.displayStep(new OptionRouteStep("silent",
            RouteType.runtime("Boolean", Boolean.class), null, RouteOptionKind.FLAG)));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("kick <target:String>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban <target:String> force")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban literal")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban [--silent]"),
            RouteParser.parse("ban literal")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban [--silent]"),
            RouteParser.parse("ban [--silent:String]")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban user"),
            RouteParser.parse("ban rank")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban [--silent|-s]"),
            RouteParser.parse("ban [--force|-f]")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban user"),
            RouteParser.parse("ban <target:String>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban [target:String]")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban [--amount:int]"),
            RouteParser.parse("ban [--amount:String]")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:enum(Ada)>"),
            RouteParser.parse("ban <target:String>")
        )));
        assertFalse(RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <amount:int{1..3}>"),
            RouteParser.parse("ban <amount:int{1..3}>")
        )).isEmpty());
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban [--target:String]")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban [--silent]"),
            RouteParser.parse("ban <target:String>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <target:String>"),
            RouteParser.parse("ban <target:enum(Ada)>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <amount:int>"),
            RouteParser.parse("ban <amount:enum(ONE)>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <amount:int>"),
            RouteParser.parse("ban <amount:int{1..3}>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <amount:int{1..3}>"),
            RouteParser.parse("ban <amount:int>")
        )));
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <amount:int{1..3}>"),
            RouteParser.parse("ban <amount:int{1..4}>")
        )));
        assertFalse(RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <mode:enum(EASY,NORMAL)>"),
            RouteParser.parse("ban <mode:enum(NORMAL,HARD)>")
        )).isEmpty());
        assertEquals(List.of(), RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban <mode:enum(EASY)>"),
            RouteParser.parse("ban <mode:enum(HARD)>")
        )));
        assertFalse(RouteConflictAnalyzer.findConflicts(List.of(
            RouteParser.parse("ban [--silent|-s]"),
            RouteParser.parse("ban [--silent]")
        )).isEmpty());
    }

    @Test
    void privateTokenHelpersRejectShapesThatPublicDslCannotProduce() throws Exception {
        Method parseLongOptionName = RouteParser.class.getDeclaredMethod("parseLongOptionName", String.class,
            String.class);
        Method parseAlias = RouteParser.class.getDeclaredMethod("parseAlias", String.class, String.class);
        Method parseRange = RouteParser.class.getDeclaredMethod("parseRange", String.class, String.class);
        parseLongOptionName.setAccessible(true);
        parseAlias.setAccessible(true);
        parseRange.setAccessible(true);

        assertReflectiveIllegalArgument(parseLongOptionName, "name", "[name:String]");
        assertReflectiveIllegalArgument(parseAlias, "n", "[--name|n]");
        assertReflectiveIllegalArgument(parseRange, "broken", "int{broken}");
    }

    private static void assertReflectiveIllegalArgument(Method method, String raw, String token) {
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
            () -> method.invoke(null, raw, token));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }
}
