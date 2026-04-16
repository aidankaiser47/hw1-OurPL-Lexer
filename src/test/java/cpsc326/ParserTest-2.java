package cpsc326;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParserTest {

    @BeforeEach
    void resetFlags() {
        OurPL.hadError = false;
        OurPL.hadRuntimeError = false;
    }

    private List<Token> lex(String source) {
        return new Lexer(source).scanTokens();
    }

    private Expr parse(String source) {
        OurPL.hadError = false;
        Parser parser = new Parser(lex(source));
        return parser.parse();
    }

    private String parseToAst(String source) {
        Expr expr = parse(source);
        assertNotNull(expr, "Parser returned null for valid expression.");
        assertFalse(OurPL.hadError, "Parser reported an error for valid expression.");
        return new ASTPrinter().print(expr);
    }

    private ParseOutcome parseWithCapturedErr(String source) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            Expr expr = parse(source);
            return new ParseOutcome(expr, err.toString());
        } finally {
            System.setErr(originalErr);
        }
    }

    private EvalOutcome interpret(String source) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        try {
            new Interpreter().interpret(parse(source));
            return new EvalOutcome(out.toString().trim(), err.toString().trim());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static Stream<Arguments> primaryLiteralCases() {
        return Stream.of(
            arguments("123", "123.0"),
            arguments("\"abc\"", "abc"),
            arguments("true", "true"),
            arguments("false", "false"),
            arguments("nil", "nil")
        );
    }

    @ParameterizedTest
    @MethodSource("primaryLiteralCases")
    void parsesPrimaryLiterals(String source, String expectedAst) {
        assertEquals(expectedAst, parseToAst(source));
    }

    private static Stream<Arguments> unaryCases() {
        return Stream.of(
            arguments("-123", "(- 123.0)"),
            arguments("!true", "(! true)"),
            arguments("!!false", "(! (! false))"),
            arguments("!-3", "(! (- 3.0))")
        );
    }

    @ParameterizedTest
    @MethodSource("unaryCases")
    void parsesUnaryExpressions(String source, String expectedAst) {
        assertEquals(expectedAst, parseToAst(source));
    }

    private static Stream<Arguments> flatBinaryCases() {
        return Stream.of(
            arguments("1 + 2", "(+ 1.0 2.0)"),
            arguments("1 - 2", "(- 1.0 2.0)"),
            arguments("2 * 3", "(* 2.0 3.0)"),
            arguments("8 / 4", "(/ 8.0 4.0)"),
            arguments("1 > 2", "(> 1.0 2.0)"),
            arguments("1 >= 2", "(>= 1.0 2.0)"),
            arguments("1 < 2", "(< 1.0 2.0)"),
            arguments("1 <= 2", "(<= 1.0 2.0)"),
            arguments("1 == 2", "(== 1.0 2.0)"),
            arguments("1 != 2", "(!= 1.0 2.0)")
        );
    }

    @ParameterizedTest
    @MethodSource("flatBinaryCases")
    void parsesSingleBinaryExpressions(String source, String expectedAst) {
        assertEquals(expectedAst, parseToAst(source));
    }

    @Test
    void respectsFactorPrecedenceOverTerm() {
        assertEquals("(+ 1.0 (* 2.0 3.0))", parseToAst("1 + 2 * 3"));
    }

    @Test
    void respectsParenthesesOverDefaultPrecedence() {
        assertEquals("(* (group (+ 1.0 2.0)) 3.0)", parseToAst("(1 + 2) * 3"));
    }

    @Test
    void respectsComparisonBeforeEquality() {
        assertEquals("(== (< 1.0 2.0) (> 3.0 4.0))", parseToAst("1 < 2 == 3 > 4"));
    }

    @Test
    void termOperatorsAreLeftAssociative() {
        assertEquals("(- (- 10.0 3.0) 2.0)", parseToAst("10 - 3 - 2"));
    }

    @Test
    void factorOperatorsAreLeftAssociative() {
        assertEquals("(* (/ 8.0 4.0) 2.0)", parseToAst("8 / 4 * 2"));
    }

    @Test
    void equalityOperatorsAreLeftAssociative() {
        assertEquals("(!= (== 1.0 2.0) 3.0)", parseToAst("1 == 2 != 3"));
    }

    @Test
    void handlesCommentsAndWhitespaceDuringParse() {
        assertEquals("(+ 1.0 2.0)", parseToAst(" \n 1 + 2   # trailing comment"));
    }

    @Test
    void parsesComplexMixedExpressionFromAssignmentStyleInput() {
        assertEquals(
            "(+ (- (+ 12.0 45.0) (/ 56.0 34.0)) (* 12.0 67.0))",
            parseToAst("12 + 45 - 56 / 34 + 12 * 67")
        );
    }

    @Test
    void reportsMissingRightParen() {
        ParseOutcome out = parseWithCapturedErr("(1 + 2");
        assertNull(out.expr);
        assertTrue(OurPL.hadError);
    }

    @Test
    void reportsEmptyInputAsExpectExpression() {
        ParseOutcome out = parseWithCapturedErr("");
        assertNull(out.expr);
        assertTrue(OurPL.hadError);
    }

    @Test
    void reportsUnexpectedTokenAtStart() {
        ParseOutcome out = parseWithCapturedErr(")");
        assertNull(out.expr);
        assertTrue(OurPL.hadError);
    }

    @Test
    void reportsUnaryPlusAsInvalidPrimary() {
        ParseOutcome out = parseWithCapturedErr("+1");
        assertNull(out.expr);
        assertTrue(OurPL.hadError);
    }

    @Test
    void printsPrecedenceUsingPrefixNotation() {
        Expr expr = parse("1 + 2 * 3 - 4");
        assertEquals("(- (+ 1.0 (* 2.0 3.0)) 4.0)", new ASTPrinter().print(expr));
    }

    @Test
    void printsGroupingExplicitly() {
        Expr expr = parse("(1 + 2) * 3");
        assertEquals("(* (group (+ 1.0 2.0)) 3.0)", new ASTPrinter().print(expr));
    }

    @Test
    void printsUnaryExpressions() {
        Expr expr = parse("!(-3)");
        assertEquals("(! (group (- 3.0)))", new ASTPrinter().print(expr));
    }

    @Test
    void printsComparisonInsideEquality() {
        Expr expr = parse("1 < 2 == 3 > 4");
        assertEquals("(== (< 1.0 2.0) (> 3.0 4.0))", new ASTPrinter().print(expr));
    }

    private static Stream<Arguments> evaluationCases() {
        return Stream.of(
            arguments("1 + 2 * 3", "7"),
            arguments("(1 + 2) * 3", "9"),
            arguments("8 / 4 * 2", "4"),
            arguments("1 < 2", "true"),
            arguments("1 == 2", "false"),
            arguments("nil == nil", "true"),
            arguments("!false", "true"),
            arguments("!!false", "false"),
            arguments("-3", "-3"),
            arguments("\"ab\" + \"cd\"", "abcd")
        );
    }

    @ParameterizedTest
    @MethodSource("evaluationCases")
    void evaluatesExpressions(String source, String expectedOutput) {
        EvalOutcome out = interpret(source);
        assertFalse(OurPL.hadRuntimeError, "Unexpected runtime error for valid expression.");
        assertFalse(OurPL.hadError, "Unexpected parse error for valid expression.");
        assertTrue(out.stderr.isEmpty(), "Expected no stderr output.");
        assertTrue(out.stdout.endsWith(expectedOutput), "Unexpected interpreter output.");
    }

    @Test
    void stringifiesWholeNumberDoubleWithoutDecimalSuffix() {
        EvalOutcome out = interpret("1");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("1", out.stdout);
        assertTrue(out.stderr.isEmpty());
    }

    @Test
    void stringifiesNilAsNil() {
        EvalOutcome out = interpret("nil");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("nil", out.stdout);
        assertTrue(out.stderr.isEmpty());
    }

    @Test
    void evaluatesBooleanEqualityOutput() {
        EvalOutcome out = interpret("true == false");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("false", out.stdout);
        assertTrue(out.stderr.isEmpty());
    }

    @Test
    void runtimeErrorForUnaryMinusOnString() {
        EvalOutcome out = interpret("-\"abc\"");
        assertTrue(OurPL.hadRuntimeError);
        assertTrue(out.stdout.isEmpty());
        assertTrue(out.stderr.contains("Operand must be a number."));
        assertTrue(out.stderr.contains("[line 1]"));
    }

    @Test
    void runtimeErrorForInvalidAdditionOperands() {
        EvalOutcome out = interpret("1 + \"abc\"");
        assertTrue(OurPL.hadRuntimeError);
        assertTrue(out.stdout.isEmpty());
        assertTrue(out.stderr.contains("Operands must be two numbers or two strings."));
        assertTrue(out.stderr.contains("[line 1]"));
    }

    @Test
    void runtimeErrorForNumericComparisonOnBoolean() {
        EvalOutcome out = interpret("true < 1");
        assertTrue(OurPL.hadRuntimeError);
        assertTrue(out.stdout.isEmpty());
        assertTrue(out.stderr.contains("[line 1]"));
    }

    @Test
    void runtimeErrorForSubtractionOnMixedTypes() {
        EvalOutcome out = interpret("\"abc\" - 1");
        assertTrue(OurPL.hadRuntimeError);
        assertTrue(out.stdout.isEmpty());
        assertTrue(out.stderr.contains("[line 1]"));
    }

    @Test
    void runtimeErrorStoresTokenAndMessage() {
        Token token = new Token(TokenType.PLUS, "+", null, 7);
        RuntimeError error = new RuntimeError(token, "bad operands");

        assertSame(token, error.token);
        assertEquals("bad operands", error.getMessage());
        assertEquals(7, error.token.line);
        assertEquals(TokenType.PLUS, error.token.type);
    }

    private static class ParseOutcome {
        final Expr expr;
        final String stderr;

        ParseOutcome(Expr expr, String stderr) {
            this.expr = expr;
            this.stderr = stderr;
        }
    }

    private static final class EvalOutcome {
        final String stdout;
        final String stderr;

        EvalOutcome(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
