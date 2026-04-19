package cpsc326;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterpreterAdditionalTests {

    @BeforeEach
    void resetFlags() {
        OurPL.hadError = false;
        OurPL.hadRuntimeError = false;
    }

    private List<Token> lex(String source) {
        return new Lexer(source).scanTokens();
    }

    private List<Stmt> parse(String source) {
        OurPL.hadError = false;
        return new Parser(lex(source)).parse();
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
            return new EvalOutcome(
                out.toString().replace("\r\n", "\n").trim(),
                err.toString().replace("\r\n", "\n").trim()
            );
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    // -------------------------------------------------------------------------
    // Scoping tests
    // -------------------------------------------------------------------------

    @Test
    void variableDefinedInBlockIsNotAccessibleAfterBlock() {
        interpret("{ var x = 10; } print x;");
        assertTrue(OurPL.hadRuntimeError,
            "Accessing a block-scoped variable after the block should cause a runtime error");
    }

    @Test
    void innerScopeDoesNotAffectOuterVariable() {
        EvalOutcome out = interpret("var x = 1; { var x = 99; } print x;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("1", out.stdout,
            "Inner block shadow of x should not change the outer x");
    }

    @Test
    void innerScopeCanReadOuterVariable() {
        EvalOutcome out = interpret("var x = 5; { print x; }");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("5", out.stdout,
            "Inner block should be able to read variables from an enclosing scope");
    }

    @Test
    void innerScopeCanMutateOuterVariable() {
        EvalOutcome out = interpret("var x = 1; { x = 42; } print x;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("42", out.stdout,
            "Assigning to an outer variable from an inner block should update the outer scope");
    }

    @Test
    void nestedBlocksShareEnclosingScope() {
        EvalOutcome out = interpret("var x = 0; { { x = 7; } print x; }");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("7", out.stdout,
            "Deeply nested assignment should propagate to the enclosing scope");
    }

    // -------------------------------------------------------------------------
    // Undefined variable tests
    // -------------------------------------------------------------------------

    @Test
    void accessingUndefinedVariableThrowsRuntimeError() {
        interpret("print undeclared;");
        assertTrue(OurPL.hadRuntimeError,
            "Reading an undeclared variable should throw a runtime error");
    }

    @Test
    void assigningToUndefinedVariableThrowsRuntimeError() {
        interpret("undeclared = 5;");
        assertTrue(OurPL.hadRuntimeError,
            "Assigning to an undeclared variable should throw a runtime error");
    }

    // -------------------------------------------------------------------------
    // Logical operator tests
    // -------------------------------------------------------------------------

    @Test
    void logicalOrReturnsTrueWhenLeftIsTrue() {
        EvalOutcome out = interpret("print true or false;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("true", out.stdout);
    }

    @Test
    void logicalOrReturnsTrueWhenRightIsTrue() {
        EvalOutcome out = interpret("print false or true;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("true", out.stdout);
    }

    @Test
    void logicalAndReturnsFalseWhenLeftIsFalse() {
        EvalOutcome out = interpret("print false and true;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("false", out.stdout);
    }

    @Test
    void logicalAndReturnsTrueWhenBothTrue() {
        EvalOutcome out = interpret("print true and true;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("true", out.stdout);
    }

    @Test
    void chainedLogicalAnd() {
        EvalOutcome out = interpret("var a = true; var b = true; var c = false; print a and b and c;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("false", out.stdout);
    }

    @Test
    void chainedLogicalOr() {
        EvalOutcome out = interpret("var a = false; var b = false; var c = true; print a or b or c;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("true", out.stdout);
    }

    // -------------------------------------------------------------------------
    // Control flow edge cases
    // -------------------------------------------------------------------------

    @Test
    void whileLoopThatNeverExecutes() {
        EvalOutcome out = interpret("var x = 0; while (false) { x = 1; } print x;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("0", out.stdout,
            "While loop body should not execute when condition starts false");
    }

    @Test
    void nestedWhileLoops() {
        EvalOutcome out = interpret(
            "var i = 0;" +
            "while (i < 2) {" +
            "  var j = 0;" +
            "  while (j < 2) {" +
            "    print i;" +
            "    j = j + 1;" +
            "  }" +
            "  i = i + 1;" +
            "}"
        );
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("0\n0\n1\n1", out.stdout);
    }

    @Test
    void emptyBlock() {
        EvalOutcome out = interpret("{}");
        assertFalse(OurPL.hadError);
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("", out.stdout, "Empty block should produce no output");
    }

    @Test
    void ifWithoutElse() {
        EvalOutcome out = interpret("if (false) print 1;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("", out.stdout, "If body should not run when condition is false with no else");
    }

    @Test
    void forLoopBodyAsBlock() {
        EvalOutcome out = interpret(
            "for (var i = 0; i < 3; i = i + 1) { print i; }"
        );
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("0\n1\n2", out.stdout,
            "For loop should work when body is a block statement");
    }

    // -------------------------------------------------------------------------
    // Binary and unary operation tests
    // -------------------------------------------------------------------------

    @Test
    void stringConcatenation() {
        EvalOutcome out = interpret("print \"hello\" + \" world\";");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("hello world", out.stdout);
    }

    @Test
    void mixingStringAndNumberThrowsRuntimeError() {
        interpret("print \"hello\" + 1;");
        assertTrue(OurPL.hadRuntimeError,
            "Adding a string and a number should throw a runtime error");
    }

    @Test
    void unaryMinusOnNumber() {
        EvalOutcome out = interpret("print -5;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("-5", out.stdout);
    }

    @Test
    void bangOnFalseReturnsTrue() {
        EvalOutcome out = interpret("print !false;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("true", out.stdout);
    }

    @Test
    void bangOnTrueReturnsFalse() {
        EvalOutcome out = interpret("print !true;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("false", out.stdout);
    }

    @Test
    void divisionWorks() {
        EvalOutcome out = interpret("print 10 / 2;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("5", out.stdout);
    }

    @Test
    void multiplicationWorks() {
        EvalOutcome out = interpret("print 3 * 4;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("12", out.stdout);
    }

    @Test
    void subtractionWorks() {
        EvalOutcome out = interpret("print 10 - 3;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("7", out.stdout);
    }

    // -------------------------------------------------------------------------
    // Nil and equality tests
    // -------------------------------------------------------------------------

    @Test
    void nilEqualityWithNil() {
        EvalOutcome out = interpret("var x; var y; print x == y;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("true", out.stdout, "nil == nil should be true");
    }

    @Test
    void nilNotEqualToNumber() {
        EvalOutcome out = interpret("var x; print x == 0;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("false", out.stdout, "nil should not equal 0");
    }

    @Test
    void uninitializedVariableIsNil() {
        EvalOutcome out = interpret("var x; print x;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("nil", out.stdout, "Uninitialized variable should print as nil");
    }

    // -------------------------------------------------------------------------
    // Chained assignment tests
    // -------------------------------------------------------------------------

    @Test
    void chainedAssignment() {
        EvalOutcome out = interpret("var a; var b; a = b = 5; print a; print b;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("5\n5", out.stdout, "Chained assignment should set both variables");
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
